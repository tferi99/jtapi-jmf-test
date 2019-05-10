package org.ftoth.general.util.jmf;

import java.awt.Dimension;
import java.io.IOException;
import java.net.InetAddress;

import javax.media.Codec;
import javax.media.Control;
import javax.media.Controller;
import javax.media.ControllerClosedEvent;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.Format;
import javax.media.MediaLocator;
import javax.media.NoProcessorException;
import javax.media.Owned;
import javax.media.Player;
import javax.media.Processor;
import javax.media.control.FormatControl;
import javax.media.control.QualityControl;
import javax.media.control.TrackControl;
import javax.media.format.AudioFormat;
import javax.media.format.VideoFormat;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.DataSource;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;
import javax.media.rtp.RTPManager;
import javax.media.rtp.SendStream;
import javax.media.rtp.SessionAddress;
import javax.media.rtp.rtcp.SourceDescription;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class RtpTransmit
{
	private static final Log log = LogFactory.getLog(RtpTransmit.class);
	
	//private static final int TRANSMIT_SECS = 60;
	
	
	// Input MediaLocator
	// Can be a file or http or capture source
	private MediaLocator locator;
	private String ipAddress;
	private int portBase;

	private Processor processor = null;
	private RTPManager rtpMgrs[];
	private DataSource dataOutput = null;

	// ------------------------- startup -------------------------
	public RtpTransmit(MediaLocator locator, String ipAddress, String pb, Format format)
	{

		this.locator = locator;
		this.ipAddress = ipAddress;
		Integer integer = Integer.valueOf(pb);
		if (integer != null)
			this.portBase = integer.intValue();
	}

	
	// ------------------------- actions -------------------------
	/**
	 * Starts the transmission. Returns null if transmission started ok.
	 * Otherwise it returns a string with the reason why the setup failed.
	 * @throws Exception 
	 */
	public synchronized String start() throws Exception
	{
		String result;

		// Create a processor for the specified media locator
		// and program it to output JPEG/RTP
		result = createProcessor();
		if (result != null)
			return result;

		// Create an RTP session to transmit the output of the
		// processor to the specified IP address and port no.
		result = createTransmitter();
		if (result != null) {
			processor.close();
			processor = null;
			return result;
		}

		// Start the transmission
		processor.start();

		return null;
	}

	/**
	 * Stops the transmission if already started
	 */
	public void stop()
	{
		synchronized (this) {
			if (processor != null) {
				processor.stop();
				processor.close();
				processor = null;
				for (int i = 0; i < rtpMgrs.length; i++) {
					rtpMgrs[i].removeTargets("Session ended.");
					rtpMgrs[i].dispose();
				}
			}
		}
	}

	// ------------------------- helpers -------------------------
	/**
	 * To create a processor, whose output is Raw RTP 
	 * @return error message, null if there is no error
	 * @throws Exception 
	 */
	private String createProcessor() throws Exception
	{
		if (log.isDebugEnabled()) {
			log.debug("------------------------------------------------ processor ------------------------------------------------");
		}
		
		//------------------------------------ input data source ------------------------------------------
		if (locator == null) {
			return "Locator is null";
		}
		DataSource ds;
		try {
			ds = javax.media.Manager.createDataSource(locator);
		}
		catch (Exception e) {
			e.printStackTrace();
			return "Couldn't create DataSource";
		}
		if (log.isDebugEnabled()) {
			log.debug(JmfUtil.dumpDataSource(ds, "input"));
		}
		
		//------------------------------------ processor ------------------------------------------
		// Try to create a processor to handle the input media locator
		try {
			processor = javax.media.Manager.createProcessor(ds);
			processor.addControllerListener(JmfUtil.createEventDumpControllerListener());
		}
		catch (NoProcessorException npe) {
			npe.printStackTrace();
			return "Couldn't create processor";
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
			return "IOException creating processor";
		}

		// Wait for it to configure
		boolean result = waitForState(processor, Processor.Configured);
		if (result == false) {
			return "Couldn't configure processor";
		}

		//------------------------------------ processor output ------------------------------------------
/*		if (log.isDebugEnabled()) {
			log.debug(JmfUtil.dumpProcessor(processor, "BEFORE setting output content type"));
		}*/
		// Set the output content descriptor to RAW_RTP
		// This will limit the supported formats reported from
		// Track.getSupportedFormats to only valid RTP formats.
		ContentDescriptor cd = new ContentDescriptor(ContentDescriptor.RAW_RTP);		// RAW_RTP 
		setContentDescriptor(processor, cd);
		
		//------------------------------------ processor tracks ------------------------------------------
		// Get the tracks from the processor
		TrackControl[] tracks = processor.getTrackControls();
		// Do we have at least one track?
		if (tracks == null || tracks.length < 1) {
			return "Couldn't find tracks in processor";
		}

		if (log.isDebugEnabled()) {
			log.debug(JmfUtil.dumpProcessor(processor, "BEFORE init tracks"));
		}
		
		// build codec chain 
		boolean atLeastOneTrack = false;
		//atLeastOneTrack = initTracksWithFirstSupportedFormat(tracks);
		atLeastOneTrack = initTracksWithCustomFormat(tracks);
		
		if (!atLeastOneTrack) {
			return "Couldn't set any of the tracks to a valid RTP format";
		}

		if (log.isDebugEnabled()) {
			log.debug(JmfUtil.dumpProcessor(processor, "AFTER init tracks"));
		}
		
		// Realize the processor. This will internally create a flow
		// graph and attempt to create an output datasource for JPEG/RTP
		// audio frames.
		result = waitForState(processor, Controller.Realized);
		if (result == false) {
			return "Couldn't realize processor";
		}

		// Set the JPEG quality to .5.
		setJPEGQuality(processor, 0.5f);

		// Get the output data source of the processor
		dataOutput = processor.getDataOutput();
		if (log.isDebugEnabled()) {
			log.debug(JmfUtil.dumpDataSource(dataOutput, "output"));
		}
		
		return null;	// everything is OK
	}


	/**
	 * Use the RTPManager API to create sessions for each media track of the
	 * processor.
	 */
	private String createTransmitter()
	{
		if (log.isDebugEnabled()) {
			log.debug("------------------------------------------------ transmitter ------------------------------------------------");
		}

		// Cheated. Should have checked the type.
		PushBufferDataSource pbds = (PushBufferDataSource) dataOutput;
		PushBufferStream pbss[] = pbds.getStreams();

		rtpMgrs = new RTPManager[pbss.length];
		SessionAddress localAddr, destAddr;
		InetAddress ipAddr;
		SendStream sendStream;
		int port;
		SourceDescription srcDesList[];

		log.debug("Transmitter input DataSource streams:");
		for (int i = 0; i < pbss.length; i++) {
			log.debug("    PushBufferDataSource[" + i + "] : " + pbss[i].toString());
			
			try {
				rtpMgrs[i] = RTPManager.newInstance();

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
				//localAddr = new SessionAddress(InetAddress.getLocalHost(), SessionAddress.ANY_PORT);

				destAddr = new SessionAddress(ipAddr, port);

				rtpMgrs[i].initialize(localAddr);

				rtpMgrs[i].addTarget(destAddr);

				System.err.println("Created RTP session: " + ipAddress + " " + port);

				sendStream = rtpMgrs[i].createSendStream(dataOutput, i);
				sendStream.start();
			}
			catch (Exception e) {
				e.printStackTrace();
				return e.getMessage();
			}
		}

		return null;
	}

	/**
	 * It enumerates tracks and checks supported formats. 
	 * Track will be disabled if no supported format found.
	 * 
	 * The first supported format will be set as output format.
	 * 
	 * @param tracks
	 * @return
	 */
	private boolean initTracksWithFirstSupportedFormat(TrackControl[] tracks)
	{
		boolean atLeastOneTrack = false;
		Format supported[];
		Format firstSupportedFormat;

		// Program the tracks		
		for (int n = 0; n < tracks.length; n++) {
			TrackControl track = tracks[n];
			Format format = track.getFormat();
			
			if (track.isEnabled()) {
				supported = track.getSupportedFormats();

				// We've set the output content to the RAW_RTP.
				// So all the supported formats should work with RTP.
				// We'll just pick the first one.

				if (supported.length > 0) {
					if (supported[0] instanceof VideoFormat) {
						// For video formats, we should double check the
						// sizes since not all formats work in all sizes.
						firstSupportedFormat = checkForVideoSizes(track.getFormat(), supported[0]);
					}
					else {
						firstSupportedFormat = supported[0];
					}
					if (log.isDebugEnabled()) {
						log.debug("Choosing the first supported format...");
					}
					track.setFormat(firstSupportedFormat);
					atLeastOneTrack = true;
				}
				else {
					track.setEnabled(false);
				}
			}
			else {
				track.setEnabled(false);
			}
		}
		return atLeastOneTrack;
	}
	
	private boolean initTracksWithCustomFormat(TrackControl[] tracks) throws Exception
	{
		boolean formatInited = false;
		
		AudioFormat format = new AudioFormat(AudioFormat.ULAW_RTP,		// ULAW_RTP, G729_RTP, DVI_RTP, GSM_RTP
				8000, 
				8, 
				1,
				AudioFormat.LITTLE_ENDIAN,
				AudioFormat.UNSIGNED,
				8,
				Format.NOT_SPECIFIED,
				Format.byteArray
		); 
		
		for (int i=0; i<tracks.length; i++) {
			TrackControl track = tracks[i];
			if (!formatInited) {
				// initialization of format
				if (((FormatControl)track).setFormat(format) == null) {
					track.setEnabled(false);		// format not supported
					continue;
				}
				formatInited = true;

				// initialization of codec
				Codec codec[] = null;
				
				// uLaw without encoding
				codec = new Codec[1];
				codec[0] = new com.sun.media.codec.audio.ulaw.Packetizer();
				((com.sun.media.codec.audio.ulaw.Packetizer)codec[0]).setPacketSize(160);

				// uLaw with encoding
/*				codec = new Codec[3];
				//codec[0] = new com.andtek.andphone.media.codec.audio.speex.Decoder();
				codec[0] = new StereoToMono();
				codec[1] = new com.ibm.media.codec.audio.ulaw.JavaEncoder();
				codec[2] = new com.sun.media.codec.audio.ulaw.Packetizer();
				((com.sun.media.codec.audio.ulaw.Packetizer)codec[2]).setPacketSize(160);*/
				
				// g729 without encoding
/*				codec = new Codec[1];
				codec[0] = new org.ftoth.general.util.jmf.g729.Packetizer();*/
				
				// g729 encoder
/*				codec = new Codec[1];
				codec[0] = new G729Encoder();*/
				
				
				// GSM
/*				codec = new Codec[3];
				codec[0] = new com.ibm.media.codec.audio.ulaw.JavaDecoder();
				codec[1] = new com.ibm.media.codec.audio.gsm.JavaEncoder();
				codec[2] = new com.ibm.media.codec.audio.gsm.Packetizer();*/

/*				codec = new Codec[2];
				codec[0] = new com.ibm.media.codec.audio.gsm.JavaEncoder();
				codec[1] = new com.ibm.media.codec.audio.gsm.Packetizer();*/
				
				((TrackControl)track).setCodecChain(codec);
			} else { 
				// if format inited other tracks can disabled 
				track.setEnabled(false); 
			} 
		}
		return formatInited;
	}

	
/*	private boolean initTracksWithCodecManager(TrackControl[] tracks) throws Exception
	{
		
	}*/
	
	private void setContentDescriptor(Processor processor2, ContentDescriptor cd)
	{
		if (log.isDebugEnabled()) {
			log.debug("Setting output content type of processor to: " + cd);
		}
		processor.setContentDescriptor(cd);
	}

	
	/****************************************************************
	 * Convenience methods to handle processor's state changes.
	 ****************************************************************/

	private Integer stateLock = new Integer(0);
	private boolean failed = false;

	Integer getStateLock()
	{
		return stateLock;
	}

	void setFailed()
	{
		failed = true;
	}

	private synchronized boolean waitForState(Processor p, int state)
	{
		p.addControllerListener(new StateListener());
		failed = false;

		// Call the required method on the processor
		if (state == Processor.Configured) {
			p.configure();
		}
		else if (state == Processor.Realized) {
			p.realize();
		}

		// Wait until we get an event that confirms the
		// success of the method, or a failure event.
		// See StateListener inner class
		while (p.getState() < state && !failed) {
			synchronized (getStateLock()) {
				try {
					getStateLock().wait();
				}
				catch (InterruptedException ie) {
					return false;
				}
			}
		}

		if (failed)
			return false;
		else
			return true;
	}

	/****************************************************************
	 * Inner Classes
	 ****************************************************************/

	class StateListener implements ControllerListener
	{

		public void controllerUpdate(ControllerEvent ce)
		{
			if (log.isTraceEnabled()) {
				log.trace(">>> ControllerEvent: " + ce.toString());
			}
			
			// If there was an error during configure or
			// realize, the processor will be closed
			if (ce instanceof ControllerClosedEvent)
				setFailed();

			// All controller events, send a notification
			// to the waiting thread in waitForState method.
			if (ce instanceof ControllerEvent) {
				synchronized (getStateLock()) {
					getStateLock().notifyAll();
				}
			}
		}
	}

	/****************************************************************
	 * Sample Usage for AVTransmit2 class
	 ****************************************************************/
/*
	public static void main(String[] args)
	{
		// We need three parameters to do the transmission
		// For example,
		// java AVTransmit2 file:/C:/media/test.mov 129.130.131.132 42050

		if (args.length < 3) {
			prUsage();
		}

		Format fmt = null;
		int i = 0;

		// Create a audio transmit object with the specified params.
		RtpTransmit at = new RtpTransmit(new MediaLocator(args[i]), args[i + 1], args[i + 2], fmt);
		// Start the transmission
		String result = null;
		try {
			result = at.start();
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		// result will be non-null if there was an error. The return
		// value is a String describing the possible error. Print it.
		if (result != null) {
			System.err.println("Error : " + result);
			System.exit(0);
		}

		System.err.println("Start transmission for " + TRANSMIT_SECS + " seconds...");

		// Transmit for 60 seconds and then close the processor
		// This is a safeguard when using a capture data source
		// so that the capture device will be properly released
		// before quitting.
		// The right thing to do would be to have a GUI with a
		// "Stop" button that would call stop on AVTransmit2
		try {
			Thread.currentThread().sleep(TRANSMIT_SECS * 1000);
		}
		catch (InterruptedException ie) {
		}

		// Stop the transmission
		at.stop();

		System.err.println("...transmission ended.");

		System.exit(0);
	}
*/
	static void prUsage()
	{
		System.err.println("Usage: AVTransmit2 <sourceURL> <destIP> <destPortBase>");
		System.err.println("     <sourceURL>: input URL or file name");
		System.err.println("     <destIP>: multicast, broadcast or unicast IP address for the transmission");
		System.err.println("     <destPortBase>: network port numbers for the transmission.");
		System.err.println("                     The first track will use the destPortBase.");
		System.err.println("                     The next track will use destPortBase + 2 and so on.\n");
		System.exit(0);
	}
	
	/**
	 * For JPEG and H263, we know that they only work for particular sizes. So
	 * we'll perform extra checking here to make sure they are of the right
	 * sizes.
	 */
	Format checkForVideoSizes(Format original, Format supported)
	{

		int width, height;
		Dimension size = ((VideoFormat) original).getSize();
		Format jpegFmt = new Format(VideoFormat.JPEG_RTP);
		Format h263Fmt = new Format(VideoFormat.H263_RTP);

		if (supported.matches(jpegFmt)) {
			// For JPEG, make sure width and height are divisible by 8.
			width = (size.width % 8 == 0 ? size.width : (int) (size.width / 8) * 8);
			height = (size.height % 8 == 0 ? size.height : (int) (size.height / 8) * 8);
		}
		else if (supported.matches(h263Fmt)) {
			// For H.263, we only support some specific sizes.
			if (size.width < 128) {
				width = 128;
				height = 96;
			}
			else if (size.width < 176) {
				width = 176;
				height = 144;
			}
			else {
				width = 352;
				height = 288;
			}
		}
		else {
			// We don't know this particular format. We'll just
			// leave it alone then.
			return supported;
		}

		return (new VideoFormat(null, new Dimension(width, height), Format.NOT_SPECIFIED, null, Format.NOT_SPECIFIED))
				.intersects(supported);
	}

	/**
	 * Setting the encoding quality to the specified value on the JPEG encoder.
	 * 0.5 is a good default.
	 */
	void setJPEGQuality(Player p, float val)
	{

		Control cs[] = p.getControls();
		QualityControl qc = null;
		VideoFormat jpegFmt = new VideoFormat(VideoFormat.JPEG);

		// Loop through the controls to find the Quality control for
		// the JPEG encoder.
		for (int i = 0; i < cs.length; i++) {

			if (cs[i] instanceof QualityControl && cs[i] instanceof Owned) {
				Object owner = ((Owned) cs[i]).getOwner();

				// Check to see if the owner is a Codec.
				// Then check for the output format.
				if (owner instanceof Codec) {
					Format fmts[] = ((Codec) owner).getSupportedOutputFormats(null);
					for (int j = 0; j < fmts.length; j++) {
						if (fmts[j].matches(jpegFmt)) {
							qc = (QualityControl) cs[i];
							qc.setQuality(val);
							System.err.println("- Setting quality to " + val + " on " + qc);
							break;
						}
					}
				}
				if (qc != null)
					break;
			}
		}
	}
}
