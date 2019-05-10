package org.ftoth.general.util.jtapi;

import com.cisco.jtapi.extensions.CiscoRTPPayload;

public class CiscoRTPPayloadUtil
{
    public static String getTypeName(int type)
    {
        switch(type) {
        case CiscoRTPPayload.ACTIVEVOICE:
        	return "ACTIVEVOICE";
        case CiscoRTPPayload.ACY_G729AASSN:
        	return "ACY_G729AASSN";
        case CiscoRTPPayload.DATA56:
        	return "DATA56";
        case CiscoRTPPayload.DATA64:
        	return "DATA64";
        case CiscoRTPPayload.G711ALAW56K:
        	return "G711ALAW56K";
        case CiscoRTPPayload.G711ALAW64K:
        	return "G711ALAW64K";
        case CiscoRTPPayload.G711ULAW56K:
        	return "G711ULAW56K";
        case CiscoRTPPayload.G711ULAW64K:
        	return "G711ULAW64K";
        case CiscoRTPPayload.G722_48K:
        	return "G722_48K";
        case CiscoRTPPayload.G722_56K:
        	return "G722_56K";
        case CiscoRTPPayload.G722_64K:
        	return "G722_64K";
        case CiscoRTPPayload.G7231:
        	return "G7231";
        case CiscoRTPPayload.G728:
        	return "G728";
        case CiscoRTPPayload.G729:
        	return "G729";
        case CiscoRTPPayload.G729ANNEXA:
        	return "G729ANNEXA";
        case CiscoRTPPayload.GSM:
        	return "GSM";
        case CiscoRTPPayload.IS11172AUDIOCAP:
        	return "IS11172AUDIOCAP";
        case CiscoRTPPayload.IS13818AUDIOCAP:
        	return "IS13818AUDIOCAP";
        case CiscoRTPPayload.NONSTANDARD:
        	return "NONSTANDARD";
        case CiscoRTPPayload.WIDEBAND_256K:
        	return "WIDEBAND_256K";
        }
        return "?";
    }
}
