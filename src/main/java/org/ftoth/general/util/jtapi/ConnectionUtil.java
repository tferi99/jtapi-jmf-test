package org.ftoth.general.util.jtapi;

import javax.telephony.Connection;

/**
 * <p>Description: </p>
 *
 * @author ftoth
 */
public class ConnectionUtil
{
    public static String getStateName(int state)
    {
        switch(state) {
            case Connection.ALERTING:
                return "ALERTING";
            case Connection.CONNECTED:
                return "CONNECTED";
            case Connection.DISCONNECTED:
                return "DISCONNECTED";
            case Connection.FAILED:
                return "FAILED";
            case Connection.IDLE:
                return "IDLE";
            case Connection.INPROGRESS:
                return "INPROGRESS";
            case Connection.UNKNOWN:
                return "UNKNOWN";
        }
        return "?";
    }
}
