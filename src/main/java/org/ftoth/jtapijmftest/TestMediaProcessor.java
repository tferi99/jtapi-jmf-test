package org.ftoth.jtapijmftest;

import com.sun.media.codec.audio.ulaw.Packetizer;
import com.sun.media.parser.audio.G729Parser;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ftoth.general.util.jmf.HeadlessAudioMux;
import org.ftoth.general.util.jmf.MediaProcessor;
import org.ftoth.general.util.jmf.MediaProcessor.CustomProcessing;
import org.ftoth.general.util.jmf.MediaProcessor.MediaProcessorConfig;
import org.ftoth.general.util.jmf.MediaProcessor.PresentingTarget;
import org.ftoth.general.util.jmf.g729.G729Packetizer;
import org.ftoth.general.util.onesec.codec.alaw.libjitsi.AlawAudioFormat;
import org.ftoth.general.util.onesec.codec.alaw.libjitsi.Constants;
import org.ftoth.general.util.onesec.codec.alaw.titov.UlawPacketizer;
import org.ftoth.general.util.onesec.codec.g729.G729AudioFormat;
import org.ftoth.general.util.onesec.codec.g729.G729Decoder;
import org.ftoth.general.util.onesec.codec.g729.G729Encoder;

import javax.media.Format;
import javax.media.PlugInManager;
import javax.media.format.AudioFormat;
import javax.media.protocol.FileTypeDescriptor;

public class TestMediaProcessor
{
	private static final Log log = LogFactory.getLog(TestMediaProcessor.class);

	public static void main(String[] args)
	{
		//initCustomCodecs();

		MediaProcessorConfig cfg = new MediaProcessorConfig();

		// ================================== processing ==================================
		//cfg = initTestProcessing();

		// -------------- convert from PCM -----------------
		//cfg = initPCM_To_ULaw();								// ok

		// -------------- convert to PCM -----------------
		//cfg = initULaw_To_PCM();								// ok

		// -------------- convert to g729 -----------------
		//cfg = initPCM_To_G729();								// ok



		//-------------- RTP transmit ---------------------
		//cfg = initUlawRTP();									// ok
		//cfg = initG729RTP();									// ok
		cfg = initAlawRTP();									// ok

		//cfg = initG729_To_729RTP();

        //cfg = initULaw_To_Player();
        //cfg.setInteractiveMode(true);

		// ================================== custom processing ==================================
		//cfg.setCustomProcessing(CustomProcessing.GAIN);

		// ================================== RTP client endpoint ==================================
		cfg.setRtpTargetAddress("10.122.188.255");
		cfg.setRtpTargetPort(22222);

		// ================================== is interactive? ==================================
		cfg.setInteractiveMode(true);


		MediaProcessor mp = new MediaProcessor(cfg);
		// media
		try {
			if (!mp.initAndStart()) {
				System.exit(1);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	// PCM -> uLaw file
	private static MediaProcessorConfig initPCM_To_ULaw()
	{
		MediaProcessorConfig cfg = new MediaProcessorConfig();

		cfg.setInputDataUrl("file:/c:/Users/ftoth/Documents/media/ring-8000Hz-16b-mono.wav");
		cfg.setContentType(new FileTypeDescriptor(FileTypeDescriptor.WAVE));
		cfg.setCustomProcessing(CustomProcessing.NONE);
		cfg.setDesiredOutputFormat(new AudioFormat(AudioFormat.ULAW, 8000, 8, 1, AudioFormat.LITTLE_ENDIAN, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.byteArray));
		cfg.setPresentingTarget(PresentingTarget.SINK);
		cfg.setOutputSinkDataUrl("file:/c:/Users/ftoth/Documents/media/ring-8000Hz-16b-mono-ulaw.wav");

		return cfg;
	}


	// PCM -> g729 file
	private static MediaProcessorConfig initPCM_To_G729()
	{
		MediaProcessorConfig cfg = new MediaProcessorConfig();

		cfg.setInputDataUrl("file:/c:/Users/ftoth/Documents/media/ring-8000Hz-16b-mono.wav");
		cfg.setContentType(new FileTypeDescriptor(HeadlessAudioMux.OUTPUT_FORMAT_HEADLESS_G729));
		cfg.setCustomProcessing(CustomProcessing.NONE);
		cfg.setDesiredOutputFormat(new G729AudioFormat(new AudioFormat(AudioFormat.G729_RTP, 8000, 8, 1, AudioFormat.LITTLE_ENDIAN, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.byteArray)));
		cfg.setPresentingTarget(PresentingTarget.SINK);
		cfg.setOutputSinkDataUrl("file:/c:/Users/ftoth/Documents/media/ring-8000Hz-16b-mono.g729");

		return cfg;
	}


	// ------------------------- some sample environment -------------------------
	// PCM -> uLaw RTP
	private static MediaProcessorConfig initUlawRTP()
	{
		MediaProcessorConfig cfg = new MediaProcessorConfig();
		
		cfg.setInputDataUrl("file:/c:/Users/ftoth/Documents/media/pcm-8000Hz-16b-mono.wav");
		cfg.setContentType(new FileTypeDescriptor(FileTypeDescriptor.RAW_RTP));
		cfg.setCustomProcessing(CustomProcessing.NONE);
		cfg.setDesiredOutputFormat(new AudioFormat(AudioFormat.ULAW_RTP, 8000, 8, 1, AudioFormat.LITTLE_ENDIAN, AudioFormat.UNSIGNED, 8, Format.NOT_SPECIFIED, Format.byteArray));
		cfg.setPresentingTarget(PresentingTarget.RTP);
		
		return cfg;
	}

	// PCM -> g729 RTP
	private static MediaProcessorConfig initG729RTP()
	{
		MediaProcessorConfig cfg = new MediaProcessorConfig();

		cfg.setInputDataUrl("file:/c:/Users/ftoth/Documents/media/pcm-8000Hz-16b-mono.wav");
		cfg.setContentType(new FileTypeDescriptor(FileTypeDescriptor.RAW_RTP));
		cfg.setCustomProcessing(CustomProcessing.NONE);
		cfg.setDesiredOutputFormat(new G729AudioFormat(new AudioFormat(AudioFormat.G729_RTP, 8000, 8, 1, AudioFormat.LITTLE_ENDIAN, Format.NOT_SPECIFIED, 8, Format.NOT_SPECIFIED, Format.byteArray)));
		cfg.setPresentingTarget(PresentingTarget.RTP);

		return cfg;
	}


	// PCM -> ALaw RTP
	private static MediaProcessorConfig initAlawRTP()
	{
		MediaProcessorConfig cfg = new MediaProcessorConfig();

		cfg.setInputDataUrl("file:/c:/Users/ftoth/Documents/media/pcm-8000Hz-16b-mono.wav");
		cfg.setContentType(new FileTypeDescriptor(FileTypeDescriptor.RAW_RTP));
		cfg.setCustomProcessing(CustomProcessing.NONE);
		AudioFormat f = new AudioFormat(Constants.ALAW_RTP, 8000,8,1, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, 8, 8000, Format.byteArray);
		cfg.setDesiredOutputFormat(f);
		cfg.setPresentingTarget(PresentingTarget.RTP);

		return cfg;
	}

	// g729 -> g729 RTP
	private static MediaProcessorConfig initG729_To_729RTP()
	{
		MediaProcessorConfig cfg = new MediaProcessorConfig();
		
		cfg.setInputDataUrl("file:/c:/Users/ftoth/Documents/media/out.g729");
		//inputDataUrl = "file:/c:/Users/ftoth/Documents/media/x.g729");
		
		cfg.setContentType(new FileTypeDescriptor(FileTypeDescriptor.RAW_RTP));
		cfg.setCustomProcessing(CustomProcessing.G729_RTP_FROM_RAW);
		
		//desiredOutputFormat = new G729AudioFormat(new AudioFormat(AudioFormat.G729_RTP, 8000, 8, 1, AudioFormat.LITTLE_ENDIAN, AudioFormat.SIGNED, G729Parser.CODEFRAMESIZE * 8, G729Parser.FRAMERATE, Format.byteArray)));
		cfg.setDesiredOutputFormat(new AudioFormat(AudioFormat.G729_RTP, 8000, 8, 1, AudioFormat.LITTLE_ENDIAN, AudioFormat.SIGNED, G729Parser.CODEFRAMESIZE * 8, G729Parser.FRAMERATE, Format.byteArray));
		
		cfg.setPresentingTarget(PresentingTarget.RTP);					
		
		return cfg;
	}


	// uLaw -> PCM file
	private static MediaProcessorConfig initULaw_To_PCM()
	{
		MediaProcessorConfig cfg = new MediaProcessorConfig();
		
		cfg.setInputDataUrl("file:/c:/Users/ftoth/Documents/media/3_ulaw.wav");		
		cfg.setContentType(new FileTypeDescriptor(FileTypeDescriptor.WAVE));
		cfg.setCustomProcessing(CustomProcessing.NONE);
		cfg.setDesiredOutputFormat(new AudioFormat(AudioFormat.LINEAR, 8000, 16, 1, AudioFormat.LITTLE_ENDIAN, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.byteArray));
		cfg.setPresentingTarget(PresentingTarget.SINK);
		cfg.setOutputSinkDataUrl("file:/c:/Users/ftoth/Documents/media/3_ulaw_to_pcm.wav");
		
		return cfg;
	}

    // uLaw -> PCM file
    private static MediaProcessorConfig initULaw_To_Player()
    {
        MediaProcessorConfig cfg = new MediaProcessorConfig();

        cfg.setInputDataUrl("file:/c:/Users/ftoth/Documents/media/3_ulaw.wav");
        cfg.setContentType(new FileTypeDescriptor(FileTypeDescriptor.WAVE));
        cfg.setCustomProcessing(CustomProcessing.NONE);
        cfg.setDesiredOutputFormat(new AudioFormat(AudioFormat.LINEAR, 8000, 16, 1, AudioFormat.LITTLE_ENDIAN, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.byteArray));

        cfg.setPresentingTarget(PresentingTarget.PLAYER);

        return cfg;
    }

	private static MediaProcessorConfig initTestProcessing()
	{
		MediaProcessorConfig cfg = new MediaProcessorConfig();
		
		//inputDataUrl = "file:/c:/Users/ftoth/Documents/media/out.g729");
		cfg.setInputDataUrl("file:/c:/Users/ftoth/Documents/media/x.g729");
		cfg.setContentType(new FileTypeDescriptor(FileTypeDescriptor.RAW_RTP));
		cfg.setCustomProcessing(CustomProcessing.TEST);
		cfg.setDesiredOutputFormat(new G729AudioFormat(new AudioFormat(AudioFormat.G729_RTP, 8000, 8, 1, AudioFormat.LITTLE_ENDIAN, Format.NOT_SPECIFIED, 8, Format.NOT_SPECIFIED, Format.byteArray)));
		//desiredOutputFormat = new AudioFormat(AudioFormat.G729_RTP, 8000, 8, 1, AudioFormat.LITTLE_ENDIAN, Format.NOT_SPECIFIED, 8, Format.NOT_SPECIFIED, Format.byteArray));
		cfg.setPresentingTarget(PresentingTarget.RTP);					
		
		return cfg;
	}

	private static void initCustomCodecs()
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

/*		AlawEncoder en = new AlawEncoder();
		PlugInManager.addPlugIn(AlawEncoder.class.getName(), en.getSupportedInputFormats(), en.getSupportedOutputFormats(null), PlugInManager.CODEC);
		if (log.isDebugEnabled()) {
			log.debug("ALAW codec (" + AlawEncoder.class.getName() + ") successfully added");
		}

		AlawPacketizer p = new AlawPacketizer();
		PlugInManager.addPlugIn(AlawPacketizer.class.getName(), p.getSupportedInputFormats(), p.getSupportedOutputFormats(null), PlugInManager.CODEC);
		if (log.isDebugEnabled()) {
			log.debug("ALAW packetizer codec (" + AlawPacketizer.class.getName() + ") successfully added");
		}*/

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

		G729Packetizer g729pck = new G729Packetizer();
		PlugInManager.addPlugIn(G729Packetizer.class.getName(), g729pck.getSupportedInputFormats(), g729pck.getSupportedOutputFormats(null), PlugInManager.CODEC);
		if (log.isDebugEnabled()) {
			log.debug("G729 Packetizer (" + G729Packetizer.class.getName() + ") successfully added");
		}

		G729Parser g729parser = new G729Parser();
		PlugInManager.addPlugIn(G729Parser.class.getName(), g729parser.getSupportedInputContentDescriptors(), null, PlugInManager.DEMULTIPLEXER);
		if (log.isDebugEnabled()) {
			log.debug("G729Parser (" + G729Parser.class.getName() + ") successfully added");
		}

/*		alawRtpFormat = p.getSupportedOutputFormats(null)[0];
		g729RtpFormat = g.getSupportedOutputFormats(null)[0];*/

		try {
			PlugInManager.commit();
		}
		catch (Exception e) {
			e.printStackTrace();
		}

	}
}
