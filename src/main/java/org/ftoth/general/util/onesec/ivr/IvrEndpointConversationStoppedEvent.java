package org.ftoth.general.util.onesec.ivr;


public interface IvrEndpointConversationStoppedEvent extends IvrEndpointConversationEvent
{
    /**
     * Returns the completion code of stopped conversation
     */
    public CompletionCode getCompletionCode();
//    /**
//     * Returns the call associated with conversation
//     */
//    public Call getCall();
}
