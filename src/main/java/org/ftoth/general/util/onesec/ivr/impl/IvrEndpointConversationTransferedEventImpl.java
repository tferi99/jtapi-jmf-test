package org.ftoth.general.util.onesec.ivr.impl;

import org.ftoth.general.util.onesec.ivr.IvrEndpointConversation;
import org.ftoth.general.util.onesec.ivr.IvrEndpointConversationTransferedEvent;

/**
 *
 * @author Mikhail Titov
 */
public class IvrEndpointConversationTransferedEventImpl
        extends IvrEndpointConversationEventImpl implements IvrEndpointConversationTransferedEvent
{
    private final String transferAddress;

    public IvrEndpointConversationTransferedEventImpl(
            IvrEndpointConversation conversation, String tranferAddress)
    {
        super(conversation);
        this.transferAddress = tranferAddress;
    }

    public String getTransferAddress() {
        return transferAddress;
    }
}
