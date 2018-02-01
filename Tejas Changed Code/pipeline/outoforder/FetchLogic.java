package pipeline.outoforder;

import config.SimulationConfig;
import main.ArchitecturalComponent;
import main.CustomObjectPool;
import memorysystem.AddressCarryingEvent;
import generic.Barrier;
import generic.BarrierTable;
import generic.Core;
import generic.Event;
import generic.EventQueue;
import generic.GenericCircularQueue;
import generic.Instruction;
import generic.OperationType;
import generic.PortType;
import generic.RequestType;
import generic.SimulationElement;

public class FetchLogic extends SimulationElement {
	
	Core core;
	OutOrderExecutionEngine execEngine;

	GenericCircularQueue<Instruction>[] inputToPipeline;
	int inputPipeToReadNext;
	ICacheBuffer iCacheBuffer;
	GenericCircularQueue<Instruction> fetchBuffer;	
	int fetchWidth;
	OperationType[] instructionsToBeDropped;
	boolean sleep;

	public FetchLogic(Core core, OutOrderExecutionEngine execEngine)
	{
		super(PortType.Unlimited, -1, -1, -1, -1);
		this.core = core;
		this.execEngine = execEngine;
		fetchBuffer = execEngine.getFetchBuffer();
		fetchWidth = core.getDecodeWidth();
		inputPipeToReadNext = 0;
		sleep = false;

		instructionsToBeDropped = new OperationType[] {
															OperationType.interrupt,
															OperationType.sync
													};
	}
	
	public void performFetch()
	{
		//to detach pipeline
		boolean checkTranslatorSpeed = false;
		
		if(checkTranslatorSpeed)
		{
			Instruction inst;
			while((inst = inputToPipeline[0].dequeue()) != null)
			{
				if(inst.getOperationType() == OperationType.inValid)
				{
					execEngine.setExecutionComplete(true);
				}
				CustomObjectPool.getInstructionPool().returnObject(inst);
			}
			
			return;
		}
		
		if(sleep == true)
		{
			return;
		}
		
		Instruction newInstruction;
		
		if(!execEngine.isToStall1() &&
				!execEngine.isToStall2() &&
				!execEngine.isToStall3() &&
				!execEngine.isToStall4() &&
				!execEngine.isToStall5())
		{
			//add instructions, for whom "fetch" from iCache has completed, to fetch buffer
			//decode stage reads from this buffer
			for(int i = 0; i < fetchWidth; i++)
			{
				if(fetchBuffer.isFull() == true)
				{
					break;
				}
				
				newInstruction = iCacheBuffer.getNextInstruction();
				if(newInstruction != null)
				{
					fetchBuffer.enqueue(newInstruction);
				}
				else
				{
					this.core.getExecEngine().incrementInstructionMemStall(1); 
					break;
				}
			}
		}
		
		//this loop reads from inputToPipeline and places the instruction in iCacheBuffer
		//fetch of the instruction is also issued to the iCache
		for(int i = 0; i < iCacheBuffer.size; i++)
		{
			if(inputToPipeline[inputPipeToReadNext].size() <= 0)
			{
				break;
			}
			
			newInstruction = inputToPipeline[inputPipeToReadNext].peek(0);
			
			//process sync operation(Barrier)
			if(newInstruction.getOperationType() == OperationType.sync){
				long barrierAddress = newInstruction.getCISCProgramCounter();
				Barrier bar = BarrierTable.barrierList.get(barrierAddress);
				bar.incrementThreads();
				if(this.core.TreeBarrier == true){
					setSleep(true);
					int coreId = this.core.getCore_number();
					ArchitecturalComponent.coreBroadcastBus.getPort().put(new AddressCarryingEvent(
							0,
							this.core.eventQueue,
							 1,
							 ArchitecturalComponent.coreBroadcastBus, 
							 ArchitecturalComponent.coreBroadcastBus, 
							 RequestType.TREE_BARRIER, 
							 barrierAddress,
							 coreId));
				}
				else{
					if(bar.timeToCross())
					{
						System.out.println("    Time to cross " + bar.getBarrierAddress());
						setSleep(true);
						for(int j=0; j<bar.getNumThreads(); j++ ){
							ArchitecturalComponent.coreBroadcastBus.addToResumeCore(bar.getBlockedThreads().elementAt(j));
						}
						ArchitecturalComponent.coreBroadcastBus.getPort().put(new AddressCarryingEvent(
								this.core.eventQueue,
								 1,
								 ArchitecturalComponent.coreBroadcastBus, 
								 ArchitecturalComponent.coreBroadcastBus, 
								 RequestType.PIPELINE_RESUME, 
								 0));
	
					}
					else
					{
						System.out.println("Total on bar " + bar.getBarrierAddress() + " is " + bar.getNumThreadsArrived());
						setSleep(true);
					}
				}
			}
			
			//drop instructions on the drop list
			if(shouldInstructionBeDropped(newInstruction) == true)
			{
				inputToPipeline[inputPipeToReadNext].pollFirst();
				CustomObjectPool.getInstructionPool().returnObject(newInstruction);
				i--;
				continue;
			}
			
			//drop memory operations if specified in configuration file
			if(newInstruction.getOperationType() == OperationType.load ||
					newInstruction.getOperationType() == OperationType.store)
			{
				if(SimulationConfig.detachMemSysData == true)
				{
					inputToPipeline[inputPipeToReadNext].pollFirst();
					CustomObjectPool.getInstructionPool().returnObject(newInstruction);
					i--;
					continue;
				}
			}
			
			//add to iCache buffer, and issue request to iCache
			if(!iCacheBuffer.isFull() && execEngine.getCoreMemorySystem().getiCache().isBusy()==false)
			{
				iCacheBuffer.addToBuffer(inputToPipeline[inputPipeToReadNext].pollFirst());
				if(SimulationConfig.detachMemSysInsn == false && newInstruction.getOperationType() != OperationType.inValid)
				{
						// The first micro-operation of an instruction has a valid CISC IP. All the subsequent 
					  	// micro-ops will have IP = -1(meaning invalid). We must not forward this requests to iCache.
						if(newInstruction.getCISCProgramCounter()!=-1)
						{
							execEngine.getCoreMemorySystem().issueRequestToInstrCache(newInstruction.getCISCProgramCounter());
						}
				}
			}
			else
			{
				break;
			}
		}
		
		//SMT support
		//round-robin among the various input-to-pipelines, fetching from a different
		//non-empty input every cycle
		/*
		int noOfIterations = 0;
		do
		{
			inputPipeToReadNext = (inputPipeToReadNext + 1)%core.getNo_of_input_pipes();
			noOfIterations++;
		}while(inputToPipeline[inputPipeToReadNext].isEmpty() == true
				&& noOfIterations < fetchWidth);*/
	}

	@Override
	public void handleEvent(EventQueue eventQ, Event event) {
			
	}
	
	boolean shouldInstructionBeDropped(Instruction instruction)
	{
		for(int i = 0; i < instructionsToBeDropped.length; i++)
		{
			if(instructionsToBeDropped[i] == instruction.getOperationType())
			{
				return true;
			}
		}
		return false;
	}
	
	public void processCompletionOfMemRequest(long address)
	{
		iCacheBuffer.updateFetchComplete(address);
	}
	
	public GenericCircularQueue<Instruction>[] getInputToPipeline() {
		return inputToPipeline;
	}

	public void setInputToPipeline(GenericCircularQueue<Instruction>[] inputToPipeline) {
		this.inputToPipeline = inputToPipeline;
	}

	public void setICacheBuffer(ICacheBuffer iCacheBuffer)
	{
		this.iCacheBuffer = iCacheBuffer;
	}

	public boolean isSleep() {
		return sleep;
	}

	public void setSleep(boolean sleep) {
		if(sleep == true)
			System.out.println("sleeping pipeline " + this.core.getCore_number());
		else
			System.out.println("resuming pipeline " + this.core.getCore_number());
		this.sleep = sleep;
	}
}
