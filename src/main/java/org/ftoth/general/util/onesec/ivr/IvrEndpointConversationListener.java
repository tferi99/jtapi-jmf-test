package org.ftoth.general.util.onesec.ivr;

public interface IvrEndpointConversationListener
{
    /**
     * Fires when listener added to the conversation. The advantage to execute code inside 
     * this method is that the conversation state can not change at the time of execution of this
     * method
     */
    public void listenerAdded(IvrEndpointConversationEvent event);
    /**
     * Fires when conversation was started.
     */
    public void conversationStarted(IvrEndpointConversationEvent event);
    /**
     * Fires when conversation was stopped.
     */
    public void conversationStopped(IvrEndpointConversationStoppedEvent event);
    /**
     * Fires when conversation was transfered to the number passed in the parameter
     * @address number to which conversation was transfered
     */
    public void conversationTransfered(IvrEndpointConversationTransferedEvent event);
    /**
     * Fires when incoming RTP of the conversation were started
     */
    public void incomingRtpStarted(IvrIncomingRtpStartedEvent event);
    /**
     * Fires when outgoing RTP of the conversation were started
     */
    public void outgoingRtpStarted(IvrOutgoingRtpStartedEvent event);
    
    public void dtmfReceived(IvrDtmfReceivedConversationEvent event);
}
