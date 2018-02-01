package pipeline.outoforder;

import java.io.FileWriter;
import java.io.IOException;
import config.EnergyConfig;
import memorysystem.CoreMemorySystem;
import pipeline.ExecutionCore;
import pipeline.ExecutionEngine;
import generic.Core;
import generic.GenericCircularQueue;
import generic.Instruction;

/**
 * execution engine comprises of : decode logic, ROB, instruction window, register files,
 * rename tables and functional units
 */
public class OutOrderExecutionEngine extends ExecutionEngine {

	//the containing core
	private Core core;

	//components of the execution engine
	private ICacheBuffer iCacheBuffer;
	private FetchLogic fetcher;
	private GenericCircularQueue<Instruction> fetchBuffer;
	private DecodeLogic decoder;
	private GenericCircularQueue<ReorderBufferEntry> decodeBuffer;
	private RenameLogic renamer;
	private GenericCircularQueue<ReorderBufferEntry> renameBuffer;
	private IWPushLogic IWPusher;
	private SelectLogic selector;
	private ExecutionLogic executer;
	private WriteBackLogic writeBackLogic;

	private ReorderBuffer reorderBuffer;
	private InstructionWindow instructionWindow;
	private RegisterFile integerRegisterFile;
	private RegisterFile floatingPointRegisterFile;
	private RenameTable integerRenameTable;
	private RenameTable floatingPointRenameTable;

	//Core-specific memory system (a set of LSQ, TLB and L1 cache)
	private OutOrderCoreMemorySystem outOrderCoreMemorySystem;
	private long dataHazardCount;
	//flags
	private boolean toStall1;					//if IW full
	//fetcher, decoder and renamer stall

	private boolean toStall2;					//if physical register cannot be
	//allocated to the dest of an instruction,
	//all subsequent processing must stall
	//fetcher and decoder stall

	private boolean toStall3;					//if LSQ full, and a load/store needs to be
	//allocated an entry
	//fetcher stall

	private boolean toStall4;					//if ROB full
	//fetcher stall

	private boolean toStall5;					//if branch mis-predicted
	//fetcher stall

	private long instructions;
	public long prevCycles;

	public OutOrderExecutionEngine(Core containingCore)
	{
		super(containingCore);
		dataHazardCount = 0;
		core = containingCore;
		instructions =0;
		reorderBuffer = new ReorderBuffer(core, this);
		instructionWindow = new InstructionWindow(core, this);
		integerRegisterFile = new RegisterFile(core, core.getIntegerRegisterFileSize());
		integerRenameTable = new RenameTable(this, core.getNIntegerArchitecturalRegisters(), core.getIntegerRegisterFileSize(), integerRegisterFile, core.getNo_of_input_pipes());
		floatingPointRegisterFile = new RegisterFile(core, core.getFloatingPointRegisterFileSize());
		floatingPointRenameTable = new RenameTable(this, core.getNFloatingPointArchitecturalRegisters(), core.getFloatingPointRegisterFileSize(), floatingPointRegisterFile, core.getNo_of_input_pipes());

		fetchBuffer = new GenericCircularQueue(Instruction.class, core.getDecodeWidth());
		fetcher = new FetchLogic(core, this);
		decodeBuffer = new GenericCircularQueue(ReorderBufferEntry.class, core.getDecodeWidth());
		decoder = new DecodeLogic(core, this);
		renameBuffer = new GenericCircularQueue(ReorderBufferEntry.class, core.getDecodeWidth());
		renamer = new RenameLogic(core, this);
		IWPusher = new IWPushLogic(core, this);
		selector = new SelectLogic(core, this);
		executer = new ExecutionLogic(core, this);
		writeBackLogic = new WriteBackLogic(core, this);


		toStall1 = false;
		toStall2 = false;
		toStall3 = false;
		toStall4 = false;
		toStall5 = false;
		prevCycles=0;
	}

	public ICacheBuffer getiCacheBuffer() {
		return iCacheBuffer;
	}

	public Core getCore() {
		return core;
	}

	public DecodeLogic getDecoder() {
		return decoder;
	}

	public RegisterFile getFloatingPointRegisterFile() {
		return floatingPointRegisterFile;
	}

	public RenameTable getFloatingPointRenameTable() {
		return floatingPointRenameTable;
	}

	public RegisterFile getIntegerRegisterFile() {
		return integerRegisterFile;
	}

	public RenameTable getIntegerRenameTable() {
		return integerRenameTable;
	}

	public ReorderBuffer getReorderBuffer() {
		return reorderBuffer;
	}

	public InstructionWindow getInstructionWindow() {
		return instructionWindow;
	}

	public void setInstructionWindow(InstructionWindow instructionWindow) {
		this.instructionWindow = instructionWindow;
	}

	public boolean isToStall1() {
		return toStall1;
	}

	public void setToStall1(boolean toStall1) {
		this.toStall1 = toStall1;
	}

	public boolean isToStall2() {
		return toStall2;
	}

	public void setToStall2(boolean toStall2) {
		this.toStall2 = toStall2;
	}

	public boolean isToStall3() {
		return toStall3;
	}

	public void setToStall3(boolean toStall3) {
		this.toStall3 = toStall3;
	}

	public boolean isToStall4() {
		return toStall4;
	}

	public void setToStall4(boolean toStall4) {
		this.toStall4 = toStall4;
	}

	public boolean isToStall5() {
		return toStall5;
	}

	public void setToStall5(boolean toStall5) {
		this.toStall5 = toStall5;
	}

	public GenericCircularQueue<Instruction> getFetchBuffer() {
		return fetchBuffer;
	}

	public GenericCircularQueue<ReorderBufferEntry> getDecodeBuffer() {
		return decodeBuffer;
	}

	public GenericCircularQueue<ReorderBufferEntry> getRenameBuffer() {
		return renameBuffer;
	}

	public FetchLogic getFetcher() {
		return fetcher;
	}

	public RenameLogic getRenamer() {
		return renamer;
	}
	
	public void increaseDataHazard()
	{
		dataHazardCount++;
	}
	
	public long getDataHazard()
	{
		return dataHazardCount;
	}
	
	public void setIns(long a)
	{
		this.instructions=a;
	}
	
	public long getIns()
	{
		return this.instructions;
	}

	public IWPushLogic getIWPusher() {
		return IWPusher;
	}

	public SelectLogic getSelector() {
		return selector;
	}

	public ExecutionLogic getExecuter() {
		return executer;
	}

	public WriteBackLogic getWriteBackLogic() {
		return writeBackLogic;
	}

	@Override
	public void setInputToPipeline(GenericCircularQueue<Instruction>[] inpList) {

		fetcher.setInputToPipeline(inpList);

	}

	public OutOrderCoreMemorySystem getCoreMemorySystem()
	{
		return outOrderCoreMemorySystem;
	}

	public void setCoreMemorySystem(CoreMemorySystem coreMemorySystem) {
		this.coreMemorySystem = coreMemorySystem;
		this.outOrderCoreMemorySystem = (OutOrderCoreMemorySystem)coreMemorySystem;
		this.iCacheBuffer = new ICacheBuffer((int)(core.getDecodeWidth() *
				coreMemorySystem.getiCache().getLatency()));
		this.fetcher.setICacheBuffer(iCacheBuffer);
	}

	public EnergyConfig calculateAndPrintEnergy(FileWriter outputFileWriter, String componentName) throws IOException
	{
		EnergyConfig totalPower = new EnergyConfig(0, 0);

		EnergyConfig bPredPower =  getBranchPredictor().calculateAndPrintEnergy(outputFileWriter, componentName + ".bPred");
		totalPower.add(totalPower, bPredPower);

		EnergyConfig decodePower =  getDecoder().calculateAndPrintEnergy(outputFileWriter, componentName + ".decode");
		totalPower.add(totalPower, decodePower);

		EnergyConfig renamePower =  getRenamer().calculateAndPrintEnergy(outputFileWriter, componentName + ".rename");
		totalPower.add(totalPower, renamePower);

		EnergyConfig lsqPower =  getCoreMemorySystem().getLsqueue().calculateAndPrintEnergy(outputFileWriter, componentName + ".LSQ");
		totalPower.add(totalPower, lsqPower);

		EnergyConfig intRegFilePower =  getIntegerRegisterFile().calculateAndPrintEnergy(outputFileWriter, componentName + ".intRegFile");
		totalPower.add(totalPower, intRegFilePower);

		EnergyConfig floatRegFilePower =  getFloatingPointRegisterFile().calculateAndPrintEnergy(outputFileWriter, componentName + ".floatRegFile");
		totalPower.add(totalPower, floatRegFilePower);

		EnergyConfig iwPower =  getInstructionWindow().calculateAndPrintEnergy(outputFileWriter, componentName + ".InstrWindow");
		totalPower.add(totalPower, iwPower);

		EnergyConfig robPower =  getReorderBuffer().calculateAndPrintEnergy(outputFileWriter, componentName + ".ROB");
		totalPower.add(totalPower, robPower);

		EnergyConfig fuPower =  getExecutionCore().calculateAndPrintEnergy(outputFileWriter, componentName + ".FuncUnit");
		totalPower.add(totalPower, fuPower);

		EnergyConfig resultsBroadcastBusPower =  getExecuter().calculateAndPrintEnergy(outputFileWriter, componentName + ".resultsBroadcastBus");
		totalPower.add(totalPower, resultsBroadcastBusPower);

		totalPower.printEnergyStats(outputFileWriter, componentName + ".total");

		return totalPower;
	}

	@Override
	public long getNumberOfBranches() {
		return reorderBuffer.branchCount;
	}

	@Override
	public long getNumberOfMispredictedBranches() {
		return reorderBuffer.mispredCount;
	}

	@Override
	public void setNumberOfBranches(long numBranches) {
		reorderBuffer.branchCount = numBranches;
	}

	@Override
	public void setNumberOfMispredictedBranches(long numMispredictedBranches) {
		reorderBuffer.mispredCount = numMispredictedBranches;
	}
	
	public EnergyConfig getbPredPower()
	{
		EnergyConfig bPredPower = getBranchPredictor().calculateEnergy();
		return bPredPower;
	}
		
	public EnergyConfig getDecodePower()
	{
		EnergyConfig decodePower = getDecoder().calculateEnergy();
		return decodePower;
	}
		
	public EnergyConfig getRenameLogicPower()
	{
		EnergyConfig renameLogicPower = getRenamer().calculateEnergy();
		return renameLogicPower;
	}
		
	public EnergyConfig getLSQPower()
	{
		EnergyConfig LSQPower = getCoreMemorySystem().getLsqueue().calculateEnergy();
		return LSQPower;
	}
		
	public EnergyConfig getIntRegPower()
	{
		EnergyConfig IntRegPower = getIntegerRegisterFile().calculateEnergy();
		return IntRegPower;
	}
		
	public EnergyConfig getFloatRegPower()
	{
		EnergyConfig FloatRegPower = getFloatingPointRegisterFile().calculateEnergy();
		return FloatRegPower;
	}
		
	public EnergyConfig getInsWinPower()
	{
		EnergyConfig InsWinPower = getInstructionWindow().calculateEnergy();
		return InsWinPower;
	}
		
	public EnergyConfig getROBPower()
	{
		EnergyConfig ROBPower = getReorderBuffer().calculateEnergy();
		return ROBPower;
	}
		
	public EnergyConfig getExecCorePower()
	{
		EnergyConfig execCorePower = getExecutionCore().calculateEnergy();
		return execCorePower;
	}
		
	public EnergyConfig getBCastBusPower()
	{
		EnergyConfig BCastBusPower = getExecuter().calculateEnergy();
		return BCastBusPower;
	}

	public EnergyConfig getWriteBackUnitInPower() 
	{
		return new EnergyConfig(0, 0);
	}
}