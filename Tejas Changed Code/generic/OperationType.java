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

	Contributors:  Prathmesh Kallurkar, Rajshekar Kalyappam
*****************************************************************************/

package generic;

//TODO needs to renamed
public enum OperationType 
{
	inValid (0),
	integerALU (1),
	integerMul (2),
	integerDiv (3),
	floatALU (4),
	floatMul (5),
	floatDiv (6),
	load (7),
	store (8),
	jump (9),
	branch (10),
	mov (11),
	xchg (12),
	acceleratedOp (13),
	nop (14),
	interrupt (15),
	no_of_types (16),
	sync (17);
	private final int opCode;
    OperationType(int opCode) 
    {
        this.opCode = opCode;
    }    
    public int getOpCode() 
    {
        return this.opCode;
    }
}