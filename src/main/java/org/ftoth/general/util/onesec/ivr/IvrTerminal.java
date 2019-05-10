package org.ftoth.general.util.onesec.ivr;

import org.ftoth.general.util.onesec.Codec;
import org.ftoth.general.util.onesec.core.ObjectDescription;

public interface IvrTerminal extends ObjectDescription
{
    /**
     * Returns the terminal address (number)
     */
    public String getAddress();
    public RtpStreamManager getRtpStreamManager();
    public Codec getCodec();
    public Integer getRtpPacketSize();
    public Integer getRtpMaxSendAheadPacketsCount();
    public Boolean getEnableIncomingRtp();
    public Boolean getEnableIncomingCalls();
}
