package org.ftoth.general.util.jmf.effect;

import javax.media.Buffer;
import javax.media.Control;
import javax.media.Effect;
import javax.media.Format;
import javax.media.ResourceUnavailableException;
import javax.media.format.AudioFormat;

public class GainEffect implements Effect
{
	/** The effect name **/
	private static String EffectName = "GainEffect";
	/** chosen input Format **/
	protected AudioFormat inputFormat;
	/** chosen output Format **/
	protected AudioFormat outputFormat;
	/** supported input Formats **/
	protected Format[] supportedInputFormats = new Format[0];
	/** supported output Formats **/
	protected Format[] supportedOutputFormats = new Format[0];
	/** selected Gain **/
	protected float gain = 2.0F;

	/** initialize the formats **/
	public GainEffect()
	{
		supportedInputFormats = new Format[] { new AudioFormat(AudioFormat.LINEAR, Format.NOT_SPECIFIED, 16, Format.NOT_SPECIFIED, AudioFormat.LITTLE_ENDIAN, AudioFormat.SIGNED, 16,
				Format.NOT_SPECIFIED, Format.byteArray) };
		supportedOutputFormats = new Format[] { new AudioFormat(AudioFormat.LINEAR, Format.NOT_SPECIFIED, 16, Format.NOT_SPECIFIED, AudioFormat.LITTLE_ENDIAN, AudioFormat.SIGNED, 16,
				Format.NOT_SPECIFIED, Format.byteArray) };
	}

	/** get the resources needed by this effect **/
	public void open()
			throws ResourceUnavailableException
	{
	}

	/** free the resources allocated by this codec **/
	public void close()
	{
	}

	/** reset the codec **/
	public void reset()
	{
	}

	/** no controls for this simple effect **/
	public Object[] getControls()
	{
		return (Object[]) new Control[0];
	}

	/**
	 * Return the control based on a control type for the effect.
	 **/
	public Object getControl(String controlType)
	{
		try {
			Class cls = Class.forName(controlType);
			Object cs[] = getControls();
			for (int i = 0; i < cs.length; i++) {
				if (cls.isInstance(cs[i]))
					return cs[i];
			}
			return null;
		}
		catch (Exception e) { // no such controlType or such control
			return null;
		}
	}

	/************** format methods *************/
	/** set the input format **/
	public Format setInputFormat(Format input)
	{
		// the following code assumes valid Format
		inputFormat = (AudioFormat) input;
		return (Format) inputFormat;
	}

	/** set the output format **/
	public Format setOutputFormat(Format output)
	{
		// the following code assumes valid Format
		outputFormat = (AudioFormat) output;
		return (Format) outputFormat;
	}

	/** get the input format **/
	protected Format getInputFormat()
	{
		return inputFormat;
	}

	/** get the output format **/
	protected Format getOutputFormat()
	{
		return outputFormat;
	}

	/** supported input formats **/
	public Format[] getSupportedInputFormats()
	{
		return supportedInputFormats;
	}

	public Format[] getSupportedOutpuFormats()
	{
		return supportedOutputFormats;
	}
	
	/** output Formats for the selected input format **/
	public Format[] getSupportedOutputFormats(Format in)
	{
		if (!(in instanceof AudioFormat))
			return new Format[0];
		AudioFormat iaf = (AudioFormat) in;
		if (!iaf.matches(supportedInputFormats[0]))
			return new Format[0];
		AudioFormat oaf = new AudioFormat(AudioFormat.LINEAR, iaf.getSampleRate(), 16, iaf.getChannels(), AudioFormat.LITTLE_ENDIAN, AudioFormat.SIGNED, 16, Format.NOT_SPECIFIED, Format.byteArray);
		return new Format[] { oaf };
	}

	/** gain accessor method **/
	public void setGain(float newGain)
	{
		gain = newGain;
	}

	/** return effect name **/
	public String getName()
	{
		return EffectName;
	}

	/** do the processing **/
	public int process(Buffer inputBuffer, Buffer outputBuffer)
	{
		// == prolog
		byte[] inData = (byte[]) inputBuffer.getData();
		int inLength = inputBuffer.getLength();
		int inOffset = inputBuffer.getOffset();
		byte[] outData = validateByteArraySize(outputBuffer, inLength);
		int outOffset = outputBuffer.getOffset();
		int samplesNumber = inLength / 2;
		// == main
		for (int i = 0; i < samplesNumber; i++) {
			int tempL = inData[inOffset++];
			int tempH = inData[inOffset++];
			int sample = tempH | (tempL & 255);
			sample = (int) (sample * gain);
			if (sample > 32767) // saturate
				sample = 32767;
			else if (sample < -32768)
				sample = -32768;
			outData[outOffset++] = (byte) (sample & 255);
			outData[outOffset++] = (byte) (sample >> 8);
		}
		// == epilog
		updateOutput(outputBuffer, outputFormat, samplesNumber, 0);
		return BUFFER_PROCESSED_OK;
	}

	/**
	 * Utility: validate that the Buffer object's data size is at least newSize
	 * bytes.
	 * 
	 * @return array with sufficient capacity
	 **/
	protected byte[] validateByteArraySize(Buffer buffer, int newSize)
	{
		Object objectArray = buffer.getData();
		byte[] typedArray;
		if (objectArray instanceof byte[]) { // is correct type AND not null
			typedArray = (byte[]) objectArray;
			if (typedArray.length >= newSize) { // is sufficient capacity
				return typedArray;
			}
		}
		System.out.println(getClass().getName() + " : allocating byte[" + newSize + "] ");
		typedArray = new byte[newSize];
		buffer.setData(typedArray);
		return typedArray;
	}

	/** utility: update the output buffer fields **/
	protected void updateOutput(Buffer outputBuffer, Format format, int length, int offset)
	{

		outputBuffer.setFormat(format);
		outputBuffer.setLength(length);
		outputBuffer.setOffset(offset);
	}
}
