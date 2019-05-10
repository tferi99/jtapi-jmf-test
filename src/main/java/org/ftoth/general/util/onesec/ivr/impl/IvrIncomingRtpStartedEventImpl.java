package org.ftoth.general.util.onesec.ivr.impl;

import org.ftoth.general.util.onesec.ivr.IncomingRtpStream;
import org.ftoth.general.util.onesec.ivr.IvrEndpointConversation;
import org.ftoth.general.util.onesec.ivr.IvrIncomingRtpStartedEvent;

/**
 *
 * @author Mikhail Titov
 */
public class IvrIncomingRtpStartedEventImpl extends IvrEndpointConversationEventImpl 
        implements IvrIncomingRtpStartedEvent
{
    private final IncomingRtpStream inRtp;

    public IvrIncomingRtpStartedEventImpl(IvrEndpointConversation conversation) {
        super(conversation);
        this.inRtp = conversation.getIncomingRtpStream();
    }

    public IncomingRtpStream getIncomingRtpStream() {
        return inRtp;
    }
}
