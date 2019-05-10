package org.ftoth.general.util.onesec.ivr;

public interface IvrEndpointConversationTransferedEvent extends IvrEndpointConversationEvent
{
    /**
     * Return the address where call was transfered
     */
    public String getTransferAddress();
}
