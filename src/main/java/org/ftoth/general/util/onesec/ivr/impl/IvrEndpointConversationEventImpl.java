package org.ftoth.general.util.onesec.ivr.impl;

import org.ftoth.general.util.onesec.ivr.IvrEndpointConversation;
import org.ftoth.general.util.onesec.ivr.IvrEndpointConversationEvent;


public class IvrEndpointConversationEventImpl implements IvrEndpointConversationEvent
{
    private final IvrEndpointConversation conversation;

    public IvrEndpointConversationEventImpl(IvrEndpointConversation conversation) {
        this.conversation = conversation;
    }

    public IvrEndpointConversation getConversation() {
        return conversation;
    }
}
