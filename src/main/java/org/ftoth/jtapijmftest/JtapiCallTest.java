package org.ftoth.jtapijmftest;

import javax.telephony.events.ConnConnectedEv;
import javax.telephony.events.ConnDisconnectedEv;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ftoth.jtapijmftest.listener.CallConnectedListener;
import org.ftoth.jtapijmftest.listener.CallDisconnectedListener;
import org.ftoth.jtapijmftest.service.CallService;

public class JtapiCallTest extends JtapiAppBase
{
	private static final Log log = LogFactory.getLog(JtapiCallTest.class);

	private CallService callService;


	public static void main(String[] args)
	{
		JtapiCallTest app = new JtapiCallTest();
		app.start();
	}

	@Override
	protected void init() throws Exception
	{
		super.init();
		
		callService = (CallService) getBean("callService");
	}

	@Override
	protected void action()
			throws Exception
	{
		callService.call("9000", "9001", callEstablishedListener, callClosedListener);
		
		// callService.call("9000", "9010");
		
		waitOnEndOfAction();
	}


	// ------------------------- callbacks -------------------------
	CallConnectedListener callEstablishedListener = new CallConnectedListener()
	{
		@Override
		public void onCallConnected(ConnConnectedEv ev)
		{
			if (log.isDebugEnabled()) {
				log.debug(">>>>>>>> CALL ESTABLISHED to " + ev.getConnection().getAddress().getName());
			}
		}
	};

	CallDisconnectedListener callClosedListener = new CallDisconnectedListener()
	{
		@Override
		public void onCallDisconnected(ConnDisconnectedEv ev)
		{
			if (log.isDebugEnabled()) {
				log.debug("<<<<<<<<< CALL CLOSED from " + ev.getConnection().getAddress().getName());
			}

			fireEndOfAction();
		}
	};
}
