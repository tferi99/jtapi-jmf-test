package org.ftoth.general.util.onesec.ivr;

public interface IvrIncomingRtpStartedEvent extends IvrEndpointConversationEvent {
    public IncomingRtpStream getIncomingRtpStream();
}
