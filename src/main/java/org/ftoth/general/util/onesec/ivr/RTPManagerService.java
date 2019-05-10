package org.ftoth.general.util.onesec.ivr;

import javax.media.rtp.RTPManager;

public interface RTPManagerService
{
    /**
     * Creates new RTPManager and adds additional payloads to it.
     */
    public RTPManager createRtpManager();
}
