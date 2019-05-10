package org.ftoth.general.util.onesec.ivr;

public interface IvrEndpointConversationEvent
{
    /**
     * Returns the conversation that fires the event
     */
    public IvrEndpointConversation getConversation();
}
