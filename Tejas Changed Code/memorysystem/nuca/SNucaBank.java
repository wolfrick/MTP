/*****************************************************************************
				Tejas Simulator
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

				Contributor: Eldhose Peter
*****************************************************************************/
package memorysystem.nuca;
import generic.Event;
import generic.EventQueue;
import memorysystem.CoreMemorySystem;
import config.CacheConfig;

public class SNucaBank extends NucaCache  implements NucaInterface
{
	NucaCache parent;
	public SNucaBank(String cacheName, int id, CacheConfig cacheParameters,
			CoreMemorySystem containingMemSys, NucaCache p)
	{
		super(cacheName, id, cacheParameters, containingMemSys);
		parent = p;
		this.mshr = parent.getMshr();
		this.eventsWaitingOnMSHR = parent.eventsWaitingOnMSHR;        
	}
	
	@Override
	public void handleEvent(EventQueue q, Event e)
	{
		callCacheHandleEvent(q, e);
	}
}