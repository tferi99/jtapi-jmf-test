package org.ftoth.general.util.jmf;

import com.ibm.media.codec.audio.PCMToPCM;
import com.sun.media.multiplexer.RTPSyncBufferMux;
import com.sun.media.parser.audio.G729Parser;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ftoth.general.util.jmf.effect.GainEffect;
import org.ftoth.general.util.jmf.g729.G729Packetizer;
import org.ftoth.general.util.onesec.codec.g729.G729Encoder;

import javax.media.*;
import javax.media.control.FormatControl;
import javax.media.control.TrackControl;
import javax.media.protocol.DataSource;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;
import javax.media.rtp.RTPManager;
import javax.media.rtp.SendStream;
import javax.media.rtp.SessionAddress;
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
		NONE, PCM_TO_PCM, ULAW, ULAW_RTP, G729, G729_RTP, G729_RTP_FROM_RAW, GSM, GSM_RTP, TEST, GAIN
	}

	public enum PresentingTarget {
		NONE, SINK, RTP, PLAYER
	}

	private boolean savePerforming = false; // save performing right now
	private boolean mediaPlayFailed = false;
	private boolean endOfMedia = false;

	private MediaProcessorConfig config;
	private int mediaPlayTimeout = 0;
	private Processor processor; // current processor
	private MediaProcessorUI ui;

	// ============================= startup parameters =============================
	private boolean jmfLogging = true;
	private String jmfLoggingDirectory = "/tmp";

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

		initCustomFormatsForProcessor();
	}

	public boolean initAndStart()
			throws Exception
	{
		savePerforming = false;

/*		// config
		FileTypeDescriptor contentType = config.getOutputContentType();
		String inputDataUrl = config.getInputDataUrl();
		Format desiredOutputFormat = config.getDesiredOutputFormat();
		outputSinkDataUrl
		PresentingTarget presentingTarget = config.getPresentingTarget();
		String rtpTargetAddress = config.getRtpTargetAddress();
		int rtpTargetPort = config.getRtpTargetPort();*/
		CustomProcessing customProcessing = config.getCustomProcessing();

		// logging
		JmfUtil.configLogging(jmfLogging, jmfLoggingDirectory, false);
		

		// install custom plugins
		// initCustomPlugins();

		// codec manager to install custom plugins
		// CodecManager cm = new CodecManagerImpl();

		if (config.getPresentingTarget() == PresentingTarget.PLAYER) {
			config.setOutputContentType(null);
		}

		if (log.isInfoEnabled()) {
			String ct = (config.getOutputContentType() == null) ? "<RENDERING>" : config.getOutputContentType().toString();
			log.info("======================================================================");
			log.info("Input data URL: " + config.getInputDataUrl());
			log.info("Output content type: " + ct);
			log.info("Desired output format: " + config.getDesiredOutputFormat());
			log.info("Custom processing mode: " + customProcessing);
			log.info("Presenting target: " + config.getPresentingTarget());
			switch (config.getPresentingTarget()) {
			case SINK:
				log.info("Output sink URL: " + config.getOutputSinkDataUrl());
				break;
			case RTP:
				log.info("RTP host: " + config.getRtpTargetAddress() + ":" + config.getRtpTargetPort());
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
				processor = JmfFactory.createRealizedProcessor(config.getInputDataUrl(), config.getDesiredOutputFormat(), config.getOutputContentType());

			}
			else {
				processor = JmfFactory.createCustomProcessor(config.getInputDataUrl(), config.getOutputContentType(), null, this, this.getClass().getSimpleName());
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

		Format desiredOutputFormat = config.getDesiredOutputFormat();
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
			case GAIN:
				codecs.add(new GainEffect());
				break;
			case TEST:
				//codecs.add(new com.ibm.media.codec.audio.PCMToPCM());
				// codecs.add(new org.ftoth.general.util.onesec.codec.g729.G729Encoder());
				//codecs.add(new G729Decoder());
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

				// adding custom formats to RTPManager
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

	/**
 	 * RTPManager.addFormat(...) is used to add a dynamic payload to format mapping to the RTPManager.
	 * The RTPManager maintains all static payload numbers and their correspnding formats as
 	 * mentioned in the Audio/Video profile document.
	 */
	private void initCustomFormatsForProcessor()
	{
		// ------------------- RTP formats -------------------
		// dummy (to access internal static format list via non-static addFormat() )
		RTPManager mgr = RTPManager.newInstance();
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
