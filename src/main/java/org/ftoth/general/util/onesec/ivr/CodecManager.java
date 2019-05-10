package org.ftoth.general.util.onesec.ivr;

import javax.media.Demultiplexer;
import javax.media.Format;
import javax.media.Multiplexer;
import javax.media.format.AudioFormat;

public interface CodecManager {
    public CodecConfig[] buildCodecChain(AudioFormat inFormat, AudioFormat outFormat) throws CodecManagerException;
    public Demultiplexer buildDemultiplexer(String contentType);
    public Multiplexer buildMultiplexer(String contentType);
    public Format getAlawRtpFormat();
    public Format getG729RtpFormat();
}
