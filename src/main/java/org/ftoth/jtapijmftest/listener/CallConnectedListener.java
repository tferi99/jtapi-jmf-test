package org.ftoth.jtapijmftest.listener;

import javax.telephony.events.ConnConnectedEv;

public interface CallConnectedListener
{
	public abstract void onCallConnected(ConnConnectedEv ev);
}
