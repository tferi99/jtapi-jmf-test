package org.ftoth.general.util.jtapi;

import javax.telephony.events.Ev;

/**
 * <p>Description: </p>
 *
 * @author ftoth
 */
public class EvUtil
{
	protected static final String EVENT_PREFIX = "EVENT - ";
	protected static final String INHERITED_EVENT_PREFIX = "     - ";

	public static String dump(Ev ev)
	{
		return dump(ev, "", true);
	}
	
	public static String dump(Ev ev, String label, boolean withNewline)
	{
		String nl = withNewline ? "\n" : "";
		return EVENT_PREFIX + label + "[" + ev.getID() + "] (" + getCauseName(ev.getCause()) + ")(" + ev + ")" + nl; 
	}
	
	
    public static String getCauseName(int cause)
    {
        switch (cause) {
        case Ev.CAUSE_NORMAL:
            return("CAUSE_NORMAL");
        case Ev.CAUSE_UNKNOWN:
            return("CAUSE_UNKNOWN");
        case Ev.CAUSE_CALL_CANCELLED:
            return("CAUSE_CALL_CANCELLED");
        case Ev.CAUSE_DEST_NOT_OBTAINABLE:
            return("CAUSE_DEST_NOT_OBTAINABLE");
        case Ev.CAUSE_INCOMPATIBLE_DESTINATION:
            return("CAUSE_INCOMPATIBLE_DESTINATION");
        case Ev.CAUSE_LOCKOUT:
            return("CAUSE_LOCKOUT");
        case Ev.CAUSE_NEW_CALL:
            return("CAUSE_NEW_CALL");
        case Ev.CAUSE_RESOURCES_NOT_AVAILABLE:
            return("CAUSE_RESOURCES_NOT_AVAILABLE");
        case Ev.CAUSE_NETWORK_CONGESTION:
            return("CAUSE_NETWORK_CONGESTION");
        case Ev.CAUSE_NETWORK_NOT_OBTAINABLE:
            return("CAUSE_NETWORK_NOT_OBTAINABLE");
        case Ev.CAUSE_SNAPSHOT:
            return("CAUSE_SNAPSHOT");
        case Ev.META_CALL_STARTING:
            return("META_CALL_STARTING");
        case Ev.META_CALL_PROGRESS:
            return("META_CALL_PROGRESS");
        case Ev.META_CALL_ADDITIONAL_PARTY:
            return("META_CALL_ADDITIONAL_PARTY");
        case Ev.META_CALL_REMOVING_PARTY:
            return("META_CALL_REMOVING_PARTY");
        case Ev.META_CALL_ENDING:
            return("META_CALL_ENDING");
        case Ev.META_CALL_MERGING:
            return("META_CALL_MERGING");
        case Ev.META_CALL_TRANSFERRING:
            return("META_CALL_TRANSFERRING");
        case Ev.META_SNAPSHOT:
            return("META_SNAPSHOT");
        case Ev.META_UNKNOWN:
            return "META_UNKNOWN";
        }
        return "?";
    }
}
