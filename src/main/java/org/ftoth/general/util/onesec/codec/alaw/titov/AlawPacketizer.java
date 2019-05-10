package org.ftoth.general.util.onesec.codec.alaw.titov;

import javax.media.Format;
import javax.media.format.AudioFormat;

public class AlawPacketizer extends UlawPacketizer
{
    public AlawPacketizer()
    {
        this.supportedInputFormats = new AudioFormat[] {
            new AudioFormat(AudioFormat.ALAW, -1.0D, 8, 1, -1, -1, 8, -1.0D, Format.byteArray) };

        this.defaultOutputFormats = new AudioFormat[] {
            new AudioFormat(AlawAudioFormat.ALAW_RTP, -1.0D, 8, 1, -1, -1, 8, -1.0D, Format.byteArray) };

        this.PLUGIN_NAME = "A-Law Packetizer";
    }

    @Override
    protected Format[] getMatchingOutputFormats(Format in)
    {
        AudioFormat af = (AudioFormat)in;

        this.supportedOutputFormats = new AudioFormat[] {
            new AudioFormat(AlawAudioFormat.ALAW_RTP, af.getSampleRate(), 8, 1, -1, -1, 8, -1.0D, Format.byteArray) };

        return this.supportedOutputFormats;
    }

    @Override
    public Format setOutputFormat(Format format)
    {
        if (format instanceof AudioFormat && AlawAudioFormat.ALAW_RTP.equals(format.getEncoding()))
            format = new AlawAudioFormat(format);
        return super.setOutputFormat(format);
    }
}
