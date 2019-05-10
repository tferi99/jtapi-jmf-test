package org.ftoth.general.util.jtapi;

import javax.telephony.Call;

/**
 * <p>Description: </p>
 *
 * @author ftoth
 */
public class CallUtil
{
    public static String getStateName(int state)
    {
        switch(state) {
        case Call.ACTIVE:
            return "ACTIVE";
        case Call.IDLE:
            return "IDLE";
        case Call.INVALID:
            return "INVALID";
        }
        return "?";
    }
}
