package org.ftoth.general.util.onesec.ivr;

public interface IvrOutgoingRtpStartedEvent extends IvrEndpointConversationEvent
{
	public AudioStream getAudioStream();
}
