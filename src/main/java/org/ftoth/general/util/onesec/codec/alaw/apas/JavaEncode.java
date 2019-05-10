package org.ftoth.general.util.onesec.codec.alaw.apas;

import javax.media.Buffer;
import javax.media.Control;
import javax.media.Format;
import javax.media.ResourceUnavailableException;
import javax.media.format.AudioFormat;

import com.ibm.media.codec.audio.AudioCodec;

public class JavaEncode extends AudioCodec {

	private Format lastFormat = null;
	private int inputSampleSize;
	private boolean bigEndian = false;

	public JavaEncode() {
		supportedInputFormats = new AudioFormat[]{
				new AudioFormat(
						AudioFormat.LINEAR,
						Format.NOT_SPECIFIED,
						16,
						1,
						Format.NOT_SPECIFIED,
						Format.NOT_SPECIFIED),
						new AudioFormat(
								AudioFormat.LINEAR,
								Format.NOT_SPECIFIED,
								8,
								1,
								Format.NOT_SPECIFIED,
								Format.NOT_SPECIFIED)};

		defaultOutputFormats = new AudioFormat[]{
				new AudioFormat(
						AudioFormat.ALAW,
						8000,
						8,
						1,
						Format.NOT_SPECIFIED,
						Format.NOT_SPECIFIED)};
	}

	@Override
	public String getName() {
		PLUGIN_NAME = "pcm to alaw converter";
		return PLUGIN_NAME;
	}

	@Override
	protected Format[] getMatchingOutputFormats(Format in) {

		AudioFormat inFormat = (AudioFormat) in;
		int sampleRate = (int) (inFormat.getSampleRate());
		supportedOutputFormats = new AudioFormat[]{
				new AudioFormat(
						AudioFormat.ALAW,
						sampleRate,
						8,
						1,
						Format.NOT_SPECIFIED,
						Format.NOT_SPECIFIED)};

		return supportedOutputFormats;
	}

	@Override
	public void open() throws ResourceUnavailableException {
	}

	@Override
	public void close() {
	}

	private int calculateOutputSize(int inputLength) {
		if (inputSampleSize == 16) {
			inputLength /= 2;
		}

		return inputLength;
	}

	private void initConverter(AudioFormat inFormat) {
		lastFormat = inFormat;
		inputSampleSize = inFormat.getSampleSizeInBits();
		bigEndian = inFormat.getEndian() == AudioFormat.BIG_ENDIAN;
	}

	public int process(Buffer inputBuffer, Buffer outputBuffer) {
		if (!checkInputBuffer(inputBuffer)) {
			return BUFFER_PROCESSED_FAILED;
		}

		if (isEOM(inputBuffer)) {
			propagateEOM(outputBuffer);
			return BUFFER_PROCESSED_OK;
		}

		Format newFormat = inputBuffer.getFormat();

		if (lastFormat != newFormat) {
			initConverter((AudioFormat) newFormat);
		}

		int outLength = calculateOutputSize(inputBuffer.getLength());

		byte[] inpData = (byte[]) inputBuffer.getData();
		byte[] outData = validateByteArraySize(outputBuffer, outLength);

		pcm162alaw(inpData, inputBuffer.getOffset(), outData, outputBuffer.getOffset(), outData.length, bigEndian);
		updateOutput(outputBuffer, outputFormat, outLength, outputBuffer.getOffset());
		return BUFFER_PROCESSED_OK;
	}

	private static final byte QUANT_MASK = 0xf; /* Quantization field mask. */

	private static final byte SEG_SHIFT = 4;
	/* Left shift for segment number. */
	private static final short[] seg_end = {
			0xFF, 0x1FF, 0x3FF, 0x7FF, 0xFFF, 0x1FFF, 0x3FFF, 0x7FFF
	};

	private static byte linear2alaw(short pcm_val) /* 2's complement (16-bit range) */ {
		byte mask;
		byte seg = 8;
		byte aval;

		if (pcm_val >= 0) {
			mask = (byte) 0xD5; /* sign (7th) bit = 1 */
		} else {
			mask = 0x55; /* sign bit = 0 */
			pcm_val = (short) (-pcm_val - 8);
		}

		/* Convert the scaled magnitude to segment number. */
		for (int i = 0; i < 8; i++) {
			if (pcm_val <= seg_end[i]) {
				seg = (byte) i;
				break;
			}
		}

		/* Combine the sign, segment, and quantization bits. */
		if (seg >= 8) /* out of range, return maximum value. */ {
			return (byte) ((0x7F ^ mask) & 0xFF);
		} else {
			aval = (byte) (seg << SEG_SHIFT);
			if (seg < 2) {
				aval |= (pcm_val >> 4) & QUANT_MASK;
			} else {
				aval |= (pcm_val >> (seg + 3)) & QUANT_MASK;
			}
			return (byte) ((aval ^ mask) & 0xFF);
		}
	}

	private static void pcm162alaw(byte[] inBuffer, int inByteOffset,
			byte[] outBuffer, int outByteOffset,
			int sampleCount, boolean bigEndian) {
		int shortIndex = inByteOffset;
		int alawIndex = outByteOffset;
		if (bigEndian) { //bigEndian
			while (sampleCount > 0) {
				outBuffer[alawIndex++] = linear2alaw(bytesToShort16(inBuffer[shortIndex],
						inBuffer[shortIndex + 1]));
				shortIndex++;
				shortIndex++;
				sampleCount--;
			}
		} else {
			while (sampleCount > 0) {
				outBuffer[alawIndex++] = linear2alaw(bytesToShort16(inBuffer[shortIndex + 1],
						inBuffer[shortIndex]));
				shortIndex++;
				shortIndex++;
				sampleCount--;
			}
		}
	}

	private static short bytesToShort16(byte highByte, byte lowByte) {
		return (short) ((highByte << 8) | (lowByte & 0xFF));
	}

	@Override
	public java.lang.Object[] getControls() {
		if (controls == null) {
			controls = new Control[1];
			controls[0] = new com.sun.media.controls.SilenceSuppressionAdapter(this, false, false);
		}
		return (Object[]) controls;
	}
}