/*****************************************************************************
				BhartiSim Simulator
------------------------------------------------------------------------------------------------------------

   Copyright [2010] [Indian Institute of Technology, Delhi]
   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
------------------------------------------------------------------------------------------------------------

				Contributor: Mayur Harne
*****************************************************************************/

package memorysystem.nuca;
import java.util.Vector;

import memorysystem.AddressCarryingEvent;
import generic.EventQueue;
import generic.RequestType;
import generic.SimulationElement;

public class DestinationBankEvent extends AddressCarryingEvent
{
	private Vector<Integer> sourceBankId;
	private Vector<Integer> destinationBankId;
	public SimulationElement oldRequestingElement;
	public Vector<Integer> oldSourceBankId;
	public AddressCarryingEvent oldAddrEvent;
	public DestinationBankEvent(EventQueue eventQ, long eventTime,
			SimulationElement requestingElement,
			SimulationElement processingElement,
			RequestType requestType, long address,int coreId) {
		super(address, eventQ, eventTime, requestingElement, 
			  processingElement, requestType,address,coreId);
		sourceBankId = null;
		destinationBankId = null;
		oldSourceBankId = null;
		this.oldAddrEvent = null;
	}

	public DestinationBankEvent updateEvent(EventQueue eventQ, long eventTime, 
			SimulationElement requestingElement,
			SimulationElement processingElement,
			RequestType requestType, 
			Vector<Integer> sourceBankId,
			Vector<Integer> destinationBankId) {
		this.sourceBankId = (Vector<Integer>) sourceBankId.clone();
		this.destinationBankId = (Vector<Integer>) destinationBankId.clone();
		return (DestinationBankEvent) this.update(eventQ, eventTime, requestingElement, processingElement, requestType);
	}
	
	public void setSourceBankId(Vector<Integer> sourceBankId) {
		this.sourceBankId = (Vector<Integer>) sourceBankId.clone();
	}

	public Vector<Integer> getSourceBankId() {
		return sourceBankId;
	}

	public void setDestinationBankId(Vector<Integer> destinationBankId) {
		this.destinationBankId = (Vector<Integer>) destinationBankId.clone();
	}

	public Vector<Integer> getDestinationBankId() {
		return destinationBankId;
	}
}
