package org.ftoth.jtapijmftest;

import com.sun.media.codec.audio.ulaw.Packetizer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ftoth.general.util.onesec.codec.alaw.titov.AlawEncoder;
import org.ftoth.general.util.onesec.codec.alaw.titov.AlawPacketizer;
import org.ftoth.general.util.onesec.codec.alaw.titov.UlawPacketizer;
import org.ftoth.general.util.onesec.codec.g729.G729Decoder;
import org.ftoth.general.util.onesec.codec.g729.G729Encoder;

import javax.media.Format;
import javax.media.PlugInManager;

public class TestCodecChange
{
	private static final Log log = LogFactory.getLog(TestCodecChange.class);
	
	private static Format alawRtpFormat;
	private static Format g729RtpFormat;
	
	public static void main(String[] args)
	{
		if (PlugInManager.removePlugIn(Packetizer.class.getName(), PlugInManager.CODEC)) {
			if (log.isDebugEnabled()) {
				log.debug("ULAW packetizier codec (with getControls() bug) ({}) successfully removed");
			}
		}
		UlawPacketizer up = new UlawPacketizer();
		PlugInManager.addPlugIn(UlawPacketizer.class.getName(), up.getSupportedInputFormats(), up.getSupportedOutputFormats(null), PlugInManager.CODEC);
		if (log.isDebugEnabled()) {
			log.debug("New ULAW packetizier codec (" + UlawPacketizer.class.getName() + ") successfully added");
		}

		AlawEncoder en = new AlawEncoder();
		PlugInManager.addPlugIn(AlawEncoder.class.getName(), en.getSupportedInputFormats(), en.getSupportedOutputFormats(null), PlugInManager.CODEC);
		if (log.isDebugEnabled()) {
			log.debug("ALAW codec (" + AlawEncoder.class.getName() + ") successfully added");
		}

		AlawPacketizer p = new AlawPacketizer();
		PlugInManager.addPlugIn(AlawPacketizer.class.getName(), p.getSupportedInputFormats(), p.getSupportedOutputFormats(null), PlugInManager.CODEC);
		if (log.isDebugEnabled()) {
			log.debug("ALAW packetizer codec (" + AlawPacketizer.class.getName() + ") successfully added");
		}

		G729Encoder g = new G729Encoder();
		PlugInManager.addPlugIn(G729Encoder.class.getName(), g.getSupportedInputFormats(), g.getSupportedOutputFormats(null), PlugInManager.CODEC);
		if (log.isDebugEnabled()) {
			log.debug("G729 encoder/packetizer (" + G729Encoder.class.getName() + ") successfully added");
		}

		G729Decoder d = new G729Decoder();
		PlugInManager.addPlugIn(G729Decoder.class.getName(), d.getSupportedInputFormats(), d.getSupportedOutputFormats(null), PlugInManager.CODEC);
		if (log.isDebugEnabled()) {
			log.debug("G729 decoder/depacketizer (" + G729Decoder.class.getName() + ") successfully added");
		}

		alawRtpFormat = p.getSupportedOutputFormats(null)[0];
		g729RtpFormat = g.getSupportedOutputFormats(null)[0];

		try {
			PlugInManager.commit();
		}
		catch (Exception e) {
			e.printStackTrace();
		}

	}
}
