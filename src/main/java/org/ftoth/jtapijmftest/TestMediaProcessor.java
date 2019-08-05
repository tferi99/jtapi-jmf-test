package org.ftoth.jtapijmftest;

import com.sun.media.parser.audio.G729Parser;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ftoth.general.util.jmf.HeadlessAudioMux;
import org.ftoth.general.util.jmf.MediaProcessor;
import org.ftoth.general.util.jmf.MediaProcessor.CustomProcessing;
import org.ftoth.general.util.jmf.MediaProcessor.PresentingTarget;
import org.ftoth.general.util.jmf.MediaProcessorConfig;
import org.ftoth.general.util.jmf.MediaProcessorConfigImpl;
import org.ftoth.general.util.jmf.g729.G729Packetizer;
import org.ftoth.general.util.onesec.codec.alaw.libjitsi.Constants;
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

	// RTP
	//private static final String RTP_HOST = "10.122.188.255";
	private static final String RTP_HOST = "192.168.8.255";
	private static final int RTP_PORT = 22222;

	private static final String MEDIA_PATH_URL_PREFIX = "file:/c:/Users/ftoth/Documents/media";


	public static void main(String[] args)
	{
		initCustomPlugins();

		if (false) {
			MediaProcessorConfig cfg = new MediaProcessorConfigImpl();

			// ================================== processing ==================================
			//cfg = initTestProcessing();

			// -------------- convert file to file  -----------------
			//cfg = initPCM_to_UlawWav();
			//cfg = initUlaw_to_PCM();
			//cfg = initPCM_to_G729();
			//cfg = initPCM_to_Alaw();
			//cfg = initPCM_to_AlawWav();
			cfg = initUlawWav_to_AlawWav();

			//-------------- file to RTP transmit ---------------------
			//cfg = initPCM_to_UlawRTP();
			//cfg = initUlawWav_to_UlawRTP();
			// cfg = initPCM_to_G729RTP();
			//cfg = initG729_to_G729RTP();
			//cfg = initAlawWav_to_AlawRTP();

			//-------------- play from file ---------------------
			//cfg = initLinear_to_Player();
			//cfg = initUlawWav_to_Player();

			runMediaProcessor(cfg);
		}
		else {
			//-------------- format tests ---------------------
			//testUlawConvertAndPlay();
			//testAlawConvertAndPlay();
			//testG729ConvertAndPlay();

			//testG729ConvertAndRTP();
			//testUlawConvertAndRTP();
			testAlawConvertAndRTP();

			// ================================== custom processing ==================================
			//cfg.setCustomProcessing(CustomProcessing.GAIN);
		}
	}

	// ================================== general tasks ==================================
	private static void runMediaProcessor(MediaProcessorConfig cfg)
	{
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

	private static void playWav(String wavFile)
	{
		MediaProcessorConfig cfg = new MediaProcessorConfigImpl();
	 	cfg.setDesiredOutputFormat(new AudioFormat(AudioFormat.LINEAR, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.byteArray));
		playWav(wavFile, cfg);
	}

	private static void playWav(String wavFile, MediaProcessorConfig cfg)
	{
		cfg.setInputDataUrl(wavFile);
		cfg.setInteractiveMode(true);
		cfg.setPresentingTarget(PresentingTarget.PLAYER);
		runMediaProcessor(cfg);
	}


	// ================================== advanced processings ==================================
	private static void testUlawConvertAndPlay()
	{
		// convert
		MediaProcessorConfig cfg = initPCM_to_UlawWav();
		runMediaProcessor(cfg);

		// play
		playWav(cfg.getOutputSinkDataUrl());
	}

	private static void testAlawConvertAndPlay()
	{
		// convert
		MediaProcessorConfig cfg = initPCM_to_AlawWav();
		runMediaProcessor(cfg);

		// play
		playWav(cfg.getOutputSinkDataUrl());
	}

	private static void testG729ConvertAndPlay()
	{
		// convert
		MediaProcessorConfig cfg = initPCM_to_G729();
		runMediaProcessor(cfg);

		// play
		playWav(cfg.getOutputSinkDataUrl());
	}

	private static void testUlawConvertAndRTP()
	{
		// convert
		MediaProcessorConfig cfg = initPCM_to_UlawWav();
		runMediaProcessor(cfg);

		MediaProcessorConfig cfg2 = initUlawWav_to_UlawRTP();
		cfg2.setInputDataUrl(cfg.getOutputSinkDataUrl());
		runMediaProcessor(cfg2);
	}

	private static void testG729ConvertAndRTP()
	{
		MediaProcessorConfig cfg = initPCM_to_G729();
		runMediaProcessor(cfg);

		MediaProcessorConfig cfg2 = initG729_to_G729RTP();
		cfg2.setInputDataUrl(cfg.getOutputSinkDataUrl());
		runMediaProcessor(cfg2);
	}

	private static void testAlawConvertAndRTP()
	{
		// convert
		MediaProcessorConfig cfg = initPCM_to_AlawWav();
		runMediaProcessor(cfg);

		//MediaProcessorConfig cfg2 = initALawWav_to_AlawRTP();
		MediaProcessorConfig cfg2 = initAlawWav_to_AlawRTP();
		cfg2.setInputDataUrl(cfg.getOutputSinkDataUrl());
		runMediaProcessor(cfg2);
	}

	// PCM -> uLaw file
	private static MediaProcessorConfig initPCM_to_UlawWav()
	{
		String input = MEDIA_PATH_URL_PREFIX + "/fr-8000Hz-16b-mono-pcm.wav";
		String output = MEDIA_PATH_URL_PREFIX + "/ulaw/fr-8000Hz-16b-mono-ulaw.wav";
		FileTypeDescriptor contentType = new FileTypeDescriptor(FileTypeDescriptor.WAVE);
		Format outputFormat = new AudioFormat(AudioFormat.ULAW, 8000, 8, 1, AudioFormat.LITTLE_ENDIAN, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.byteArray);

		MediaProcessorConfig cfg = new MediaProcessorConfigImpl();
		cfg.setInputDataUrl(input);
		cfg.setOutputContentType(contentType);
		cfg.setDesiredOutputFormat(outputFormat);
		cfg.setPresentingTarget(PresentingTarget.SINK);
		cfg.setOutputSinkDataUrl(output);

		return cfg;
	}


	// PCM -> g729 file
	private static MediaProcessorConfig initPCM_to_G729()
	{
		String input = MEDIA_PATH_URL_PREFIX + "/fr-8000Hz-16b-mono-pcm.wav";
		String output = MEDIA_PATH_URL_PREFIX + "/g729/fr-8000Hz-16b-mono.g729";
		FileTypeDescriptor contentType = new FileTypeDescriptor(HeadlessAudioMux.OUTPUT_FORMAT_HEADLESS_G729);
		Format outputFormat = new G729AudioFormat(new AudioFormat(null, 8000, 8, 1, AudioFormat.LITTLE_ENDIAN, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.byteArray));

		MediaProcessorConfig cfg = new MediaProcessorConfigImpl();
		cfg.setInputDataUrl(input);
		cfg.setOutputContentType(contentType);
		cfg.setDesiredOutputFormat(outputFormat);
		cfg.setPresentingTarget(PresentingTarget.SINK);
		cfg.setOutputSinkDataUrl(output);

		return cfg;
	}

	// PCM -> alaw file
	private static MediaProcessorConfig initPCM_to_Alaw()
	{
		String input = MEDIA_PATH_URL_PREFIX + "/pcm-8000Hz-16b-mono.wav";
		String output = MEDIA_PATH_URL_PREFIX + "/_TEST/alaw/pcm-8000Hz-16b-mono.alaw";
		FileTypeDescriptor contentType = new FileTypeDescriptor(HeadlessAudioMux.OUTPUT_FORMAT_HEADLESS_ALAW);
		Format outputFormat = new AudioFormat(AudioFormat.ALAW, 8000, 8, 1, AudioFormat.LITTLE_ENDIAN, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.byteArray);

		MediaProcessorConfig cfg = new MediaProcessorConfigImpl();
		cfg.setInputDataUrl(input);
		cfg.setOutputContentType(contentType);
		cfg.setDesiredOutputFormat(outputFormat);
		cfg.setPresentingTarget(PresentingTarget.SINK);
		cfg.setOutputSinkDataUrl(output);

		return cfg;
	}

	// PCM -> uLaw file
	private static MediaProcessorConfig initPCM_to_AlawWav()
	{
		String input = MEDIA_PATH_URL_PREFIX + "/fr-8000Hz-16b-mono-pcm.wav";
		String output = MEDIA_PATH_URL_PREFIX + "/alaw/fr-8000Hz-16b-mono-alaw.wav";
		FileTypeDescriptor contentType = new FileTypeDescriptor(FileTypeDescriptor.WAVE);
		Format outputFormat = new AudioFormat(AudioFormat.ALAW, 8000, 8, 1, AudioFormat.LITTLE_ENDIAN, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.byteArray);

		MediaProcessorConfig cfg = new MediaProcessorConfigImpl();
		cfg.setInputDataUrl(input);
		cfg.setOutputContentType(contentType);
		cfg.setDesiredOutputFormat(outputFormat);
		cfg.setPresentingTarget(PresentingTarget.SINK);
		cfg.setOutputSinkDataUrl(output);

		return cfg;
	}


	// ------------------------- some sample environment -------------------------
	// PCM -> uLaw RTP
	private static MediaProcessorConfig initPCM_to_UlawRTP()
	{
		MediaProcessorConfig cfg = new MediaProcessorConfigImpl();
		
		cfg.setInputDataUrl(MEDIA_PATH_URL_PREFIX + "/pcm-8000Hz-16b-mono.wav");
		cfg.setOutputContentType(new FileTypeDescriptor(FileTypeDescriptor.RAW_RTP));
		cfg.setDesiredOutputFormat(new AudioFormat(AudioFormat.ULAW_RTP, 8000, 8, 1, AudioFormat.LITTLE_ENDIAN, AudioFormat.UNSIGNED, 8, Format.NOT_SPECIFIED, Format.byteArray));
		cfg.setPresentingTarget(PresentingTarget.RTP);
		
		return cfg;
	}

	// Ulaw Wav -> uLaw RTP
	private static MediaProcessorConfig initUlawWav_to_UlawRTP()
	{
		MediaProcessorConfig cfg = new MediaProcessorConfigImpl();

		cfg.setInputDataUrl(MEDIA_PATH_URL_PREFIX + "/ulaw/fr-8000Hz-16b-mono-ulaw.wav");
		cfg.setOutputContentType(new FileTypeDescriptor(FileTypeDescriptor.RAW_RTP));
		cfg.setDesiredOutputFormat(new AudioFormat(AudioFormat.ULAW_RTP, 8000, 8, 1, AudioFormat.LITTLE_ENDIAN, AudioFormat.UNSIGNED, 8, Format.NOT_SPECIFIED, Format.byteArray));
		cfg.setPresentingTarget(PresentingTarget.RTP);
		cfg.setRtpTargetAddress(RTP_HOST);
		cfg.setRtpTargetPort(RTP_PORT);
		cfg.setInteractiveMode(true);

		return cfg;
	}

	// PCM -> g729 RTP
	private static MediaProcessorConfig initPCM_to_G729RTP()
	{
		MediaProcessorConfig cfg = new MediaProcessorConfigImpl();

		cfg.setInputDataUrl(MEDIA_PATH_URL_PREFIX + "/pcm-8000Hz-16b-mono.wav");
		cfg.setOutputContentType(new FileTypeDescriptor(FileTypeDescriptor.RAW_RTP));
		cfg.setDesiredOutputFormat(new G729AudioFormat(new AudioFormat(AudioFormat.G729_RTP, 8000, 8, 1, AudioFormat.LITTLE_ENDIAN, Format.NOT_SPECIFIED, 8, Format.NOT_SPECIFIED, Format.byteArray)));
		cfg.setPresentingTarget(PresentingTarget.RTP);

		return cfg;
	}

	// g729 -> g729 RTP
	private static MediaProcessorConfig initG729_to_G729RTP()
	{
		MediaProcessorConfig cfg = new MediaProcessorConfigImpl();

		cfg.setInputDataUrl(MEDIA_PATH_URL_PREFIX + "/pcm-8000Hz-16b-mono.g729");
		cfg.setOutputContentType(new FileTypeDescriptor(FileTypeDescriptor.RAW_RTP));
		cfg.setDesiredOutputFormat(new G729AudioFormat(new AudioFormat(AudioFormat.G729_RTP, 8000, 8, 1, AudioFormat.LITTLE_ENDIAN, Format.NOT_SPECIFIED, 8, Format.NOT_SPECIFIED, Format.byteArray)));
		cfg.setPresentingTarget(PresentingTarget.RTP);
		cfg.setRtpTargetAddress(RTP_HOST);
		cfg.setRtpTargetPort(RTP_PORT);
		cfg.setInteractiveMode(true);

		return cfg;
	}


	// Alaw -> ALaw RTP
	// Custom codecs are registered here, you can use it with default jmf.properties
	private static MediaProcessorConfig initAlawWav_to_AlawRTP()
	{
		MediaProcessorConfig cfg = new MediaProcessorConfigImpl();

		cfg.setInputDataUrl(MEDIA_PATH_URL_PREFIX + "/alaw/fr-8000Hz-16b-mono-alaw.wav");
		cfg.setOutputContentType(new FileTypeDescriptor(FileTypeDescriptor.RAW_RTP));
		AudioFormat f = new AudioFormat(Constants.ALAW_RTP, Format.NOT_SPECIFIED,8,1, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, 8, Format.NOT_SPECIFIED, Format.byteArray);
		cfg.setDesiredOutputFormat(f);
		cfg.setPresentingTarget(PresentingTarget.RTP);
		cfg.setRtpTargetAddress(RTP_HOST);
		cfg.setRtpTargetPort(RTP_PORT);
		cfg.setInteractiveMode(true);

		return cfg;
	}

	// uLaw -> PCM file
	private static MediaProcessorConfig initUlaw_to_PCM()
	{
		MediaProcessorConfig cfg = new MediaProcessorConfigImpl();
		
		cfg.setInputDataUrl(MEDIA_PATH_URL_PREFIX + "/3_ulaw.wav");
		cfg.setOutputContentType(new FileTypeDescriptor(FileTypeDescriptor.WAVE));
		cfg.setDesiredOutputFormat(new AudioFormat(AudioFormat.LINEAR, 8000, 16, 1, AudioFormat.LITTLE_ENDIAN, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.byteArray));
		cfg.setPresentingTarget(PresentingTarget.SINK);
		cfg.setOutputSinkDataUrl(MEDIA_PATH_URL_PREFIX + "/3_ulaw_to_pcm.wav");
		
		return cfg;
	}

	// uLaw -> PCM file
	private static MediaProcessorConfig initUlawWav_to_AlawWav()
	{
		String input = MEDIA_PATH_URL_PREFIX + "/ulaw/fr-8000Hz-16b-mono-ulaw.wav";
		String output = MEDIA_PATH_URL_PREFIX + "/alaw/fr-8000Hz-16b-mono-alaw_from-ulaw.wav";
		FileTypeDescriptor contentType = new FileTypeDescriptor(FileTypeDescriptor.WAVE);
		Format outputFormat = new AudioFormat(AudioFormat.ALAW, 8000, 8, 1, AudioFormat.LITTLE_ENDIAN, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.byteArray);

		MediaProcessorConfig cfg = new MediaProcessorConfigImpl();
		cfg.setInputDataUrl(input);
		cfg.setOutputContentType(contentType);
		cfg.setDesiredOutputFormat(outputFormat);
		cfg.setPresentingTarget(PresentingTarget.SINK);
		cfg.setOutputSinkDataUrl(output);

		return cfg;
	}

	// uLaw -> PCM file
	private static MediaProcessorConfig initPCM_to_Player()
	{
		MediaProcessorConfig cfg = new MediaProcessorConfigImpl();

		cfg.setInputDataUrl(MEDIA_PATH_URL_PREFIX + "/3_ulaw.wav");
		cfg.setOutputContentType(new FileTypeDescriptor(FileTypeDescriptor.WAVE));
		cfg.setDesiredOutputFormat(new AudioFormat(AudioFormat.LINEAR, 8000, 16, 1, AudioFormat.LITTLE_ENDIAN, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.byteArray));

		cfg.setPresentingTarget(PresentingTarget.PLAYER);

		return cfg;
	}

    // uLaw -> PCM file
    private static MediaProcessorConfig initUlawWav_to_Player()
    {
        MediaProcessorConfig cfg = new MediaProcessorConfigImpl();

		cfg.setInputDataUrl(MEDIA_PATH_URL_PREFIX + "/3_ulaw.wav");
		cfg.setDesiredOutputFormat(new AudioFormat(AudioFormat.LINEAR, 8000, 16, 1, AudioFormat.LITTLE_ENDIAN, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.byteArray));
		cfg.setInteractiveMode(true);
        cfg.setPresentingTarget(PresentingTarget.PLAYER);

        return cfg;
    }

/*	// g729 -> PCM file
	private static MediaProcessorConfig initUlawWav_to_Player()
	{
		MediaProcessorConfig cfg = new MediaProcessorConfigImpl);

		cfg.setInputDataUrl(MEDIA_PATH_URL_PREFIX + "/3_ulaw.wav");
		cfg.setOutputContentType(new FileTypeDescriptor(FileTypeDescriptor.WAVE));
		cfg.setCustomProcessing(CustomProcessing.NONE);
		cfg.setDesiredOutputFormat(new AudioFormat(AudioFormat.LINEAR, 8000, 16, 1, AudioFormat.LITTLE_ENDIAN, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.byteArray));

		cfg.setPresentingTarget(PresentingTarget.PLAYER);

		return cfg;
	}*/

	private static MediaProcessorConfig initTestProcessing()
	{
		MediaProcessorConfig cfg = new MediaProcessorConfigImpl();
		
		//inputDataUrl = MEDIA_PATH_URL_PREFIX + "/out.g729");
		cfg.setInputDataUrl(MEDIA_PATH_URL_PREFIX + "/x.g729");
		cfg.setOutputContentType(new FileTypeDescriptor(FileTypeDescriptor.RAW_RTP));
		cfg.setCustomProcessing(CustomProcessing.TEST);
		cfg.setDesiredOutputFormat(new G729AudioFormat(new AudioFormat(AudioFormat.G729_RTP, 8000, 8, 1, AudioFormat.LITTLE_ENDIAN, Format.NOT_SPECIFIED, 8, Format.NOT_SPECIFIED, Format.byteArray)));
		//desiredOutputFormat = new AudioFormat(AudioFormat.G729_RTP, 8000, 8, 1, AudioFormat.LITTLE_ENDIAN, Format.NOT_SPECIFIED, 8, Format.NOT_SPECIFIED, Format.byteArray));
		cfg.setPresentingTarget(PresentingTarget.RTP);					
		
		return cfg;
	}

	/**
	 * Add a plugin here if you don't want to add it to registry config.
	 *
	 * If codec expects a not-supported RTP format you have to add it to RTPManager
	 * (see also {@link org.ftoth.general.util.jmf.MediaProcessor#initCustomFormatsForRtp})
	 */
	private static void initCustomPlugins()
	{
		// ------------------------- headless ----------------------------------
		HeadlessAudioMux g729mux = new HeadlessAudioMux();
		PlugInManager.addPlugIn(HeadlessAudioMux.class.getName(), g729mux.getSupportedInputFormats(), g729mux.getSupportedOutputContentDescriptors(null), PlugInManager.MULTIPLEXER);
		if (log.isDebugEnabled()) {
			log.debug(g729mux.getName() + " (" + HeadlessAudioMux.class.getName() + ") successfully added");
		}

		// ------------------------- g729 ----------------------------------
		G729Parser g729parser = new G729Parser();
		PlugInManager.addPlugIn(G729Parser.class.getName(), g729parser.getSupportedInputContentDescriptors(), null, PlugInManager.DEMULTIPLEXER);
		if (log.isDebugEnabled()) {
			log.debug("G729Parser (" + G729Parser.class.getName() + ") successfully added");
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

		G729Packetizer g729pck = new G729Packetizer();
		PlugInManager.addPlugIn(G729Packetizer.class.getName(), g729pck.getSupportedInputFormats(), g729pck.getSupportedOutputFormats(null), PlugInManager.CODEC);
		if (log.isDebugEnabled()) {
			log.debug("G729 Packetizer (" + G729Packetizer.class.getName() + ") successfully added");
		}


		// ------------------------- alaw ----------------------------------
		// custom codecs
		org.ftoth.general.util.onesec.codec.alaw.libjitsi.JavaEncoder en = new org.ftoth.general.util.onesec.codec.alaw.libjitsi.JavaEncoder();
		PlugInManager.addPlugIn(org.ftoth.general.util.onesec.codec.alaw.libjitsi.JavaEncoder.class.getName(), en.getSupportedInputFormats(), en.getSupportedOutputFormats(null), PlugInManager.CODEC);
		if (log.isDebugEnabled()) {
			log.debug("ALAW codec (" + org.ftoth.general.util.onesec.codec.alaw.libjitsi.JavaEncoder.class.getName() + ") successfully added");
		}

		org.ftoth.general.util.onesec.codec.alaw.libjitsi.Packetizer p = new org.ftoth.general.util.onesec.codec.alaw.libjitsi.Packetizer();
		PlugInManager.addPlugIn(org.ftoth.general.util.onesec.codec.alaw.libjitsi.Packetizer.class.getName(), p.getSupportedInputFormats(), p.getSupportedOutputFormats(null), PlugInManager.CODEC);
		if (log.isDebugEnabled()) {
			log.debug("ALAW packetizer codec (" + org.ftoth.general.util.onesec.codec.alaw.libjitsi.Packetizer.class.getName() + ") successfully added");
		}

/*		if (PlugInManager.removePlugIn(Packetizer.class.getName(), PlugInManager.CODEC)) {
			if (log.isDebugEnabled()) {
				log.debug("ULAW packetizier codec (with getControls() bug) ({}) successfully removed");
			}
		}
		UlawPacketizer up = new UlawPacketizer();
		PlugInManager.addPlugIn(UlawPacketizer.class.getName(), up.getSupportedInputFormats(), up.getSupportedOutputFormats(null), PlugInManager.CODEC);
		if (log.isDebugEnabled()) {
			log.debug("New ULAW packetizier codec (" + UlawPacketizer.class.getName() + ") successfully added");
		}*/

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
