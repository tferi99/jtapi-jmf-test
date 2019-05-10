package org.ftoth.general.util.jtapi;

import com.cisco.jtapi.extensions.CiscoRTPBitRate;

public class CiscoRTPBitRateUtil
{
	public static String getName(int value)
	{
		
		switch(value) {
		case CiscoRTPBitRate.R5_3:
			return "R5_3";
		case CiscoRTPBitRate.R6_4:
			return "R6_4";
		}
		return "?";
	}
}
