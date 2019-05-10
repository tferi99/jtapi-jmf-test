package org.ftoth.jtapijmftest.service.impl;

import java.util.Hashtable;
import java.util.Map;

import javax.telephony.Address;
import javax.telephony.Call;
import javax.telephony.CallObserver;
import javax.telephony.Connection;
import javax.telephony.Provider;
import javax.telephony.events.CallEv;
import javax.telephony.events.ConnConnectedEv;
import javax.telephony.events.ConnDisconnectedEv;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ftoth.jtapijmftest.exception.JtapiRuntimeException;
import org.ftoth.jtapijmftest.listener.CallConnectedListener;
import org.ftoth.jtapijmftest.listener.CallDisconnectedListener;
import org.ftoth.jtapijmftest.listener.CallObserverListener;
import org.ftoth.jtapijmftest.service.CallService;
import org.ftoth.jtapijmftest.service.ProviderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component("callService")
public class CallServiceImpl implements CallService
{
	private static final Log log = LogFactory.getLog(CallServiceImpl.class);

	@Autowired
	ProviderService providerService;
	
	// TODO only for development, this implementations can contain only 1 entry for 1 target DN
	private Map<String, CallConnectedListener> callConnectedListeners = new Hashtable<String, CallConnectedListener>();
	private Map<String, CallDisconnectedListener> callDisconnectedListeners = new Hashtable<String, CallDisconnectedListener>();
	
	// ------------------------- properties -------------------------
	private CallObserver callObserver;
	
	
	
	public CallObserver getCallObserver()
	{
		if (callObserver == null) {
			callObserver = createCallObserver();
		}
		return callObserver;
	}



	// ------------------------- implements -------------------------
	/* 
	 * It performs a call.
	 * 
	 *  - it adds CallObserver to source Address
	 *  - registers callbacks
	 *  - creates Call
	 *  - connects source address terminal with source address to destination address
	 * 
	 * (non-Javadoc)
	 * @see org.ftoth.jtapijmftest.service.CallService#call(java.lang.String, java.lang.String, org.ftoth.jtapijmftest.listener.CallConnectedListener, org.ftoth.jtapijmftest.listener.CallDisconnectedListener)
	 */
	@Override
	public Call call(String srcDn, String destDn, CallConnectedListener callEstablishedListener, CallDisconnectedListener callClosedListener)
	{
		if (log.isInfoEnabled()) {
			log.info("Creating call(" + srcDn + " -> " + destDn + ")"); 
		}
		
		Provider provider = providerService.getProvider();
		
		try {
			// adding CallObserver to source Address
			Address srcAddr = provider.getAddress(srcDn);
			srcAddr.addCallObserver(getCallObserver());
			
			// registering callbacks
			if (callEstablishedListener != null) {
				callConnectedListeners.put(destDn, callEstablishedListener);
			}
			if (callClosedListener != null) {
				callDisconnectedListeners.put(destDn, callClosedListener);
			}
			
			// making call
			Call call = provider.createCall();
			call.connect(srcAddr.getTerminals()[0], srcAddr, destDn);
			//Address a = provider.getAddress("9010");
			//call.connect(srcAddr.getTerminals()[0], a, destDn);
			
			if (log.isDebugEnabled()) {
				log.debug("Call(" + srcDn + " -> " + destDn + ") has been created");
			} 
			
			return call;
		}
		catch (Exception e) {
			throw new JtapiRuntimeException("Error during call(" + srcDn + " -> " + destDn + ")", e);
		}
		
	}

	// ------------------------- helpers -------------------------
	private CallObserver createCallObserver()
	{
		CallObserverListener co = new CallObserverListener("test")
		{
			
			@Override
			protected void onDisconnected(ConnDisconnectedEv ev)
			{
				Connection conn = ev.getConnection();
				String targetDn = conn.getAddress().getName();
				
				// callback
				CallDisconnectedListener disconnCb = getCallDisconnectedListenerByTarget(targetDn);
				if (disconnCb != null) {
					disconnCb.onCallDisconnected(ev);
				}
			}
			
			@Override
			protected void onConnected(ConnConnectedEv ev)
			{
				Connection conn = ev.getConnection();
				String targetDn = conn.getAddress().getName();
				
				// callback
				CallConnectedListener connCb = getCallConnectedListenerByTarget(targetDn);
				if (connCb != null) {
					connCb.onCallConnected(ev);
				}						
			}
			
			@Override
			protected void onCallChanged(CallEv ev)
			{
			}
		};
		
/*		CallObserver co2 = new CallObserver() {
			public void callChangedEvent (CallEv[] eventList) {
				for (CallEv ev : eventList) {
					Call call = ev.getCall();
					if (log.isTraceEnabled()) {
						log.trace("CallObserver EVENT [" +  ev.getID() + "] - " + ev + ", Call(" + CallUtil.getStateName(call.getState()) + ")");
					}
					
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
						Connection conn = cev.getConnection();
						String targetDn = conn.getAddress().getName();
						if (log.isDebugEnabled()) {
							log.debug("    Connection(dn:" + targetDn + ", state:" + ConnectionUtil.getStateName(conn.getState()) + ")");
						}
						
						// callback
						CallConnectedListener connCb = getCallConnectedListenerByTarget(targetDn);
						if (connCb != null) {
							connCb.onCallConnected(cev);
						}						
					}
					// ConnDisconnectedEv
					else if (ev instanceof ConnDisconnectedEv) {
						ConnDisconnectedEv cev = (ConnDisconnectedEv) ev;
						Connection conn = cev.getConnection();
						String targetDn = conn.getAddress().getName();
						if (log.isDebugEnabled()) {
							log.debug("    Connection(dn:" + targetDn + ", state:" + ConnectionUtil.getStateName(conn.getState()) + ")");
						}
						
						// callback
						CallDisconnectedListener disconnCb = getCallDisconnectedListenerByTarget(targetDn);
						if (disconnCb != null) {
							disconnCb.onCallDisconnected(cev);
						}
					}
				}
			}
		};*/
		return co;
	}

	private CallConnectedListener getCallConnectedListenerByTarget(String targetDn)
	{
		return callConnectedListeners.get(targetDn);
	}
	
	private CallDisconnectedListener getCallDisconnectedListenerByTarget(String targetDn)
	{
		return callDisconnectedListeners.get(targetDn);
	}		
	
}
