package org.ftoth.jtapijmftest.listener;

import javax.telephony.events.ConnDisconnectedEv;

public abstract class CallDisconnectedListener
{
	public abstract void onCallDisconnected(ConnDisconnectedEv ev);
}
