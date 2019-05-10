package org.ftoth.jtapijmftest.listener;

import javax.telephony.Call;
import javax.telephony.CallObserver;
import javax.telephony.Connection;
import javax.telephony.events.CallEv;
import javax.telephony.events.ConnConnectedEv;
import javax.telephony.events.ConnCreatedEv;
import javax.telephony.events.ConnDisconnectedEv;
import javax.telephony.events.ConnFailedEv;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ftoth.general.util.jtapi.CallUtil;
import org.ftoth.general.util.jtapi.ConnectionUtil;
import org.ftoth.jtapijmftest.exception.JtapiRuntimeException;

public abstract class CallObserverListener implements CallObserver
{
	private static final Log log = LogFactory.getLog(CallObserverListener.class);

	private String listenerId;
	
	// ------------------------- startup -------------------------
	public CallObserverListener(String listenerId)
	{
		this.listenerId = listenerId;
	}
	
	// ------------------------- CallObserver -------------------------
	/* 
	 * Call observer main event handler. 
	 * 
	 * It writes an event log and calls event handler to generate specific callbacks.
	 *  
	 * (non-Javadoc)
	 * @see javax.telephony.CallObserver#callChangedEvent(javax.telephony.events.CallEv[])
	 */
	@Override
	public void callChangedEvent(CallEv[] events)
	{
		for (CallEv ev : events) {
			try {
				writeEventLog(ev);		// writing log
				eventHandler(ev);		// generate callbacks
			}
			catch (Exception e) {
				e.printStackTrace();
				throw new JtapiRuntimeException(e);
			}
		}
	}
	
	// callbacks
	protected abstract void onCallChanged(CallEv ev);
	protected abstract void onConnected(ConnConnectedEv ev);
	protected abstract void onDisconnected(ConnDisconnectedEv ev);
	
	// ------------------------- helpers -------------------------
	protected void writeEventLog(CallEv ev) throws Exception
	{
		if (log.isDebugEnabled()) {
			Call call = ev.getCall();
			log.debug(getEventLogMessage("CallObserver", ev, call));
		}
	}
	
	protected void eventHandler(CallEv ev)  throws Exception
	{
		onCallChanged(ev);
		
		// ConnCreatedEv
		if (ev instanceof ConnCreatedEv) {
			ConnCreatedEv cev = (ConnCreatedEv) ev;
			Connection conn = cev.getConnection();
			if (log.isDebugEnabled()) {
				log.debug("    Connection(dn:" + conn.getAddress().getName() + ", state:" + ConnectionUtil.getStateName(conn.getState()) + ")");
			}
		}
		// ConnConnectedEv
		else if (ev instanceof ConnConnectedEv) {
			ConnConnectedEv cev = (ConnConnectedEv) ev;
			
			if (log.isDebugEnabled()) {
				Connection conn = cev.getConnection();
				String targetDn = conn.getAddress().getName();
				log.debug("    Connection(dn:" + targetDn + ", state:" + ConnectionUtil.getStateName(conn.getState()) + ")");
			}
			
			onConnected(cev);
		}
		else if (ev instanceof ConnFailedEv) {
			ConnFailedEv cev = (ConnFailedEv) ev;
		}
		// ConnDisconnectedEv
		else if (ev instanceof ConnDisconnectedEv) {
			ConnDisconnectedEv cev = (ConnDisconnectedEv) ev;
			if (log.isDebugEnabled()) {
				Connection conn = cev.getConnection();
				String targetDn = conn.getAddress().getName();
				log.debug("    Connection(dn:" + targetDn + ", state:" + ConnectionUtil.getStateName(conn.getState()) + ")");
			}
			onDisconnected(cev);
		}
	}
	
	protected String getEventLogMessage(String observerName, CallEv ev, Call call)
	{
		String callState = (call == null) ? "?" : CallUtil.getStateName(call.getState()); 
		return observerName + " EVENT on listener[" + listenerId +" - CallEv[" +  ev.getID() + "] - " + ev + ", Call(" + callState + ")";		
	}
}
