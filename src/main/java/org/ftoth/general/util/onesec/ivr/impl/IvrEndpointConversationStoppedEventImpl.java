package org.ftoth.general.util.onesec.ivr.impl;

import org.ftoth.general.util.onesec.ivr.CompletionCode;
import org.ftoth.general.util.onesec.ivr.IvrEndpointConversation;
import org.ftoth.general.util.onesec.ivr.IvrEndpointConversationStoppedEvent;

public class IvrEndpointConversationStoppedEventImpl
        extends IvrEndpointConversationEventImpl implements IvrEndpointConversationStoppedEvent
{
    private final CompletionCode completionCode;

    public IvrEndpointConversationStoppedEventImpl(IvrEndpointConversation conversation, CompletionCode completionCode)
    {
        super(conversation);
        this.completionCode = completionCode;
    }

    public CompletionCode getCompletionCode() {
        return completionCode;
    }

//    public Call getCall() {
//        return call;
//    }
}
