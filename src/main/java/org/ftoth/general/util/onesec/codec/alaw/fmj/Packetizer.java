package org.ftoth.general.util.onesec.codec.alaw.fmj;

import java.util.logging.Logger;

import javax.media.Format;
import javax.media.format.AudioFormat;

/**
 * 
 * @author Ken Larson
 *
 */
public class Packetizer extends AbstractPacketizer
{
	private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(Decoder.class);

	private static final int PACKET_SIZE = 480;
	
	public String getName()
	{
		return "ALAW Packetizer";
	}
	
	public Packetizer()
	{
		super();
		this.inputFormats = new Format[] {new AudioFormat(AudioFormat.ALAW, -1.0, 8, 1, -1, -1, 8, -1.0, Format.byteArray)};
		
	}
	
	// TODO: move to base class?
	protected Format[] outputFormats = new Format[] {new AudioFormat(BonusAudioFormatEncodings.ALAW_RTP, -1.0, 8, 1, -1, -1, 8, -1.0, Format.byteArray)};
	
	public Format[] getSupportedOutputFormats(Format input)
	{
		if (input == null)
			return outputFormats;
		else
		{	
			if (!(input instanceof AudioFormat))
			{	
				log.warn(this.getClass().getSimpleName() + ".getSupportedOutputFormats: input format does not match, returning format array of {null} for " + input); // this can cause an NPE in JMF if it ever happens.
				return new Format[] {null};
			}
			final AudioFormat inputCast = (AudioFormat) input;
			if (!inputCast.getEncoding().equals(AudioFormat.ALAW) ||
				(inputCast.getSampleSizeInBits() != 8 && inputCast.getSampleSizeInBits() != Format.NOT_SPECIFIED) ||
				(inputCast.getChannels() != 1 && inputCast.getChannels() != Format.NOT_SPECIFIED) ||
				(inputCast.getFrameSizeInBits() != 8 && inputCast.getFrameSizeInBits() != Format.NOT_SPECIFIED)
				)
			{
				log.warn(this.getClass().getSimpleName() + ".getSupportedOutputFormats: input format does not match, returning format array of {null} for " + input); // this can cause an NPE in JMF if it ever happens.
				return new Format[] {null};
			}
			final AudioFormat result = new AudioFormat(BonusAudioFormatEncodings.ALAW_RTP, inputCast.getSampleRate(), 8,
					1, inputCast.getEndian(), inputCast.getSigned(), 8, 
					inputCast.getFrameRate(), inputCast.getDataType());

			return new Format[] {result};
		}
	}

	public void open()
	{
		setPacketSize(PACKET_SIZE);
	}
	
	public void close()
	{
	}
	
	public Format setInputFormat(Format arg0)
	{
		return super.setInputFormat(arg0);
	}

	public Format setOutputFormat(Format arg0)
	{
		return super.setOutputFormat(arg0);
	}

	
}
