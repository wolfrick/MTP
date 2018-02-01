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

	Contributors:  Moksh Upadhyay
*****************************************************************************/
package misc;

public class Util{
	public Util()
	{
		
	}
	public static int logbase2(int val)
	{
		if(val <= 0)
			return -1;

		int cnt = 0;
		while (true) 
		{
			if(val == 1)
				return cnt;
			val = val >> 1;
			cnt++;
		}
	}
	public static long parseLong(String s) {
		long ret = 0;
		for(int i=0; i<s.length(); i++) {
			ret*=10; ret+=s.charAt(i)-'0';
		}
		return ret;
	}
}
