package org.ftoth.general.util.jtapi;

import javax.telephony.Call;
import javax.telephony.events.CallEv;

public class CallEvUtil extends EvUtil
{
	public static String dump(CallEv ev)
	{
		Call call = ev.getCall();
		
		return EvUtil.dump(ev, "CallEv", false) + " - Call state:" + CallUtil.getStateName(call.getState())  + "|" + call.toString() ;
	}
}
