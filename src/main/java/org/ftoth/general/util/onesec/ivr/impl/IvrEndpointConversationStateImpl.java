/*
 *  Copyright 2009 Mikhail Titov.
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

package org.ftoth.general.util.onesec.ivr.impl;

import org.ftoth.general.util.onesec.core.impl.BaseState;
import org.ftoth.general.util.onesec.ivr.IvrEndpointConversation;
import org.ftoth.general.util.onesec.ivr.IvrEndpointConversationState;

public class IvrEndpointConversationStateImpl
        extends BaseState<IvrEndpointConversationState, IvrEndpointConversation>
        implements IvrEndpointConversationState
{
    public IvrEndpointConversationStateImpl(IvrEndpointConversation observableObject)
    {
        super(observableObject);

        addIdName(INVALID, "INVALID");
        addIdName(READY, "READY");
        addIdName(CONNECTING, "CONNECTING");
        addIdName(TALKING, "TALKING");
        addIdName(TRANSFERING, "TRANSFERING");
    }
}
