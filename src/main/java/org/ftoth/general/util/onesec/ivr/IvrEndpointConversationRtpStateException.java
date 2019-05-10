package org.ftoth.general.util.onesec.ivr;

public class IvrEndpointConversationRtpStateException extends IvrEndpointConversationException {

    public IvrEndpointConversationRtpStateException(String mess, String expectedStates, String currentState)
    {
        super(String.format("%s. Invalid RTP stream STATE. Expected one of (%s) but was (%s)"
                , mess, expectedStates, currentState));
    }
}
