/*
 *  Copyright 2007 Mikhail Titov.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */

package org.ftoth.general.util.onesec.core;

public interface State<T extends State, O extends ObjectDescription>
{
	public O getObservableObject();

	public int getId();

	public String getIdName();

	public long getCounter();

	public boolean hasError();

	public Throwable getErrorException();

	public String getErrorMessage();

	public StateWaitResult<T> waitForState(int[] states, long timeout);

	public StateWaitResult<T> waitForNewState(State state, long timeout);

	public void addStateListener(StateListener listener);

	public void removeStateListener(StateListener listener);
}
