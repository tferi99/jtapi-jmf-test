package org.ftoth.general.util.jmf;

import javax.media.Format;
import javax.media.format.AudioFormat;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.FileTypeDescriptor;

/**
 * @author ftoth
 *
 */public class HeadlessAudioMux extends com.sun.media.multiplexer.BasicMux
{
	public static final String OUTPUT_FORMAT_HEADLESS_LINEAR = "audio.linear";
	public static final String OUTPUT_FORMAT_HEADLESS_G729 = "audio.g729";
	public static final String OUTPUT_FORMAT_HEADLESS_ALAW = "audio.alaw";

	public HeadlessAudioMux()
	{
		supportedInputs = new Format[3];
		supportedInputs[0] = new AudioFormat(AudioFormat.LINEAR, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED,
				Format.NOT_SPECIFIED, Format.byteArray);
		supportedInputs[1] = new AudioFormat(AudioFormat.G729, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED,
				Format.NOT_SPECIFIED, Format.byteArray);
		supportedInputs[2] = new AudioFormat(AudioFormat.ALAW, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED,
				Format.NOT_SPECIFIED, Format.byteArray);

		supportedOutputs = new ContentDescriptor[3];
		supportedOutputs[0] = new FileTypeDescriptor(OUTPUT_FORMAT_HEADLESS_LINEAR);
		supportedOutputs[1] = new FileTypeDescriptor(OUTPUT_FORMAT_HEADLESS_G729);
		supportedOutputs[2] = new FileTypeDescriptor(OUTPUT_FORMAT_HEADLESS_ALAW);
	}

	@Override
	public String getName()
	{
		return "Headless Audio Multiplexer";
	}

	@Override
	public int setNumTracks(int nTracks)
	{
		// forcing track number to 1
		if (nTracks != 1) {
			return 1;
		}
		else {
			return super.setNumTracks(nTracks);
		}
	}
}

