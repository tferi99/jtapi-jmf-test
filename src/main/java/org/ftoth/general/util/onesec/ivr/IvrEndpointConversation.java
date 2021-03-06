package org.ftoth.general.util.onesec.ivr;

import java.util.concurrent.ExecutorService;

import org.ftoth.general.util.onesec.core.ObjectDescription;

public interface IvrEndpointConversation extends ObjectDescription
{
    public final static char EMPTY_DTMF = '-';
    public final static String DTMF_BINDING = "dtmf";
    public final static String DTMFS_BINDING = "dtmfs";
    public final static String CONVERSATION_STATE_BINDING = "conversationState";
    public final static String VARS_BINDING = "vars";
    public final static String NUMBER_BINDING = "number";
    public final static String CALLED_NUMBER_BINDING = "calledNumber";

    /**
     * Adds the listener to this conversation
     * @param listener the listener
     * @see #removeConversationListener
     */
    public void addConversationListener(IvrEndpointConversationListener listener);
    /**
     * Removes the listener from this conversation
     * @param listener the listener
     * @see #addConversationListener
     */
    public void removeConversationListener(IvrEndpointConversationListener listener);
    /**
     * Returns the executor service
     */
    public ExecutorService getExecutorService();
    /**
     * Returns the audio stream
     */
    public AudioStream getAudioStream();
    /**
     * Returns the incoming rtp stream of the conversation
     */
    public IncomingRtpStream getIncomingRtpStream();
    /**
     * Returns the number (address) of the calling side.
     */
    public String getCallingNumber();
    /**
     * Returns the number (address) of the called side.
     */
    public String getCalledNumber();
    /**
     * Continues the conversation with passed in the parameter dtmf char
     * @param dtmfChar the dtmf char
     */
    public void continueConversation(char dtmfChar);
    /**
     * Stops the current conversation
     */
    public void stopConversation(CompletionCode completionCode);
    /**
     * Returns the conversation scenario state
     */
    //public ConversationScenarioState getConversationScenarioState();
    /**
     * Returns the current conversation state
     */
    public IvrEndpointConversationState getState();
    /**
     * Transfers current call to the address passed in the parameter.
     * @param address The destination telephone address string to where the Call is being
     *      transferred
     * @param monitorTransfer if <code>true</code> then method will monitor call transfer, etc will
     *      wait until transfered call end
     * @param callStartTimeout
     * @param callEndTimeout
     */
    public void transfer(String address, boolean monitorTransfer, long callStartTimeout, long callEndTimeout);
    /**
     * Sends text message to the one of the conversation participants. 
     * @param message the message that will be sent to terminal
     * @param encoding in this encoding message will be sent to the terminal
     * @param direction point to the terminal to which the message will be sent
     */
    //public void sendMessage(String message, String encoding, SendMessageDirection direction);
    /**
     * Sends every char in <b>digits</b> as dtmf signals.
     */
    public void sendDTMF(String digits);
}
