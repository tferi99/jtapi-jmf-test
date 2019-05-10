package org.ftoth.general.util.onesec.ivr.impl;

import org.ftoth.general.util.onesec.ivr.AudioStream;
import org.ftoth.general.util.onesec.ivr.IvrEndpointConversation;
import org.ftoth.general.util.onesec.ivr.IvrOutgoingRtpStartedEvent;

/**
 *
 * @author Mikhail Titov
 */
public class IvrOutgoingRtpStartedEventImpl extends IvrEndpointConversationEventImpl implements IvrOutgoingRtpStartedEvent
{
    private final AudioStream audioStream;

    public IvrOutgoingRtpStartedEventImpl(IvrEndpointConversation conversation) {
        super(conversation);
        this.audioStream = conversation.getAudioStream();
    }

    public AudioStream getAudioStream() {
        return audioStream;
    }
}
