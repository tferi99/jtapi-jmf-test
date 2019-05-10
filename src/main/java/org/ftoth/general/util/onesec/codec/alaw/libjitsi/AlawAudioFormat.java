package org.ftoth.general.util.onesec.codec.alaw.libjitsi;

import javax.media.Format;
import javax.media.format.AudioFormat;

/**
 * 
 * @author Mikhail Titov
 */
public class AlawAudioFormat extends AudioFormat
{
	private static final long serialVersionUID = -3207617349647901284L;

	public final static String ALAW_RTP = "ALAW/rtp";
	//public final static String ALAW_RTP = "alaw";

	private double koef = 0d;

	public AlawAudioFormat(Format format)
	{
		super(ALAW_RTP);
		copy(format);
	}

	@Override
	public long computeDuration(long length)
	{
		if (koef == 0d)
			koef = (8000000 / sampleSizeInBits / channels / sampleRate);
		return (long) (length * koef * 1000L);
	}
}
