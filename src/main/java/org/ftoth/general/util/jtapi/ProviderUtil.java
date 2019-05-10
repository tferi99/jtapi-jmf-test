package org.ftoth.general.util.jtapi;

import javax.telephony.Provider;

/**
 * <p>Description: </p>
 *
 * @author ftoth
 */
public class ProviderUtil
{
    public static String getStateName(int state)
    {
        switch(state) {
            case Provider.IN_SERVICE:
                return "IN_SERVICE";
            case Provider.OUT_OF_SERVICE:
                return "OUT_OF_SERVICE";
            case Provider.SHUTDOWN:
                return "SHUTDOWN";
        }
        return "?";
    }
}