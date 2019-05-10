package org.ftoth.general.util.jtapi;

import javax.telephony.TerminalConnection;

/**
 * <p>Description: </p>
 *
 * @author ftoth
 */
public class TerminalConnectionUtil
{
    public static String getStateName(int state)
    {
        switch(state) {
        case TerminalConnection.ACTIVE:
            return "ACTIVE";
        case TerminalConnection.DROPPED:
            return "DROPPED";
        case TerminalConnection.IDLE:
            return "IDLE";
        case TerminalConnection.PASSIVE:
            return "PASSIVE";
        case TerminalConnection.RINGING:
            return "RINGING";
        case TerminalConnection.UNKNOWN:
            return "UNKNOWN";
        }
        return "?";
    }

}
