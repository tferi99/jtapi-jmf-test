package org.ftoth.general.util.jmf;

import com.ibm.media.codec.audio.PCMToPCM;
import com.sun.media.multiplexer.RTPSyncBufferMux;
import com.sun.media.parser.audio.G729Parser;
import com.sun.media.rtp.RTPSessionMgr;
import jmapps.util.JMFUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ftoth.general.util.jmf.g729.G729Packetizer;
import org.ftoth.general.util.onesec.codec.g729.G729Decoder;
import org.ftoth.general.util.onesec.codec.g729.G729Encoder;

import javax.media.*;
import javax.media.control.FormatControl;
import javax.media.control.TrackControl;
import javax.media.format.AudioFormat;
import javax.media.protocol.DataSource;
import javax.media.protocol.FileTypeDescriptor;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;
import javax.media.rtp.RTPManager;
import javax.media.rtp.ReceiveStreamListener;
import javax.media.rtp.SendStream;
import javax.media.rtp.SessionAddress;
import javax.media.rtp.event.ReceiveStreamEvent;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MediaProcessor implements ControllerListener, CustomProcessorHelper
{
	private static final long serialVersionUID = -4539142210811833627L;

	private static final Log log = LogFactory.getLog(MediaProcessor.class);

	public enum CustomProcessing {
		NONE, PCM_TO_PCM, ULAW, ULAW_RTP, G729, G729_RTP, G729_RTP_FROM_RAW, GSM, GSM_RTP, TEST
	}

	public enum PresentingTarget {
		NONE, SINK, RTP, PLAYER
	}

	MediaProcessorConfig config;
	
	private boolean savePerforming = false; // save performing right now
	private boolean mediaPlayFailed = false;
	private boolean endOfMedia = false;

	private int mediaPlayTimeout = 0;
	private Processor processor; // current processor
	private MediaProcessorUI ui;

	// ============================= startup parameters =============================
	private boolean jmfLogging = true;
	private String jmfLoggingDirectory = "/tmp/jmf";

	public boolean isJmfLogging()
	{
		return jmfLogging;
	}

	public void setJmfLogging(boolean jmfLogging)
	{
		this.jmfLogging = jmfLogging;
	}

	public String getJmfLoggingDirectory()
	{
		return jmfLoggingDirectory;
	}

	public void setJmfLoggingDirectory(String jmfLoggingDirectory)
	{
		this.jmfLoggingDirectory = jmfLoggingDirectory;
	}

	// ============================= startup =============================
	public MediaProcessor(MediaProcessorConfig config)
	{
		this.config = config;
		if (config.isInteractiveMode()) {
			ui = new MediaProcessorUI();
		}

		initCustomRtpCodecs();
	}

	public boolean initAndStart()
			throws Exception
	{
		savePerforming = false;

		// config
		FileTypeDescriptor contentType = config.getContentType();
		String inputDataUrl = config.getInputDataUrl();
		AudioFormat desiredOutputFormat = config.getDesiredOutputFormat();
		PresentingTarget presentingTarget = config.getPresentingTarget();
		String rtpTargetAddress = config.getRtpTargetAddress();
		CustomProcessing customProcessing = config.getCustomProcessing();
		int rtpTargetPort = config.getRtpTargetPort();
		
		// logging
		JmfUtil.configLogging(jmfLogging, jmfLoggingDirectory, false);
		

		// install custom plugins
		// initCustomPlugins();

		// codec manager to install custom plugins
		// CodecManager cm = new CodecManagerImpl();

		if (config.getPresentingTarget() == PresentingTarget.PLAYER) {
			config.setContentType(null);
		}

		if (log.isInfoEnabled()) {
			String ct = (contentType == null) ? "<RENDERING>" : contentType.toString();
			log.info("======================================================================");
			log.info("Input data URL: " + inputDataUrl);
			log.info("Output content type: " + ct);
			log.info("Desired output format: " + desiredOutputFormat);
			log.info("Custom processing mode: " + customProcessing);
			log.info("Presenting target: " + presentingTarget);
			switch (presentingTarget) {
			case SINK:
				log.info("Output sink URL: " + rtpTargetPort);
				break;
			case RTP:
				log.info("RTP host: " + rtpTargetAddress + ":" + rtpTargetPort);
				break;
			case NONE:
			case PLAYER:
				break;
			}
			log.info("======================================================================");
		}

		// dumping plugins
		if (log.isTraceEnabled()) {
			log.trace(JmfUtil.dumpPlugins(true));
		}

		// processor
		try {
			Player p;

			if (customProcessing == null || customProcessing == CustomProcessing.NONE) {
				processor = JmfFactory.createRealizedProcessor(inputDataUrl, desiredOutputFormat, contentType);

			}
			else {
				processor = JmfFactory.createCustomProcessor(inputDataUrl, contentType, null, this, this.getClass().getSimpleName());
			}

			if (processor == null) {
				log.error("!!!!!!!!!!!!!!!!!!!!!!!!!!!!! Processor cannot be created !!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
				return false;
			}

			if (log.isTraceEnabled()) {
				log.trace(JmfUtil.dumpProcessor(processor, "test"));
			}

			processor.addControllerListener(JmfUtil.createEventDumpControllerListener(this));
			p = processor;

			mediaPlayFailed = false;
			endOfMedia = false;

			// GUI control
			if (config.isInteractiveMode()) {
				ui.buildUI(p);

				// presentation of processor output
				presenting();

				// start
				if (config.isAutoStartProcessor()) {
					p.start();
				}
			}
			// without GUI, non-interactive
			else {
				// presentation of processor output
				presenting();

				synchronized (this) {
					p.start();
					while (!endOfMedia && !mediaPlayFailed) {
						try {
							if (mediaPlayTimeout > 0) {
								wait(mediaPlayTimeout);
							}
							else {
								wait();
							}
						}
						catch (InterruptedException e) {
							log.error("Error during media play - " + e.getMessage());
						}
					}
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		
		if (config.isInteractiveMode()) {
			ui.initGUI(config);
		}
		
		return true;
	}

	/**
	 * Sending output of processor somewhere.
	 * 
	 * @throws Exception
	 */
	private void presenting()
			throws Exception
	{
		DataSource out;
		switch (config.getPresentingTarget()) {
		case NONE:
			// do nothing after processing
			break;
		case SINK:
			// to send output of processor into sink specified by URL
			out = processor.getDataOutput();
			DataSink sink = JmfFactory.createAndOpenDataSink(out, config.getOutputSinkDataUrl(), JmfUtil.createEventDumpDataSinkListener());
			sink.start();
			savePerforming = true;
			break;
		case PLAYER:

			break;
		case RTP:
			startTransmitter(config.getRtpTargetAddress(), config.getRtpTargetPort());
			break;
		default:
			throw new IllegalArgumentException(config.getPresentingTarget() + " : not implemented presenting target");
		}
	}

	// ------------------------- implements ControllerListener -------------------------
	@Override
	public void controllerUpdate(ControllerEvent event)
	{
		if (event instanceof EndOfMediaEvent) {
			if (savePerforming == true) {
				// stop processor if data saved into file
				// via DataSink
				stopSaving();
			}
			endOfMedia = true;
			synchronized (this) {
				notifyAll();
			}
			
			if (log.isDebugEnabled()) {
				log.debug("--------------- End of media ---------------");
			}

		}
		else if (event instanceof ControllerErrorEvent) {
			log.error("Media play  error:" + event.toString());
			mediaPlayFailed = true;
		}
	}

	private void stopSaving()
	{
		if (log.isInfoEnabled()) {
			log.info("Saving completed.");
		}

		savePerforming = false;

		if (processor != null) {
			processor.stop();
			processor.close();
		}
		if (config.isInteractiveMode()) {
			ui.showMsgBox("Save completed into " + config.getOutputSinkDataUrl());
		}
	}

	// ------------------------- implements ProcessorInitializator -------------------------
	// to perform custom processing
	@Override
	public boolean initProcessing(Processor processor)
	{
		if (log.isInfoEnabled()) {
			log.info("Custom processing:" + config.getCustomProcessing());
		}

		AudioFormat desiredOutputFormat = config.getDesiredOutputFormat();
		CustomProcessing customProcessing = config.getCustomProcessing();
				
		TrackControl[] tracks = processor.getTrackControls();
		// Do we have at least one track?
		if (tracks == null || tracks.length < 1) {
			log.error("Couldn't find any tracks in processor");
			return false;
		}

		// setting properties of the FIRST usable track
		// of which format is compatible with the desired output format
		boolean foundFirtsUsableTrack = false;
		for (int n = 0; n < tracks.length; n++) {
			TrackControl track = tracks[n];

			if (foundFirtsUsableTrack) {
				// already found usable track
				// other track will be disabled
				track.setEnabled(false);
				continue;
			}

			// deciding if current track can provide desired output format
			if (((FormatControl) track).setFormat(desiredOutputFormat) == null) {
				track.setEnabled(false); // format not supported for this track -> disable it
				continue; // try another track
			}
			foundFirtsUsableTrack = true;
			if (log.isDebugEnabled()) {
				log.debug("Track[" + n + "] is usable for desired output format (" + desiredOutputFormat + ")");
			}

			List<Codec> codecs = new ArrayList<Codec>();

			switch (customProcessing) {
			case NONE:
				// no processor adding
				break;
			case PCM_TO_PCM:
				codecs.add(new com.ibm.media.codec.audio.PCMToPCM());
				break;
			case ULAW:
				codecs.add(new com.ibm.media.codec.audio.ulaw.JavaEncoder());
				break;
			case ULAW_RTP:
				codecs.add(new com.ibm.media.codec.audio.ulaw.JavaEncoder());
				codecs.add(new com.sun.media.codec.audio.ulaw.Packetizer());
				break;
			case G729:
			case G729_RTP:
				codecs.add(new G729Encoder());
				break;
			case G729_RTP_FROM_RAW:
				codecs.add(new G729Packetizer());
				break;
			case GSM:
				codecs.add(new com.ibm.media.codec.audio.gsm.JavaEncoder());
				break;
			case GSM_RTP:
				codecs.add(new com.ibm.media.codec.audio.gsm.JavaEncoder());
				codecs.add(new com.ibm.media.codec.audio.gsm.Packetizer());
				break;
			case TEST:
				codecs.add(new com.ibm.media.codec.audio.PCMToPCM());
				// codecs.add(new GainEffect());
				// codecs.add(new org.ftoth.general.util.onesec.codec.g729.G729Encoder());
				codecs.add(new G729Decoder());
				// codecs.add(new com.ibm.media.codec.audio.gsm.JavaEncoder());
				// codecs.add(new com.ibm.media.codec.audio.gsm.Packetizer());
				break;
			default:
				throw new IllegalArgumentException("Not implemented custom processing:" + customProcessing);
			}
			Codec codecsArr[] = codecs.toArray(new Codec[0]);

			if (log.isDebugEnabled()) {
				log.debug("Building codec chain:");
				for (int i = 0; i < codecsArr.length; i++) {
					Codec codec = codecsArr[i];
					log.debug("    - " + codec);
				}
			}

			try {
				if (codecsArr.length > 0) {
					((TrackControl) track).setCodecChain(codecsArr);
				}
			}
			catch (UnsupportedPlugInException e) {
				throw new RuntimeException("Error during adding codec chain", e);
			}
			catch (NotConfiguredError e) {
				throw new RuntimeException("Error during adding codec chain", e);
			}

			// !!!!!!!!!!!!!! IMPORTANT !!!!!!!!!!!!!!!
			// without any enabled track you will get a ResourceUnavailableEvent with 'input media not supported' message
			// default is enabled
			track.setEnabled(true);
		}

		if (!foundFirtsUsableTrack) {
			log.error("Couldn't find any usable tracks in processor for desired output format: " + desiredOutputFormat);
		}
		return foundFirtsUsableTrack;
	}

	@Override
	public boolean initRenderer(Processor processor)
	{
		// use default renderer
		// NOTE: to use a renderer you have to specify 'null' as content type
		// for processor

		/*
		 * if (log.isInfoEnabled()) { log.info("Custom rendering"); }
		 * 
		 * TrackControl[] tracks = processor.getTrackControls(); // Do we have at least one track? if (tracks == null || tracks.length < 1) { log.error("Couldn't find any tracks in processor"); return
		 * false; }
		 * 
		 * for (int n = 0; n<tracks.length; n++) { TrackControl track = tracks[n]; try { track.setRenderer(new JavaSoundRenderer()); } catch (UnsupportedPlugInException e) { throw new
		 * RuntimeException("Error during adding rendere", e); } catch (NotConfiguredError e) { throw new RuntimeException("Error during adding rendere", e); } }
		 */
		return true;
	}

	// ------------------------- helpers -------------------------
	private String startTransmitter(String ipAddress, int portBase)
	{
		if (log.isDebugEnabled()) {
			log.debug("--------------------------------------- transmitter ---------------------------------");
		}

		Map<Integer, Format> customFormats = initCustomFormatsForRtp();

		// Cheated. Should have checked the type.
		PushBufferDataSource pbds = (PushBufferDataSource) processor.getDataOutput();
		if (pbds == null) {
			throw new IllegalArgumentException("RTP Transmitter needs an input data source");
		}
		PushBufferStream pbss[] = pbds.getStreams();

		RTPManager[] rtpMgrs = new RTPManager[pbss.length];
		SessionAddress localAddr, destAddr;
		InetAddress ipAddr;
		SendStream sendStream;
		int port;

		log.debug("Transmitter input DataSource streams:");
		for (int i = 0; i < pbss.length; i++) {
			log.debug("    PushBufferDataSource[" + i + "] : " + pbss[i].toString());

			try {
				RTPManager mgr = RTPManager.newInstance();
				rtpMgrs[i] =  mgr;

				for (Integer payloadType : customFormats.keySet()) {
					Format fmt = customFormats.get(payloadType);
					mgr.addFormat(fmt, payloadType);
				}

				// The local session address will be created on the
				// same port as the the target port. This is necessary
				// if you use AVTransmit2 in conjunction with JMStudio.
				// JMStudio assumes - in a unicast session - that the
				// transmitter transmits from the same port it is receiving
				// on and sends RTCP Receiver Reports back to this port of
				// the transmitting host.

				port = portBase + 2 * i;
				ipAddr = InetAddress.getByName(ipAddress);

				localAddr = new SessionAddress(InetAddress.getLocalHost(), port);
				// localAddr = new SessionAddress(InetAddress.getLocalHost(), SessionAddress.ANY_PORT);

				destAddr = new SessionAddress(ipAddr, port);

				mgr.initialize(localAddr);
				mgr.addTarget(destAddr);

				System.err.println("Created RTP session: " + ipAddress + " " + port);

				sendStream = mgr.createSendStream(processor.getDataOutput(), i);
				sendStream.start();
			}
			catch (Exception e) {
				e.printStackTrace();
				return e.getMessage();
			}
		}

		return null;
	}

	public static class MediaProcessorConfig
	{
		// ------------------------- properties -------------------------

		// inputDataUrl : URL for input data source
		// contentType : You can use the Processor setContentDescriptor method to specify the format of the data output by the Processor.
		// Setting the output data format to null causes the media data to be rendered instead of output to
		// the Processor object's output DataSource.
		// customProcessing : to build custom plugin chains
		// desiredOutputFormat : expected output format of tracks
		// presentingTarget : destination action after processing to present processor output

		// ------------------ input ---------------------
		// private String inputDataUrl = "file:/c:/Users/ftoth/Documents/media/easymoney64.wav";
		// private String inputDataUrl = "file:/c:/Users/ftoth/Documents/media/3_ulaw.wav";
		// private String inputDataUrl = "file:/c:/Users/ftoth/Documents/media/Encoded.wav";
		// private String inputDataUrl = "file:/c:/Users/ftoth/Documents/media/pcm-8000Hz-8b-mono.wav";
		// private String inputDataUrl = "file:/c:/Users/ftoth/Documents/media/pcm-8000Hz-16b-mono.wav";
		// private String inputDataUrl = "file:/c:/Users/ftoth/Documents/media/pcm-8000Hz-16b-mono.pcm";
		// private String inputDataUrl = "file:/c:/Users/ftoth/Documents/media/short.wav";
		// private String inputDataUrl = "file:/c:/Users/ftoth/Documents/media/nobody.wav";
		// private String inputDataUrl = "file:/c:/Users/ftoth/Documents/media/nobody_gsm.wav";
		// private String inputDataUrl = "file:/c:/Users/ftoth/Documents/media/Sample.g729";
		// private String inputDataUrl = "file:/c:/Users/ftoth/Documents/media/Encoded.g729";
		private String inputDataUrl = "file:/c:/Users/ftoth/Documents/media/out.g729";
		// private String inputDataUrl = "file:/c:/Users/ftoth/Documents/media/out.pcm";
		// private String inputDataUrl = "file:/c:/Users/ftoth/Documents/media/x.g729";

		// ------------------ processing ---------------------
		// output content type - you may have to change this if you change presentingTarget
		// e.g. for FILE mode (saving into wav) you need FileTypeDescriptor.WAVE
		// and for RTP you need ContentDescriptor.RAW_RTP
		// WAVE, RAW, RAW_RTP, MIXED, CONTENT_UNKNOWN
		// private FileTypeDescriptor contentType = null; // for rendering, same if you choose PresentingTarget.PLAYER
		// private FileTypeDescriptor contentType = new FileTypeDescriptor(FileTypeDescriptor.WAVE);
		private FileTypeDescriptor contentType = new FileTypeDescriptor(FileTypeDescriptor.RAW_RTP);
		// private FileTypeDescriptor contentType = new FileTypeDescriptor(FileTypeDescriptor.RAW);
		// private FileTypeDescriptor contentType = new FileTypeDescriptor(HeadlessAudioMux.OUTPUT_FORMAT_HEADLESS_G729);
		// private FileTypeDescriptor contentType = new FileTypeDescriptor(HeadlessAudioMux.OUTPUT_FORMAT_HEADLESS_LINEAR);
		// private FileTypeDescriptor contentType = new FileTypeDescriptor("lofasz");

		// ------------------------- custom processing -------------------------
		// custom processing, if doCustomProcessing is true
		// NONE, ULAW_RTP, G729, ....
		// private CustomProcessing customProcessing = CustomProcessing.NONE;
		// private CustomProcessing customProcessing = CustomProcessing.G729;
		// private CustomProcessing customProcessing = CustomProcessing.TEST;
		private CustomProcessing customProcessing = CustomProcessing.G729_RTP_FROM_RAW;

		// ------------------------- desired output format -------------------------
		// desired output format for processing
		//
		// NOTE: not all output format can be specified for automatic processing,
		// e.g. the following formats cannot work here:
		// GSM, DVI, G723
		//
		// AudioFormat(encoding, sampleRate, sampleSizeInBits, channels, endian, signed, frameSizeInBits, frameRate, dataType)
		// ULAW, ULAW_RTP, G729_RTP, DVI_RTP, GSM_RTP
		//
		// LINEAR
		// private static AudioFormat desiredOutputFormat = new AudioFormat(AudioFormat.LINEAR, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED,
		// Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.byteArray);
		// private static AudioFormat desiredOutputFormat = new AudioFormat(AudioFormat.LINEAR, 8000, 8, 1, AudioFormat.LITTLE_ENDIAN, Format.NOT_SPECIFIED, 8, Format.NOT_SPECIFIED, Format.byteArray);
		// private static AudioFormat desiredOutputFormat = new AudioFormat(AudioFormat.LINEAR, 8000, 16, 1, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, 8, Format.NOT_SPECIFIED, Format.byteArray);
		// ULAW
		// private static AudioFormat desiredOutputFormat = new AudioFormat(AudioFormat.ULAW, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED,
		// Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.byteArray);
		// private static AudioFormat desiredOutputFormat = new AudioFormat(AudioFormat.ULAW, 8000, 8, 1, AudioFormat.LITTLE_ENDIAN, AudioFormat.UNSIGNED, 8, Format.NOT_SPECIFIED, Format.byteArray);
		// ULAW_RTP
		// private static AudioFormat desiredOutputFormat = new AudioFormat(AudioFormat.ULAW_RTP, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED,
		// Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.byteArray);
		// private static AudioFormat desiredOutputFormat = new AudioFormat(AudioFormat.ULAW_RTP, 8000, 8, 1, AudioFormat.LITTLE_ENDIAN, AudioFormat.UNSIGNED, 8, Format.NOT_SPECIFIED,
		// Format.byteArray);
		// G729
		// private static AudioFormat desiredOutputFormat = new AudioFormat(AudioFormat.G729, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED,
		// Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.byteArray);
		// private AudioFormat desiredOutputFormat = new AudioFormat(AudioFormat.G729, 8000, 8, 1, AudioFormat.LITTLE_ENDIAN, AudioFormat.UNSIGNED, 8, Format.NOT_SPECIFIED, Format.byteArray);
		// private AudioFormat desiredOutputFormat = new AudioFormat(AudioFormat.G729, 8000, 8, 1);
		// G729_RTP
		private AudioFormat desiredOutputFormat = new AudioFormat(AudioFormat.G729_RTP, 8000, 8, 1, AudioFormat.LITTLE_ENDIAN, AudioFormat.UNSIGNED, 8, Format.NOT_SPECIFIED, Format.byteArray);
		// GSM
		// private static AudioFormat desiredOutputFormat = new AudioFormat(AudioFormat.GSM_RTP, 8000, 0, 1, AudioFormat.LITTLE_ENDIAN, AudioFormat.UNSIGNED, 520, Format.NOT_SPECIFIED,
		// Format.byteArray);
		// private static AudioFormat desiredOutputFormat = new AudioFormat(AudioFormat.GSM_MS, 8000, 0, 1, AudioFormat.LITTLE_ENDIAN, AudioFormat.UNSIGNED, 520, Format.NOT_SPECIFIED,
		// Format.byteArray);

		
		/**
		 * Interactive mode with UI
		 */
		private boolean interactiveMode = false;

		/**
		 * Automatically start in interactive mode (only of interactiveMode is true)
		 */
		private boolean autoStartProcessor = true;
		
		// ------------------ output, presenting ---------------------
		private PresentingTarget presentingTarget = PresentingTarget.RTP; // sending output of processor to: NONE, SINK, RTP, PLAYER
		//private String rtpTargetAddress = "10.122.188.255";
		private String rtpTargetAddress = "192.168.8.255";
		private int rtpTargetPort = 22222;
		// private String outputSinkDataUrl = "file:/c:/Users/ftoth/Documents/media/out.wav";
		private String outputSinkDataUrl = "file:/c:/Users/ftoth/Documents/media/out.g729";

		// private String outputSinkDataUrl = "file:/c:/Users/ftoth/Documents/media/pcm-8000Hz-16b-mono.pcm";
		public String getInputDataUrl()
		{
			return inputDataUrl;
		}

		public void setInputDataUrl(String inputDataUrl)
		{
			this.inputDataUrl = inputDataUrl;
		}

		public FileTypeDescriptor getContentType()
		{
			return contentType;
		}

		public void setContentType(FileTypeDescriptor contentType)
		{
			this.contentType = contentType;
		}

		public CustomProcessing getCustomProcessing()
		{
			return customProcessing;
		}

		public void setCustomProcessing(CustomProcessing customProcessing)
		{
			this.customProcessing = customProcessing;
		}

		public AudioFormat getDesiredOutputFormat()
		{
			return desiredOutputFormat;
		}

		public void setDesiredOutputFormat(AudioFormat desiredOutputFormat)
		{
			this.desiredOutputFormat = desiredOutputFormat;
		}

		public PresentingTarget getPresentingTarget()
		{
			return presentingTarget;
		}

		public void setPresentingTarget(PresentingTarget presentingTarget)
		{
			this.presentingTarget = presentingTarget;
		}

		public String getRtpTargetAddress()
		{
			return rtpTargetAddress;
		}

		public void setRtpTargetAddress(String rtpTargetAddress)
		{
			this.rtpTargetAddress = rtpTargetAddress;
		}

		public int getRtpTargetPort()
		{
			return rtpTargetPort;
		}

		public void setRtpTargetPort(int rtpTargetPort)
		{
			this.rtpTargetPort = rtpTargetPort;
		}

		public String getOutputSinkDataUrl()
		{
			return outputSinkDataUrl;
		}

		public void setOutputSinkDataUrl(String outputSinkDataUrl)
		{
			this.outputSinkDataUrl = outputSinkDataUrl;
		}

		public boolean isInteractiveMode()
		{
			return interactiveMode;
		}

		public void setInteractiveMode(boolean interactiveMode)
		{
			this.interactiveMode = interactiveMode;
		}

		public boolean isAutoStartProcessor()
		{
			return autoStartProcessor;
		}

		public void setAutoStartProcessor(boolean autoStartProcessor)
		{
			this.autoStartProcessor = autoStartProcessor;
		}
	}

	private void initCustomPlugins()
	{
		// --------------------------------- removing all ----------------------------------------------------
		JmfUtil.removeAllPlugins();

		G729Encoder g = new G729Encoder();
		PlugInManager.addPlugIn(G729Encoder.class.getName(), g.getSupportedInputFormats(), g.getSupportedOutputFormats(null), PlugInManager.CODEC);
		if (log.isDebugEnabled()) {
			log.debug("G729 encoder/packetizer (" + G729Encoder.class.getName() + ") successfully added");
		}

		/*
		 * G729Decoder d = new G729Decoder(); PlugInManager.addPlugIn(G729Decoder.class.getName(), d.getSupportedInputFormats(), d.getSupportedOutputFormats(null), PlugInManager.CODEC); if
		 * (log.isDebugEnabled()) { log.debug("G729 decoder/depacketizer (" + G729Decoder.class.getName() + ") successfully added"); }
		 */

		/*
		 * GainEffect eff = new GainEffect(); PlugInManager.addPlugIn(GainEffect.class.getName(), eff.getSupportedInputFormats(), eff.getSupportedOutputFormats(null), PlugInManager.EFFECT); if
		 * (log.isDebugEnabled()) { log.debug("GainEffect (" + GainEffect.class.getName() + ") successfully added"); }
		 */
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

		HeadlessAudioMux g729mux = new HeadlessAudioMux();
		PlugInManager.addPlugIn(HeadlessAudioMux.class.getName(), g729mux.getSupportedInputFormats(), g729mux.getSupportedOutputContentDescriptors(null), PlugInManager.MULTIPLEXER);
		if (log.isDebugEnabled()) {
			log.debug(g729mux.getName() + " (" + HeadlessAudioMux.class.getName() + ") successfully added");
		}

		// ----------------------------------- testing some plugins ------------------------------
		PlugIn p;

		Demultiplexer demux = new com.sun.media.parser.audio.WavParser();
		PlugInManager.addPlugIn(com.sun.media.parser.audio.WavParser.class.getName(), demux.getSupportedInputContentDescriptors(), null, PlugInManager.DEMULTIPLEXER);
		if (log.isDebugEnabled()) {
			log.debug(demux.getName() + " (" + com.sun.media.parser.audio.WavParser.class.getName() + ") successfully added");
		}

		Multiplexer mux = new com.sun.media.multiplexer.audio.WAVMux();
		PlugInManager.addPlugIn(com.sun.media.multiplexer.audio.WAVMux.class.getName(), mux.getSupportedInputFormats(), mux.getSupportedOutputContentDescriptors(null), PlugInManager.MULTIPLEXER);
		if (log.isDebugEnabled()) {
			log.debug(mux.getName() + " (" + com.sun.media.multiplexer.audio.WAVMux.class.getName() + ") successfully added");
		}

		Renderer rend = new com.sun.media.renderer.audio.JavaSoundRenderer();
		PlugInManager.addPlugIn(com.sun.media.renderer.audio.JavaSoundRenderer.class.getName(), rend.getSupportedInputFormats(), null, PlugInManager.RENDERER);
		if (log.isDebugEnabled()) {
			log.debug(rend.getName() + " (" + com.sun.media.renderer.audio.JavaSoundRenderer.class.getName() + ") successfully added");
		}

		Codec pcm = new PCMToPCM();
		PlugInManager.addPlugIn(PCMToPCM.class.getName(), pcm.getSupportedInputFormats(), pcm.getSupportedOutputFormats(null), PlugInManager.CODEC);
		if (log.isDebugEnabled()) {
			log.debug(pcm.getName() + " (" + PCMToPCM.class.getName() + ") successfully added");
		}

		Multiplexer rtpMux = new RTPSyncBufferMux();
		PlugInManager.addPlugIn(RTPSyncBufferMux.class.getName(), rtpMux.getSupportedInputFormats(), rtpMux.getSupportedOutputContentDescriptors(null), PlugInManager.MULTIPLEXER);
		if (log.isDebugEnabled()) {
			log.debug(mux.getName() + " (" + RTPSyncBufferMux.class.getName() + ") successfully added");
		}
	}


	private void initCustomRtpCodecs()
	{
		// dummy (to access internal static format list via non-static addFormat() )
		RTPSessionMgr mgr = JMFUtils.createSessionManager("127.0.0.1", 65000, 32, new ReceiveStreamListener()
		{
			public void update(ReceiveStreamEvent receiveStreamEvent)
			{
				// do nothing
			}
		});

		Map<Integer, Format> customFormats = initCustomFormatsForRtp();

		for (Integer payloadType : customFormats.keySet()) {
			Format fmt = customFormats.get(payloadType);
			mgr.addFormat(fmt, payloadType);
		}
	}

	/**
	 * by FToth
	 *
	 * <p>
	 * To initialize custom media formats which will be added to RTP sessions.
	 *
	 */
	private Map<Integer, Format> initCustomFormatsForRtp()
	{
		Map<Integer, Format> formats = new HashMap<Integer, Format>();

		// ALAW
		org.ftoth.general.util.onesec.codec.alaw.libjitsi.Packetizer alawPck = new org.ftoth.general.util.onesec.codec.alaw.libjitsi.Packetizer();
		Format alawInFormat = (alawPck.getSupportedInputFormats())[0];
		Format alawRtpFormat = (alawPck.getSupportedOutputFormats(alawInFormat))[0];
		formats.put(8, alawRtpFormat);
		if (log.isDebugEnabled()) {
			log.debug("ALAW Packetizer (" + org.ftoth.general.util.onesec.codec.alaw.libjitsi.Packetizer.class.getName() + ") successfully added");
		}

		return formats;
	}
}
