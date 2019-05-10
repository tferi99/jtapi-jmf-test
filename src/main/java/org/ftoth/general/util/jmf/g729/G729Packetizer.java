package org.ftoth.general.util.jmf.g729;

import javax.media.Codec;
import javax.media.Control;
import javax.media.Format;
import javax.media.ResourceUnavailableException;
import javax.media.format.AudioFormat;

import com.ibm.media.codec.audio.AudioPacketizer;
import com.sun.media.parser.audio.G729Parser;

public class G729Packetizer extends AudioPacketizer
{
	
	public G729Packetizer()
	{
		packetSize = 20;
		supportedInputFormats = new AudioFormat[]{
			// ORIG	
			//new AudioFormat(AudioFormat.G729, Format.NOT_SPECIFIED, 8, 1, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, 8, Format.NOT_SPECIFIED, Format.byteArray)
			
			new AudioFormat(AudioFormat.G729_RTP, Format.NOT_SPECIFIED, 8, 1, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, G729Parser.CODEFRAMESIZE * 8, Format.NOT_SPECIFIED, Format.byteArray)
			//new AudioFormat(AudioFormat.G729_RTP, Format.NOT_SPECIFIED, 8, 1, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, 8, Format.NOT_SPECIFIED, Format.byteArray)
		};
		
		defaultOutputFormats = new AudioFormat[] {
			// ORIG
			//new AudioFormat(AudioFormat.G729_RTP, 8000, 8, 1, AudioFormat.LITTLE_ENDIAN, AudioFormat.UNSIGNED, 8, Format.NOT_SPECIFIED, Format.byteArray)
			
			//new G729AudioFormat(new AudioFormat(AudioFormat.G729_RTP, 8000, 8, 1, AudioFormat.LITTLE_ENDIAN, AudioFormat.SIGNED, 160, G729Parser.FRAMERATE, Format.byteArray))
			new AudioFormat(AudioFormat.G729_RTP, 8000, 8, 1, AudioFormat.LITTLE_ENDIAN, AudioFormat.SIGNED, G729Parser.CODEFRAMESIZE * 8, G729Parser.FRAMERATE, Format.byteArray)				
				
		};
	}

	@Override
	public String getName() {
		PLUGIN_NAME = "G729 Packetizer";
		return PLUGIN_NAME;
	}

	@Override
	protected Format[] getMatchingOutputFormats(Format in) {

		AudioFormat af = (AudioFormat) in;
		supportedOutputFormats = new AudioFormat[] {
			// ORIG
			//new AudioFormat(AudioFormat.G729_RTP, af.getSampleRate(), 8, 1, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, 8, Format.NOT_SPECIFIED, Format.byteArray)
			
			new AudioFormat(AudioFormat.G729_RTP, 8000, 8, 1, AudioFormat.LITTLE_ENDIAN, AudioFormat.SIGNED, G729Parser.CODEFRAMESIZE * 8, G729Parser.FRAMERATE, Format.byteArray)				
			//new G729AudioFormat(new AudioFormat(AudioFormat.G729_RTP, af.getSampleRate(), 8, 1, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, 8, Format.NOT_SPECIFIED, Format.byteArray))
		};

		return supportedOutputFormats;
	}

	@Override
	public void open() throws ResourceUnavailableException {
		setPacketSize(packetSize);
		reset();
	}

	@Override
	public void close() {
	}

	@Override
	public java.lang.Object[] getControls() {
		if (controls == null) {
			controls = new Control[1];
			controls[0] = new PacketSizeAdapter(this, packetSize, true);
		}
		return controls;
	}

	public synchronized void setPacketSize(int newPacketSize) {
		packetSize = newPacketSize;

		sample_count = packetSize;

		if (history == null) {
			history = new byte[packetSize];
			return;
		}

		if (packetSize > history.length) {
			byte[] newHistory = new byte[packetSize];
			System.arraycopy(history, 0, newHistory, 0, historyLength);
			history = newHistory;
		}
	}
}

class PacketSizeAdapter
extends com.sun.media.controls.PacketSizeAdapter
{
	private int frameCount = packetSize / 80;

	public PacketSizeAdapter(Codec newOwner, int newPacketSize,
			boolean newIsSetable) {
		super(newOwner, newPacketSize, newIsSetable);
	}

	@Override
	public int setPacketSize(int numBytes)
	{
		packetSize = numBytes;
		frameCount = packetSize / 80;
		return packetSize;
	}
}