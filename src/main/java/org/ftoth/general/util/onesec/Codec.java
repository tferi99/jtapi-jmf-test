package org.ftoth.general.util.onesec;

import javax.media.Format;
import javax.media.format.AudioFormat;

import org.ftoth.general.util.onesec.codec.alaw.titov.AlawAudioFormat;
import org.ftoth.general.util.onesec.codec.g729.G729AudioFormat;

import com.cisco.jtapi.extensions.CiscoMediaCapability;

/**
 *
 * @author Mikhail Titov
 */
public enum Codec
{
    AUTO(-1), G711_MU_LAW(0), G711_A_LAW(8), G729(11), LINEAR(100);

    private final int payload;
    private final AudioFormat audioFormat;
    private final CiscoMediaCapability[] ciscoMediaCapabilities;

    private Codec(int payload)
    {
        this.payload = payload;
        switch (payload){
            case 11 :
                ciscoMediaCapabilities = new CiscoMediaCapability[]{new CiscoMediaCapability(11, 60)};
                audioFormat = new G729AudioFormat(new AudioFormat(AudioFormat.G729_RTP, 8000d, 8, 1));
                break;
            case 8 :
                ciscoMediaCapabilities = new CiscoMediaCapability[]{new CiscoMediaCapability(2, 60)};
                audioFormat = new AudioFormat(AlawAudioFormat.ALAW_RTP, 8000d, 8, 1);
                break;
            case 100:
                ciscoMediaCapabilities = null;
                audioFormat = new AudioFormat(AudioFormat.LINEAR, 8000d, 8, 1);
                break;
            case -1 :
                ciscoMediaCapabilities = new CiscoMediaCapability[]{new CiscoMediaCapability(2, 60),
                    new CiscoMediaCapability(4, 60), new CiscoMediaCapability(11, 60)};
                audioFormat = null;
                break;
            default : 
                ciscoMediaCapabilities = new CiscoMediaCapability[]{new CiscoMediaCapability(4, 60)};
                audioFormat = new AudioFormat(AudioFormat.ULAW_RTP, 8000d, 8, 1);
                break;
        }
    }
    
    public static long getMillisecondsForFormat(Format format, int packetSizeInBytes) {
        String enc = format.getEncoding();
        if (AudioFormat.G729_RTP.equals(enc)) {
            return packetSizeInBytes;
        }
        else if (AlawAudioFormat.ALAW_RTP.equals(enc) || AudioFormat.ULAW_RTP.equals(enc)) {
            return packetSizeInBytes/8;
        }
        return 0;
    }
    
    public long getPacketSizeForMilliseconds(long ms) {
        switch (payload) {
//            case 11 : return ms;
            default : return ms * 8;    
        }
    }
    
    public long getMillisecondsForPacketSize(long packetSizeInBytes) {
        switch (payload) {
//            case 11 : return packetSizeInBytes;
            default : return packetSizeInBytes / 8;    
        }        
    }

    public int getPayload() {
        return payload;
    }

    public AudioFormat getAudioFormat() {
        return audioFormat;
    }

    public CiscoMediaCapability[] getCiscoMediaCapabilities() {
        return ciscoMediaCapabilities;
    }

    public static Codec getCodecByCiscoPayload(int ciscoPayload)
    {
        for (Codec codec: values())
            if (AUTO!=codec && codec.getCiscoMediaCapabilities()[0].getPayloadType()==ciscoPayload)
                return codec;
        return null;
    }
}
