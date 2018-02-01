package pipeline.outoforder;

import java.io.FileWriter;
import java.io.IOException;

import config.EnergyConfig;
import config.SimulationConfig;
import pipeline.outoforder.ReorderBufferEntry;
import generic.Core;
import generic.Event;
import generic.EventQueue;
import generic.GenericCircularQueue;
import generic.GlobalClock;
import generic.Instruction;
import generic.OperationType;
import generic.PortType;
import generic.SimulationElement;

public class DecodeLogic extends SimulationElement {
	
	Core core;
	OutOrderExecutionEngine containingExecutionEngine;
	GenericCircularQueue<Instruction> fetchBuffer;
	GenericCircularQueue<ReorderBufferEntry> decodeBuffer;
	int decodeWidth;
	long numAccesses;
	
	int invalidCount;
	
	public DecodeLogic(Core core, OutOrderExecutionEngine execEngine)
	{
		super(PortType.Unlimited, -1, -1 , -1, -1);
		this.core = core;
		this.containingExecutionEngine = execEngine;
		fetchBuffer = execEngine.getFetchBuffer();
		decodeBuffer = execEngine.getDecodeBuffer();
		decodeWidth = core.getDecodeWidth();
	}
	
	public void performDecode()
	{
		if(containingExecutionEngine.isToStall5() == true /*pipeline stalled due to branch mis-prediction*/
				|| containingExecutionEngine.isToStall1() == true /*IW full*/
				|| containingExecutionEngine.isToStall2() == true /*rename stall*/)
		{
			return;
		}
		
		ReorderBuffer ROB = containingExecutionEngine.getReorderBuffer();
		ReorderBufferEntry newROBEntry;
		
		for(int i = 0; i < decodeWidth; i++)
		{
			if(decodeBuffer.isFull() == true)
			{
				break;
			}
			
			Instruction headInstruction = fetchBuffer.peek(0);
			if(headInstruction != null)
			{
				if(ROB.isFull())
				{
					containingExecutionEngine.setToStall4(true);
					break;
				}
				
				if(headInstruction.getOperationType() == OperationType.load ||
						headInstruction.getOperationType() == OperationType.store)
				{
					if(containingExecutionEngine.getCoreMemorySystem().getLsqueue().isFull())
					{
						containingExecutionEngine.setToStall3(true);
						break;
					}
				}
				
				newROBEntry = makeROBEntries(headInstruction);
				
				decodeBuffer.enqueue(newROBEntry);
				fetchBuffer.dequeue();
				
				incrementNumAccesses(1);
				
				if(SimulationConfig.debugMode)
				{
					System.out.println("decoded : " + GlobalClock.getCurrentTime()/core.getStepSize() + " : "  + headInstruction);
				}
			}
			else
			{
				break;
			}
			
			containingExecutionEngine.setToStall3(false);
			containingExecutionEngine.setToStall4(false);
		}
	}
	
	ReorderBufferEntry makeROBEntries(Instruction newInstruction)
	{
		if(newInstruction != null)
		{
			ReorderBufferEntry newROBEntry = containingExecutionEngine.getReorderBuffer()
											.addInstructionToROB(
													newInstruction,
													newInstruction.getThreadID());
			
			//if load or store, make entry in LSQ
			if(newInstruction.getOperationType() == OperationType.load ||
					newInstruction.getOperationType() == OperationType.store)
			{
				boolean isLoad;
				if (newInstruction.getOperationType() == OperationType.load)
					isLoad = true;
				else
					isLoad = false;
					
				containingExecutionEngine.getCoreMemorySystem().allocateLSQEntry(isLoad, 
						newROBEntry.getInstruction().getSourceOperand1MemValue(),
						newROBEntry);
			}
			
			return newROBEntry;
		}
		
		return null;
	}

	@Override
	public void handleEvent(EventQueue eventQ, Event event) {
		
	}
	
	void incrementNumAccesses(int incrementBy)
	{
		numAccesses += incrementBy;
	}

	public EnergyConfig calculateAndPrintEnergy(FileWriter outputFileWriter, String componentName) throws IOException
	{
		EnergyConfig power = new EnergyConfig(containingExecutionEngine.getContainingCore().getDecodePower(), numAccesses);
		power.printEnergyStats(outputFileWriter, componentName);
		return power;
	}
	
	public EnergyConfig calculateEnergy()
	{
		EnergyConfig power = new EnergyConfig(containingExecutionEngine.getContainingCore().getDecodePower(), numAccesses);
		return power;
	}
}
