package pipeline.outoforder;

import generic.Core;
import generic.Event;
import generic.EventQueue;
import generic.PortType;
import generic.SimulationElement;

public class SelectLogic extends SimulationElement {
	
	Core core;
	OutOrderExecutionEngine execEngine;
	InstructionWindow IW;	
	int issueWidth;
	public SelectLogic(Core core, OutOrderExecutionEngine execEngine)
	{
		super(PortType.Unlimited, -1, -1, -1, -1);
		this.core = core;
		this.execEngine = execEngine;
		IW = execEngine.getInstructionWindow();
		issueWidth = core.getIssueWidth();
	}
	
	/*
	 * ready instructions' issue are attempted (maximum of 'issueWidth' number of issues)
	 * important - all issues must be attempted first; only then must awakening be done
	 * 		this is because an awakened instruction is a
	 * 		candidate for issue ONLY in the next cycle 
	 */
	public void performSelect()
	{
		ReorderBuffer ROB = execEngine.getReorderBuffer();		
		if(execEngine.isToStall5() == true /*pipeline stalled due to branch mis-prediction*/
				|| ROB.head == -1 /*ROB empty*/)
		{
			return;
		}
		core.incCycles();	
		
		execEngine.getExecutionCore().clearPortUsage();
		
		int noIssued = 0;
		int checked = 0,nondata=0;
		int i;
		ReorderBufferEntry ROBEntry;
		i = ROB.head;
		String res;
		do
		{
			ROBEntry = ROB.ROB[i];
			
			if(ROBEntry.getIssued() == false &&
					ROBEntry.getAssociatedIWEntry() != null)
			{
				res = ROBEntry.getAssociatedIWEntry().issueInstruction();
				if(res=="OK")
				{
					//if issued
					noIssued++;						
				}
				if(res=="OK" || res=="MEMORY")
					nondata++;
			}
			
			if(noIssued >= issueWidth)
			{
				break;
			}
			
			i = (i+1)%ROB.MaxROBSize;
			checked++;
		}while(i != (ROB.tail+1)%ROB.MaxROBSize);
		core.addIssued(noIssued);
		if(nondata<issueWidth)
		{
			if(checked>=issueWidth)
			{
				for(int x=0;x<issueWidth-nondata;x++)
					execEngine.increaseDataHazard();
			}
			else
			{
				for(int x=0;x<checked-nondata;x++)
					execEngine.increaseDataHazard();	
			}
		}
	}

	@Override
	public void handleEvent(EventQueue eventQ, Event event) {
				
	}

}
