package org.ftoth.general.util.jmf.g729;

import javax.media.Codec;
import javax.media.Control;
import javax.media.Format;
import javax.media.ResourceUnavailableException;
import javax.media.format.AudioFormat;

import com.ibm.media.codec.audio.AudioPacketizer;

public class PacketizerORIG extends AudioPacketizer {

	public PacketizerORIG() {
		packetSize = 20;
		supportedInputFormats = new AudioFormat[]{
				new AudioFormat(
						AudioFormat.G729,
						Format.NOT_SPECIFIED,
						8,
						1,
						Format.NOT_SPECIFIED,
						Format.NOT_SPECIFIED,
						8,	// 8
						Format.NOT_SPECIFIED,
						Format.byteArray)
		};
		defaultOutputFormats = new AudioFormat[]{
				new AudioFormat(
						AudioFormat.G729_RTP,
						Format.NOT_SPECIFIED,
						8,
						1,
						Format.NOT_SPECIFIED,
						Format.NOT_SPECIFIED,
						8,	// 8
						Format.NOT_SPECIFIED,
						Format.byteArray)
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
		supportedOutputFormats = new AudioFormat[]{
				new AudioFormat(
						AudioFormat.G729_RTP,
						af.getSampleRate(),
						8,
						1,
						Format.NOT_SPECIFIED,
						Format.NOT_SPECIFIED,
						8,
						Format.NOT_SPECIFIED,
						Format.byteArray)
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
			controls[0] = new PacketSizeAdapter2(this, packetSize, true);
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

class PacketSizeAdapter2 extends com.sun.media.controls.PacketSizeAdapter
{

	public PacketSizeAdapter2(Codec newOwner, int newPacketSize,
			boolean newIsSetable) {
		super(newOwner, newPacketSize, newIsSetable);
	}

	@Override
	public int setPacketSize(int numBytes) {

		int numOfPackets = numBytes;

		if (numOfPackets < 10) {
			numOfPackets = 10;
		}

		if (numOfPackets > 8000) {
			numOfPackets = 8000;
		}
		packetSize = numOfPackets;

		((G729Packetizer) owner).setPacketSize(packetSize);

		return packetSize;
	}
}