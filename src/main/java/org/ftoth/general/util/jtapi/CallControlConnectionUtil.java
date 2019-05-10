package org.ftoth.general.util.jtapi;

import javax.telephony.callcontrol.CallControlConnection;

public class CallControlConnectionUtil
{
    public static String getStateName(int state)
    {
        switch(state) {
        case CallControlConnection.ALERTING:
        	return "ALERTING";
        case CallControlConnection.DIALING:
        	return "DIALING";
        case CallControlConnection.DISCONNECTED:
        	return "DISCONNECTED";
        case CallControlConnection.ESTABLISHED:
        	return "ESTABLISHED";
        case CallControlConnection.FAILED:
        	return "FAILED";
        case CallControlConnection.IDLE:
        	return "IDLE";
        case CallControlConnection.INITIATED:
        	return "INITIATED";
        case CallControlConnection.NETWORK_ALERTING:
        	return "NETWORK_ALERTING";
        case CallControlConnection.NETWORK_REACHED:
        	return "NETWORK_REACHED";
        case CallControlConnection.OFFERED:
        	return "OFFERED";
/*        case CallControlConnection.OFFERING:
        	return "OFFERING";*/
        case CallControlConnection.QUEUED:
        	return "QUEUED";
        case CallControlConnection.UNKNOWN:
        	return "UNKNOWN";
        case CallControlConnection.CONNECTED:
        	return "CONNECTED";
        case CallControlConnection.INPROGRESS:
        	return "INPROGRESS";
        }
        return "?";
    }
}
