package org.ftoth.general.util.onesec.codec.alaw.titov;

import com.sun.media.codec.audio.ulaw.Packetizer;

public class UlawPacketizer extends Packetizer
{
    @Override
    public Object[] getControls()
    {
        if (controls==null || controls.length==0)
            controls = null;
        return super.getControls();
    }
}
