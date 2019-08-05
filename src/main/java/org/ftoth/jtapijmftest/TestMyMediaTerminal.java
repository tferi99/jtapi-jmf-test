package org.ftoth.jtapijmftest;

import com.cisco.jtapi.extensions.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ftoth.general.util.jmf.MediaProcessor;
import org.ftoth.general.util.jmf.MediaProcessor.CustomProcessing;
import org.ftoth.general.util.jmf.MediaProcessorConfig;
import org.ftoth.general.util.jmf.MediaProcessor.PresentingTarget;
import org.ftoth.general.util.jmf.MediaProcessorConfigImpl;
import org.ftoth.general.util.jtapi.CallEvUtil;
import org.ftoth.general.util.jtapi.TermEvUtil;
import org.ftoth.general.util.jtapi.TerminalUtil;

import javax.media.Format;
import javax.media.format.AudioFormat;
import javax.media.protocol.FileTypeDescriptor;
import javax.telephony.Address;
import javax.telephony.CallObserver;
import javax.telephony.Terminal;
import javax.telephony.TerminalObserver;
import javax.telephony.callcontrol.CallControlCallObserver;
import javax.telephony.callcontrol.CallControlConnection;
import javax.telephony.callcontrol.events.CallCtlConnOfferedEv;
import javax.telephony.callcontrol.events.CallCtlTermConnRingingEv;
import javax.telephony.events.CallEv;
import javax.telephony.events.ConnFailedEv;
import javax.telephony.events.TermEv;

public class TestMyMediaTerminal extends JtapiAppBase
{
	private static final Log log = LogFactory.getLog(TestMyMediaTerminal.class);	
	
	public static String[] appArgs;
	private String mediaUrl;
	
	public static void main(String[] args)
	{
		appArgs = args;

		//System.out.println(JmfUtil.dumpCodecs(true));
		
		TestMyMediaTerminal app = new TestMyMediaTerminal();
		app.dumpJtapi = false;
		app.start();

	}

	@Override
	protected void action()
			throws Exception
	{
		String rpDn = "9047";
		//this.mediaUrl = "file:/c:/Users/ftoth/Documents/media/easymoney64.wav"; 
		this.mediaUrl = "file:/c:/Users/ftoth/Documents/media/3_ulaw.wav";
		//this.mediaUrl = "file:/c:/Users/ftoth/Documents/media/Encoded.wav";
		//this.mediaUrl = "file:/c:/Users/ftoth/Documents/media/pcm-8000Hz-16b-mono.wav";
		
		if (log.isInfoEnabled()) {
			log.info("Registering RP on [" + rpDn + "]");
		}
		
		// getting terminal by DN
		Terminal t = TerminalUtil.getTerminalOfDN(getProvider(), rpDn);
		if (!(t instanceof CiscoRouteTerminal)) {
			throw new IllegalArgumentException("[" + rpDn + "] is not a CiscoRouteTerminal");
		}

		// setting capabilities and registering terminal
		CiscoRouteTerminal term = (CiscoRouteTerminal) t;
		CiscoMediaCapability[] cap = { 
				new CiscoG711MediaCapability(), 
				new CiscoG729MediaCapability()
		};		
		term.register(cap, CiscoRouteTerminal.DYNAMIC_MEDIA_REGISTRATION);
		
		// adding observers
		term.addObserver(createTerminalObserver());
		Address addr = t.getAddresses()[0];
		addr.addCallObserver(createCallObserver());

		if (log.isInfoEnabled()) {
			log.info("RP registered.");
		}
		
		while (true) {
			Thread.yield();
			sleep(100);
		}
	}

	private CallObserver createCallObserver()
	{
		return new CallControlCallObserver() {

			@Override
			public void callChangedEvent(CallEv[] events)
			{
				for (CallEv ev : events) {
					int id = ev.getID();

					if (log.isDebugEnabled()) {
						log.debug(CallEvUtil.dump(ev));
					}
					
					switch(id) {
					case CallCtlConnOfferedEv.ID:
						CallCtlConnOfferedEv ccev = (CallCtlConnOfferedEv) ev;

						CallControlConnection cconn = (CallControlConnection) ccev.getConnection();
						try {
							cconn.accept();
							if (log.isDebugEnabled()) {
								log.debug("    -------> Accepted");
							}
						}
						catch (Exception e) {
							e.printStackTrace();
						}
						break;
					//case TermConnRingingEv.ID:
					case CallCtlTermConnRingingEv.ID:
						//TermConnRingingEv tcev = (TermConnRingingEv) ev;
						CallCtlTermConnRingingEv tcev = (CallCtlTermConnRingingEv) ev;
						
						if (log.isDebugEnabled()) {
							String tname = tcev.getTerminalConnection().getTerminal().getName();
							log.debug("    -------> Answering(" + tname + ")");
						}
						try {
							tcev.getTerminalConnection().answer();
							log.debug("    -------> Answered.");
						}
						catch (Exception e) {
							e.printStackTrace();
						}
						break;
					case ConnFailedEv.ID:
						ConnFailedEv cev = (ConnFailedEv) ev;
						System.out.println(cev);
						break;
					}
				}
			}
			
		};
	}
	

	private TerminalObserver createTerminalObserver()
	{
		return new CiscoTerminalObserver() {

			@Override
			public void terminalChangedEvent(TermEv[] events)
			{
				for (TermEv ev : events) {
					if (log.isDebugEnabled()) {
						log.debug(TermEvUtil.dump(ev));
					}
					
					int id = ev.getID();					
					switch(id) {
					case CiscoMediaOpenLogicalChannelEv.ID:
						onCiscoMediaOpenLogicalChannel(ev);
						break;
					case CiscoRTPOutputStartedEv.ID:
						try {
							onCiscoRTPOutputStarted((CiscoRTPOutputStartedEv)ev);
						}
						catch (Exception e) {
							e.printStackTrace();
						}
						break;
					}
				}
			}
		};
	}

	private void onCiscoMediaOpenLogicalChannel(TermEv ev)
	{
	}
	
	private void onCiscoRTPOutputStarted(CiscoRTPOutputStartedEv ev) throws Exception
	{
		// creating and starting RTP transmitter
		String ipAddress = ev.getRTPOutputProperties().getRemoteAddress().getHostAddress();
		int pb = ev.getRTPOutputProperties().getRemotePort();
		
/*		RtpTransmit tr = new RtpTransmit(new MediaLocator(mediaUrl), ipAddress, Integer.toString(pb), null);
		tr.start();*/
		
		MediaProcessorConfig cfg = new MediaProcessorConfigImpl();
		cfg.setAutoStartProcessor(true);
		cfg.setRtpTargetAddress(ipAddress);
		cfg.setRtpTargetPort(pb);
		
		cfg.setInputDataUrl("file:/c:/Users/ftoth/Documents/media/pcm-8000Hz-16b-mono.wav");
		cfg.setOutputContentType(new FileTypeDescriptor(FileTypeDescriptor.RAW_RTP));
		cfg.setCustomProcessing(CustomProcessing.NONE);
		cfg.setDesiredOutputFormat(new AudioFormat(AudioFormat.ULAW_RTP, 8000, 8, 1, AudioFormat.LITTLE_ENDIAN, AudioFormat.UNSIGNED, 8, Format.NOT_SPECIFIED, Format.byteArray));
		cfg.setPresentingTarget(PresentingTarget.RTP);
		
		
		MediaProcessor tp = new MediaProcessor(cfg);
		
		tp.initAndStart();
	}
}
