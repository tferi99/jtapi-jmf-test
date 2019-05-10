package org.ftoth.jtapijmftest.listener;

import javax.telephony.Terminal;
import javax.telephony.TerminalConnection;
import javax.telephony.events.TermEv;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ftoth.general.util.jtapi.TerminalConnectionUtil;
import org.ftoth.jtapijmftest.exception.JtapiRuntimeException;

import com.cisco.jtapi.extensions.CiscoTerminalObserver;

public class CiscoTerminalObserverListener implements CiscoTerminalObserver 
{
	private static final Log log = LogFactory.getLog(CiscoTerminalObserverListener.class);
	
	private String listenerId;
	
	// ------------------------- startup -------------------------
	public CiscoTerminalObserverListener(String listenerId)
	{
		this.listenerId = listenerId;
	}
	
	@Override
	public void terminalChangedEvent(TermEv[] events)
	{
		for (TermEv ev : events) {
			try {
				writeEventLog(ev);
				eventHandler(ev);
			}
			catch (Exception e) {
				e.printStackTrace();
				throw new JtapiRuntimeException(e);
			}
		}
	}

	
	protected void writeEventLog(TermEv ev) throws Exception
	{
		if (log.isTraceEnabled()) {
			Terminal t = ev.getTerminal();
			log.trace(getEventLogMessage("CiscoTerminalObserver", ev, t));
		}
	}

	private void eventHandler(TermEv ev) throws Exception
	{
		
	}
	
	protected String getEventLogMessage(String observerName, TermEv ev, Terminal term)
	{
		TerminalConnection[] tcs = term.getTerminalConnections();
		String tcStat;
		if (tcs != null && tcs.length > 0) {
			tcStat = TerminalConnectionUtil.getStateName(tcs[0].getState());
		}
		else {
			tcStat = "?";
		}
		return observerName + " EVENT on listener[" + listenerId +" - CallEv[" +  ev.getID() + "] - " + ev + ", Terminal(" + tcStat + ")";		
	}
}	

