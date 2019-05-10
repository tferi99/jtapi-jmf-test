package org.ftoth.general.util.jtapi;

import java.net.InetAddress;

import javax.telephony.Terminal;
import javax.telephony.TerminalConnection;
import javax.telephony.events.TermEv;

import com.cisco.jtapi.extensions.CiscoMediaOpenLogicalChannelEv;
import com.cisco.jtapi.extensions.CiscoRTPOutputProperties;
import com.cisco.jtapi.extensions.CiscoRTPOutputStartedEv;

public class TermEvUtil extends EvUtil
{


	public static String dump(TermEv ev)
	{
		StringBuilder b = new StringBuilder();
	
		Terminal term = ev.getTerminal();
		TerminalConnection[] tcs = term.getTerminalConnections();
		String tcStat;
		if (tcs != null && tcs.length > 0) {
			tcStat = TerminalConnectionUtil.getStateName(tcs[0].getState());
		}
		else {
			tcStat = "?";
		}
		b.append(EvUtil.dump(ev, "TermEv", false) + " - Terminal(" + tcStat + ")");
		
		// getting detail info for specific type 
		int id = ev.getID();
		switch(id) {
		case CiscoMediaOpenLogicalChannelEv.ID:
			break;
		case CiscoRTPOutputStartedEv.ID:
			b.append("\n");
			b.append(getCiscoRTPOutputStartedEvDetails((CiscoRTPOutputStartedEv)ev));
			break;
		}
		return b.toString();
	}
	
	
	private static String getCiscoRTPOutputStartedEvDetails(CiscoRTPOutputStartedEv ev)
	{
		CiscoRTPOutputProperties props = ev.getRTPOutputProperties();
		String payload = CiscoRTPPayloadUtil.getTypeName(props.getPayloadType());
		String bitrate = CiscoRTPBitRateUtil.getName(props.getBitRate());
		int pckSize = props.getPacketSize();
		InetAddress addr = props.getRemoteAddress();
		int port = props.getRemotePort();
		int framesPerPck = props.getMaxFramesPerPacket();
		
		return INHERITED_EVENT_PREFIX + "Call:" + ev.getCallID() + ", PayloadType:" + payload + ", BitRate:" + bitrate 
			+ ", PacketSize:" + pckSize + ", RemoteAddress:" + addr.toString() + ", RemotePort:" + port + ", MaxFramesPerPacket:" + framesPerPck;
	}
}
