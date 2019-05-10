package org.ftoth.general.util.onesec.ivr.impl;

import java.io.IOException;
import java.net.InetAddress;

import javax.media.control.BufferControl;
import javax.media.rtp.RTPManager;
import javax.media.rtp.ReceiveStreamListener;
import javax.media.rtp.RemoteListener;
import javax.media.rtp.SendStream;
import javax.media.rtp.SendStreamListener;
import javax.media.rtp.SessionAddress;
import javax.media.rtp.SessionListener;
import javax.media.rtp.TransmissionStats;
import javax.media.rtp.event.ReceiveStreamEvent;
import javax.media.rtp.event.RemoteEvent;
import javax.media.rtp.event.SendStreamEvent;
import javax.media.rtp.event.SessionEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ftoth.general.util.onesec.ivr.AudioStream;
import org.ftoth.general.util.onesec.ivr.OutgoingRtpStream;
import org.ftoth.general.util.onesec.ivr.RTPManagerService;
import org.ftoth.general.util.onesec.ivr.RtpStreamException;

/**
 * 
 * @author Mikhail Titov
 */
public class OutgoingRtpStreamImpl extends AbstractRtpStream implements OutgoingRtpStream
{
	private static final Log log = LogFactory.getLog(IncomingRtpStreamImpl.class);

	private static RTPManagerService rtpManagerService;

	private AudioStream audioStream;
	private RTPManager rtpManager;
	private SendStream sendStream;
	private SessionAddress destAddress;

	public OutgoingRtpStreamImpl(InetAddress address, int portNumber, RTPManagerService rtpManagerService)
	{
		super(address, portNumber, "Outgoing RTP");

		this.rtpManagerService = rtpManagerService;
	}

	public long getHandledBytes()
	{
		return 0;
	}

	public long getHandledPackets()
	{
		return 0;
	}

	public void open(String remoteHost, int remotePort, AudioStream audioStream)
			throws RtpStreamException
	{
		try {
			this.remoteHost = remoteHost;
			this.remotePort = remotePort;
			if (log.isDebugEnabled()) {
				log.debug(logMess("Trying to open outgoing RTP stream to the remote host (%s) using port (%s)", remoteHost, remotePort));
			}
			this.audioStream = audioStream;
			destAddress = new SessionAddress(InetAddress.getByName(remoteHost), remotePort);
			rtpManager = rtpManagerService.createRtpManager();
			rtpManager.initialize(new SessionAddress(address, port));
			rtpManager.addTarget(destAddress);
			// Listener listener = new Listener();
			// rtpManager.addReceiveStreamListener(listener);
			// rtpManager.addRemoteListener(listener);
			// rtpManager.addSendStreamListener(listener);
			// rtpManager.addSessionListener(listener);
			sendStream = rtpManager.createSendStream(audioStream.getDataSource(), 0);
			sendStream.setBitRate(1);
			BufferControl control = (BufferControl) rtpManager.getControl(BufferControl.class.getName());
			control.setMinimumThreshold(60);
			control.setBufferLength(60);
			if (log.isDebugEnabled()) {
				log.debug(logMess("RTP stream was successfully opened to the remote host (%s) using port (%s)", remoteHost, remotePort));
			}
		}
		catch (Exception e) {
			throw new RtpStreamException(String.format("Outgoing RTP. Error opening RTP stream to remote host (%s) using port (%s)", remoteHost, remotePort), e);
		}
	}

	@Override
	public void doRelease()
			throws Exception
	{
		if (sendStream == null)
			return;
		TransmissionStats stats = sendStream.getSourceTransmissionStats();
		incHandledBytesBy(stats.getBytesTransmitted());
		incHandledPacketsBy(stats.getPDUTransmitted());
		try {
			try {
				try {
					audioStream.close();
				}
				finally {
					sendStream.close();
				}
			}
			finally {
				rtpManager.removeTarget(destAddress, "disconnected");
			}
		}
		finally {
			rtpManager.dispose();
		}

	}

	public void start()
			throws RtpStreamException
	{
		try {
			if (log.isDebugEnabled()) {
				log.debug(logMess("Starting rtp packets transmission..."));
			}
			sendStream.start();
			if (log.isDebugEnabled()) {
				log.debug(logMess("Rtp packets transmission started"));
			}
		}
		catch (IOException ex) {
			throw new RtpStreamException(String.format("Outgoing RTP. Error start outgoing rtp stream (remote address: %s; remote port: %s)", remoteHost, remotePort), ex);
		}
	}

	private class Listener implements ReceiveStreamListener, RemoteListener, SendStreamListener, SessionListener
	{

		public void update(ReceiveStreamEvent event)
		{
			if (log.isDebugEnabled()) {
				log.debug(logMess("ReceiveStreamListener event: %s", event.getClass().getName()));
			}
		}

		public void update(RemoteEvent event)
		{
			if (log.isDebugEnabled()) {
				log.debug(logMess("RemoteEvent event: %s", event.getClass().getName()));
			}
		}

		public void update(SendStreamEvent event)
		{
			if (log.isDebugEnabled()) {
				log.debug(logMess("SendStreamEvent event: %s", event.getClass().getName()));
			}
		}

		public void update(SessionEvent event)
		{
			if (log.isDebugEnabled()) {
				log.debug(logMess("SessionEvent event: %s", event.getClass().getName()));
			}
		}
	}
}
