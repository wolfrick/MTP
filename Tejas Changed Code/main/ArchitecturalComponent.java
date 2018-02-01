package main;

import dram.MainMemoryDRAMController;
import emulatorinterface.communication.IpcBase;
import generic.CommunicationInterface;
import generic.Core;
import generic.CoreBcastBus;
import generic.EventQueue;
import generic.GlobalClock;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Vector;

import config.CacheConfig;
import config.EnergyConfig;
import config.MainMemoryConfig;
import config.SystemConfig;

import memorysystem.Cache;
import memorysystem.CoreMemorySystem;
import dram.MainMemoryDRAMController;
import memorysystem.MemorySystem;
import memorysystem.coherence.Coherence;
import memorysystem.nuca.NucaCache;
import net.Bus;
import net.BusInterface;
import net.InterConnect;
import net.NOC;
import net.Router;
import pipeline.multi_issue_inorder.MultiIssueInorderExecutionEngine;
import pipeline.outoforder.ICacheBuffer;
import pipeline.outoforder.OutOrderExecutionEngine;

public class ArchitecturalComponent {
	
	public static Vector<Core> cores = new Vector<Core>();
	public static Vector<CoreMemorySystem> coreMemSysArray = new Vector<CoreMemorySystem>();
	public static Vector<Coherence> coherences = new Vector<Coherence>();	
	public static Vector<MainMemoryDRAMController> memoryControllers = new Vector<MainMemoryDRAMController>();
	public static Vector<Cache> sharedCaches = new Vector<Cache>();
	public static Vector<Cache> caches = new Vector<Cache>();
	public static HashMap<String, NucaCache> nucaList=  new HashMap<String, NucaCache>();
	private static InterConnect interconnect;
	public static CoreBcastBus coreBroadcastBus;
		
	public static void createChip() {
		// Interconnect
			// Core
			// Coherence
			// Shared Cache
			// Main Memory Controller

		if(SystemConfig.interconnect ==  SystemConfig.Interconnect.Bus) {
			ArchitecturalComponent.setInterConnect(new Bus());
			createElementsOfBus();
		} else if(SystemConfig.interconnect == SystemConfig.Interconnect.Noc) {
			ArchitecturalComponent.setInterConnect(new NOC(SystemConfig.nocConfig));
			createElementsOfNOC();			
			((NOC)interconnect).ConnectNOCElements();
		}
		
		MemorySystem.createLinkBetweenCaches();
		MemorySystem.setCoherenceOfCaches();
		initCoreBroadcastBus();
		GlobalClock.systemTimingSetUp(getCores());
	}
	
	private static void createElementsOfBus() {
		
		Bus bus = new Bus();
		BusInterface busInterface;
		
		// Create Cores
		for(int i=0; i<SystemConfig.NoOfCores; i++) {
			Core core = createCore(i);
			busInterface = new BusInterface(bus);
			core.setComInterface(busInterface);
			cores.add(core);
		}
		
		// Create Shared Cache
		// PS : Directory will be created as a special shared cache
		for(CacheConfig cacheConfig : SystemConfig.sharedCacheConfigs) {
			busInterface = new BusInterface(bus);
			Cache c = MemorySystem.createSharedCache(cacheConfig.cacheName, busInterface);
//			c.setComInterface(busInterface);
		}
		
		// Create Main Memory Controller
		//Note: number of physical channels = number of Memory Controllers
		
		//added later by kush
		//in case we use simple (fixed latency) memory controllers, num channels not taken into consideration currently (default to 1)
		if(SystemConfig.memControllerToUse==true){
			for(int i=0;i<SystemConfig.mainMemoryConfig.numChans;i++){
				MainMemoryDRAMController mainMemController = new MainMemoryDRAMController(SystemConfig.mainMemoryConfig);
				mainMemController.setChannelNumber(i);
				busInterface = new BusInterface(bus);
				mainMemController.setComInterface(busInterface);
				memoryControllers.add(mainMemController);
			}
		}
		else{
			MainMemoryDRAMController mainMemController = new MainMemoryDRAMController(SystemConfig.mainMemoryConfig);
				mainMemController.setChannelNumber(0);
				busInterface = new BusInterface(bus);
				mainMemController.setComInterface(busInterface);
				memoryControllers.add(mainMemController);
		}
		
		}
	
	private static void createElementsOfNOC() {
		//create elements mentioned as topology file
		BufferedReader readNocConfig = NOC.openTopologyFile(SystemConfig.nocConfig.NocTopologyFile);
		
		// Skip the first line. It contains numrows/cols information
		try {
			readNocConfig.readLine();
		} catch (IOException e1) {
			misc.Error.showErrorAndExit("Error in reading noc topology file !!");
		}
		
		int numRows = ((NOC)interconnect).getNumRows();
		int numColumns = ((NOC)interconnect).getNumColumns();
		for(int i=0;i<numRows;i++)
		{
			String str = null;
			try {
				str = readNocConfig.readLine();
			} catch (IOException e) {
				misc.Error.showErrorAndExit("Error in reading noc topology file !!");
			}
			
			//StringTokenizer st = new StringTokenizer(str," ");
			StringTokenizer st = new StringTokenizer(str);
			
			for(int j=0;j<numColumns;j++)
			{
				String nextElementToken = (String)st.nextElement();
				
				//System.out.println("NOC [" + i + "][" + j + "] = " + nextElementToken);
				
				CommunicationInterface comInterface = ((NOC)interconnect).getNetworkElements()[i][j];
				
				if(nextElementToken.equals("C")){
					Core core = createCore(cores.size());
					cores.add(core);
					core.setComInterface(comInterface);
				} else if(nextElementToken.equals("M")) {
					MainMemoryDRAMController mainMemController = new MainMemoryDRAMController(SystemConfig.mainMemoryConfig);
					memoryControllers.add(mainMemController);
					mainMemController.setComInterface(comInterface);
				} else if(nextElementToken.equals("-")) {
					//do nothing
				} else {
					Cache c = MemorySystem.createSharedCache(nextElementToken, comInterface);
					//TODO split and multiple shared caches
				} 
			}
		}
	}	
	
	static Core createCore(int coreNumber) {
		return new Core(coreNumber, 1, 1, null, new int[]{0});
	}
		
	public static InterConnect getInterConnect() {
		return interconnect;
	}
	
	public static void setInterConnect(InterConnect i) {
		interconnect = i;
	}
		
	//TODO read a config file
	//create specified number of cores
	//map threads to cores
	public static Core[] initCores()
	{
		System.out.println("initializing cores...");
		
		Core[] cores = new Core[IpcBase.getEmuThreadsPerJavaThread()];
		for (int i=0; i<IpcBase.getEmuThreadsPerJavaThread(); i++) {
			cores[i] = new Core(i,
							1,
							1,
							null,
							new int[]{0});
		}
		
		GlobalClock.systemTimingSetUp(cores);
		
		//TODO wont work in case of multiple runnable threads
//			for(int i = 0; i<IpcBase.getEmuThreadsPerJavaThread(); i++)
//			{
//				if (SimulationConfig.isPipelineInorder)
//				{
//					((InorderExecutionEngine)cores[i].getExecEngine()).setAvailable(true);
//				}
//				else if (SimulationConfig.isPipelineMultiIssueInorder)
//				{
//					//TODO
//					((InorderExecutionEngine)cores[i].getExecEngine()).setAvailable(true);
//				}
//				else if(SimulationConfig.isPipelineOutOfOrder)
//				{	
//					((OutOrderExecutionEngine)cores[i].getExecEngine()).setAvailable(true);
//				}
//			}
		return cores;
	}

	public static Core getCore(int i) {
		return cores.get(i);
	}
	
	public static Vector<Core> getCoresVector() {
		return cores;
	}
	
	public static Core[] getCores() {
		Core[] coreArray = new Core[cores.size()];
		int i=0;
		for(Core core : cores) {
			coreArray[i] = core;
			i++;
		}
		
		return coreArray;
	}

	public static long getNoOfInstsExecuted()
	{
		long noOfInstsExecuted = 0;
		for(int i = 0; i < ArchitecturalComponent.getCores().length; i++)
		{
			noOfInstsExecuted += ArchitecturalComponent.getCores()[i].getNoOfInstructionsExecuted();
		}
		return noOfInstsExecuted;
	}
	
	public static long getNoOfDataStall()
	{
		long dataStalls = 0;
		if(ArchitecturalComponent.getCore(0).isPipelineInOrder())
			for(int i = 0; i < ArchitecturalComponent.getCores().length; i++)
				dataStalls += ((MultiIssueInorderExecutionEngine)(ArchitecturalComponent.getCores()[i].getExecEngine())).getDataHazardStall();
		else
			for(int i = 0; i < ArchitecturalComponent.getCores().length; i++)
				dataStalls += ((OutOrderExecutionEngine)(ArchitecturalComponent.getCores()[i].getExecEngine())).getDataHazard();
			
		return dataStalls;
	}

	public static void debugNoOfDataStall()
	{
		System.out.println(((OutOrderExecutionEngine)(ArchitecturalComponent.getCores()[0].getExecEngine())).getDataHazard()+" Stall for core");
		long[] res = ArchitecturalComponent.getCore(0).getIssued();
		for(int i=0;i<=ArchitecturalComponent.getCore(0).getIssueWidth();i++)
			System.out.println(res[i]+" times "+i+" instruction issued" );
	}

	
	public static long getNoIns()
	{
		long memStalls = 0;
		if(!ArchitecturalComponent.getCore(0).isPipelineInOrder())
			for(int i = 0; i < ArchitecturalComponent.getCores().length; i++)
			memStalls += ((OutOrderExecutionEngine)(ArchitecturalComponent.getCores()[i].getExecEngine())).getIns();
		
		return memStalls;
	
	}
	
	public static long getNoOfMemStall()
	{
		long memStalls = 0;
		if(ArchitecturalComponent.getCore(0).isPipelineInOrder())
		for(int i = 0; i < ArchitecturalComponent.getCores().length; i++)
			memStalls += ((MultiIssueInorderExecutionEngine)(ArchitecturalComponent.getCores()[i].getExecEngine())).getMemStall();
		
		return memStalls;
	}

	public static long getNoOfControlStall()
	{
		long controlStalls = 0;
		if(ArchitecturalComponent.getCore(0).isPipelineInOrder())
		for(int i = 0; i < ArchitecturalComponent.getCores().length; i++)
			controlStalls += ((MultiIssueInorderExecutionEngine)(ArchitecturalComponent.getCores()[i].getExecEngine())).getControlHazardStall();
		
		return controlStalls;
	}
	
	public static long getTotalHazardCount()
	{
		long hazard = 0;
		hazard += getNoOfDataStall()+getNoOfMemStall()+getNoOfControlStall();
		return hazard;
	}
	
	public static void dumpAllICacheBuffers()
	{
		System.out.println("\n\nICache Buffer DUMP\n\n");
		ICacheBuffer buffer = null;
		for(int i = 0; i < ArchitecturalComponent.getCores().length; i++)
		{
			buffer = ((OutOrderExecutionEngine)ArchitecturalComponent.getCores()[i].getExecEngine()).getiCacheBuffer();
			System.out.println("---------------------------------------------------------------------------");
			System.out.println("CORE " + i);
			buffer.dump();
		}
	}

	public static void dumpAllEventQueues()
	{
		System.out.println("\n\nEvent Queue DUMP\n\n");
		EventQueue eventQueue = null;
		for(int i = 0; i < ArchitecturalComponent.getCores().length; i++)
		{
			eventQueue = ArchitecturalComponent.getCores()[i].getEventQueue();
			System.out.println("---------------------------------------------------------------------------");
			System.out.println("CORE " + i);
		}
	}

	public static void dumpAllMSHRs()
	{
		CoreMemorySystem coreMemSys = null;
		System.out.println("\n\nMSHR DUMP\n\n");
		for(Cache c : getCacheList()) {
			c.printMSHR();
		}
		
	}

	public static void dumpOutStandingLoads()
	{
		/*System.out.println("Outstanding loads on core ");
		for(int i = 0; i < ArchitecturalComponent.getCores().length; i++)
		{
			System.out.println( "outstanding loads on core "+i +"  = "+((InorderExecutionEngine)ArchitecturalComponent.getCores()[i].getExecEngine()).noOfOutstandingLoads);
		}*/
	}
	
	
	public static CoreMemorySystem[] getCoreMemSysArray()
	{
		CoreMemorySystem[] toBeReturned = new CoreMemorySystem[coreMemSysArray.size()];
		int i = 0;
		for(CoreMemorySystem c : coreMemSysArray) {
			toBeReturned[i] = c;
			i++;
		}
		return toBeReturned;
	}

	private static ArrayList<Router> nocRouterList = new ArrayList<Router>();
	
	public static void addNOCRouter(Router router) {
		nocRouterList.add(router);		
	}
	
	public static ArrayList<Router> getNOCRouterList() {
		return nocRouterList;
	}

	public static void initCoreBroadcastBus() {
		coreBroadcastBus = new CoreBcastBus();		
	}

	public static MainMemoryDRAMController getMainMemoryDRAMController(CommunicationInterface comInterface,int chanNum) {
		//TODO : return the nearest memory controller based on the location of the communication interface
		return memoryControllers.get(chanNum);
	}
	
	public static Vector<Cache> getCacheList() {
		return caches;
	}
	
	public static Vector<Cache> getSharedCacheList() {
		return sharedCaches;
	}
	
	public static long[] getNoOfInsTypesExecuted()
	{
		long[] noOfInsTypesExecuted = new long[18];
		for(int i=0;i<18;i++)
		{
			noOfInsTypesExecuted[i] = 0;
			for(int j = 0; j < ArchitecturalComponent.getCores().length; j++)
			{
				noOfInsTypesExecuted[i] += ArchitecturalComponent.getCores()[j].getNoOfTypes()[i];
			}	
		}
		return noOfInsTypesExecuted;
	}
		
	public static long getComputeInstructions()
	{
		long[] temp = getNoOfInsTypesExecuted();
		long val=0;
		for(int i=1;i<7;i++)
			val += temp[i];
		return val;
	}
	
	public static long getMemoryInstructions()
	{
		long[] temp = getNoOfInsTypesExecuted();
		long val=0;
		for(int i=7;i<9;i++)
			val += temp[i];
		return val;
	}
	
	public static long getBranchInstructions()
	{
		long[] temp = getNoOfInsTypesExecuted();
		long val=0;
		for(int i=9;i<17;i++)
			val += temp[i];
		return val;
	}
	
	public static long getSyncInstructions()
	{
		long[] temp = getNoOfInsTypesExecuted();
		return temp[17];
	}
	
	public static EnergyConfig getiCacheEnergy()
	{
		EnergyConfig total = new EnergyConfig(0, 0);
		for(int i = 0; i < ArchitecturalComponent.getCores().length; i++)
		{
			EnergyConfig iCache = ArchitecturalComponent.getCores()[i].getiCacheEnergy();
			total.add(iCache);
		}
		return total;
	}
		
	public static EnergyConfig getdCacheEnergy()
	{
		EnergyConfig total = new EnergyConfig(0, 0);
		for(int i = 0; i < ArchitecturalComponent.getCores().length; i++)
		{
			EnergyConfig dCache = ArchitecturalComponent.getCores()[i].getL1CacheEnergy();
			total.add(dCache);
		}
		return total;
	}
		
	public static EnergyConfig getiTLBEnergy()
	{
		EnergyConfig total = new EnergyConfig(0, 0);
		for(int i = 0; i < ArchitecturalComponent.getCores().length; i++)
		{
			EnergyConfig iCache = ArchitecturalComponent.getCores()[i].getiTLBEnergy();
			total.add(iCache);
		}
		return total;
	}
		
	public static EnergyConfig getdTLBEnergy()
	{
		EnergyConfig total = new EnergyConfig(0, 0);
		for(int i = 0; i < ArchitecturalComponent.getCores().length; i++)
		{
			EnergyConfig iCache = ArchitecturalComponent.getCores()[i].getdTLBEnergy();
			total.add(iCache);
		}
		return total;
	}
		
	public static EnergyConfig getbPredPower()
	{
		EnergyConfig total = new EnergyConfig(0, 0);
		for(int i = 0; i < ArchitecturalComponent.getCores().length; i++)
		{
			EnergyConfig bPred = ArchitecturalComponent.getCores()[i].getbpredPower();
			total.add(bPred);
		}
		return total;
	}
		
	public static EnergyConfig getDecodePower()
	{
		EnergyConfig total = new EnergyConfig(0, 0);
		for(int i = 0; i < ArchitecturalComponent.getCores().length; i++)
		{
			EnergyConfig decode = ArchitecturalComponent.getCores()[i].getDecodPower();
			total.add(decode);
		}
		return total;
	}
		
	public static EnergyConfig getExecCorePower()
	{
		EnergyConfig total = new EnergyConfig(0, 0);
		for(int i = 0; i < ArchitecturalComponent.getCores().length; i++)
		{
			EnergyConfig bPred = ArchitecturalComponent.getCores()[i].getExecCorePower();
			total.add(bPred);
		}
		return total;
	}
		
	public static EnergyConfig getWriteBackPower()
	{
		EnergyConfig total = new EnergyConfig(0, 0);
		for(int i = 0; i < ArchitecturalComponent.getCores().length; i++)
		{
			EnergyConfig write = ArchitecturalComponent.getCores()[i].getWriteBackUnitInPower();
			total.add(write);
		}
		return total;
	}
		
	public static EnergyConfig getRenameLogicPower()
	{
		EnergyConfig total = new EnergyConfig(0, 0);
		for(int i = 0; i < ArchitecturalComponent.getCores().length; i++)
		{
			EnergyConfig renl = ArchitecturalComponent.getCores()[i].getRenameLogicPower();
			total.add(renl);
		}
		return total;
	}
		
	public static EnergyConfig getLSQPower()
	{
		EnergyConfig total = new EnergyConfig(0, 0);
		for(int i = 0; i < ArchitecturalComponent.getCores().length; i++)
		{
			EnergyConfig lsq = ArchitecturalComponent.getCores()[i].getLSQPower();
			total.add(lsq);
		}
		return total;
	}
		
	public static EnergyConfig getIntRegPower()
	{
		EnergyConfig total = new EnergyConfig(0, 0);
		for(int i = 0; i < ArchitecturalComponent.getCores().length; i++)
		{
			EnergyConfig intr = ArchitecturalComponent.getCores()[i].getIntRegPower();
			total.add(intr);
		}
		return total;
	}

	public static EnergyConfig getFloatRegPower()
	{
		EnergyConfig total = new EnergyConfig(0, 0);
		for(int i = 0; i < ArchitecturalComponent.getCores().length; i++)
		{
			EnergyConfig freg = ArchitecturalComponent.getCores()[i].getFloatRegPower();
			total.add(freg);
		}
		return total;
	}
		
	public static EnergyConfig getInsWinPower()
	{
		EnergyConfig total = new EnergyConfig(0, 0);
		for(int i = 0; i < ArchitecturalComponent.getCores().length; i++)
		{
			EnergyConfig insw = ArchitecturalComponent.getCores()[i].getInsWinPower();
			total.add(insw);
		}
		return total;
	}
		
	public static EnergyConfig getROBPower()
	{
		EnergyConfig total = new EnergyConfig(0, 0);
		for(int i = 0; i < ArchitecturalComponent.getCores().length; i++)
		{
			EnergyConfig rob = ArchitecturalComponent.getCores()[i].getROBPower();
			total.add(rob);
		}
		return total;
	}
		
	public static EnergyConfig getBCastBusPower()
	{
		EnergyConfig total = new EnergyConfig(0, 0);
		for(int i = 0; i < ArchitecturalComponent.getCores().length; i++)
		{
			EnergyConfig bcast = ArchitecturalComponent.getCores()[i].getBCastBusPower();
			total.add(bcast);
		}
		return total;
	}
	
	public static EnergyConfig getPipelinePower()
	{
		EnergyConfig pipeline = new EnergyConfig(0,0);
		pipeline.add(getWriteBackPower());
		pipeline.add(getbPredPower());
		pipeline.add(getDecodePower());
		pipeline.add(getExecCorePower());
		pipeline.add(getBCastBusPower());
		pipeline.add(getRenameLogicPower());
		pipeline.add(getLSQPower());
		pipeline.add(getIntRegPower());
		pipeline.add(getFloatRegPower());
		pipeline.add(getInsWinPower());
		pipeline.add(getROBPower());
		return pipeline;
	}

	public static EnergyConfig getTotalPower()
	{
		EnergyConfig total = new EnergyConfig(0,0);
		total.add(getiCacheEnergy());
		total.add(getiTLBEnergy());
		total.add(getdCacheEnergy());
		total.add(getdTLBEnergy());
		total.add(getPipelinePower());
		return total;
	}
}
