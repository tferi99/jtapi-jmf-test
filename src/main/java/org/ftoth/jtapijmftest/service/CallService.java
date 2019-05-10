package org.ftoth.jtapijmftest.service;

import javax.telephony.Call;

import org.ftoth.jtapijmftest.listener.CallConnectedListener;
import org.ftoth.jtapijmftest.listener.CallDisconnectedListener;

public interface CallService
{
	Call call(String srcDn, String destDn, CallConnectedListener callEstablishedListener, CallDisconnectedListener callClosedListener);
}
