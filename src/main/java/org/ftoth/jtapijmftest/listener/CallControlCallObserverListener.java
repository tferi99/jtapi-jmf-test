package org.ftoth.jtapijmftest.listener;

import javax.telephony.Call;
import javax.telephony.callcontrol.CallControlCallObserver;
import javax.telephony.callcontrol.events.CallCtlCallEv;
import javax.telephony.callcontrol.events.CallCtlConnOfferedEv;
import javax.telephony.events.CallEv;
import javax.telephony.events.TermConnRingingEv;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.cisco.jtapi.extensions.CiscoConnection;

public abstract class CallControlCallObserverListener extends CallObserverListener implements CallControlCallObserver
{
	private static final Log log = LogFactory.getLog(CallControlCallObserverListener.class);
	
	// ------------------------- startup -------------------------
	public CallControlCallObserverListener(String listenerId)
	{
		super(listenerId);
	}

	@Override
	protected void writeEventLog(CallEv ev) throws Exception
	{
		if (ev instanceof CallCtlCallEv) {
			if (log.isTraceEnabled()) {
				Call call = ev.getCall();
				log.trace(getEventLogMessage("CallControlCallObserver", ev, call));
			}
		}
		else {
			super.writeEventLog(ev);
		}
	}



	@Override
	protected void eventHandler(CallEv ev) throws Exception
	{
		int id = ev.getID();
		switch(id) {
		case CallCtlConnOfferedEv.ID:
			CallCtlConnOfferedEv ccev = (CallCtlConnOfferedEv) ev;
			CiscoConnection cconn = (CiscoConnection) ccev.getConnection();
			cconn.accept();
			if (log.isDebugEnabled()) {
				log.debug("    -------> Accepted.");
			}
			break;
		case TermConnRingingEv.ID:
			TermConnRingingEv tcev = (TermConnRingingEv) ev;
			if (log.isDebugEnabled()) {
				String tname = tcev.getTerminalConnection().getTerminal().getName();
				log.debug("    -------> Answering(" + tname + ")");
			}
			tcev.getTerminalConnection().answer();
			break;
			
		default:
			super.eventHandler(ev);
			break;
		}
	}
}
