package org.ftoth.general.util.jtapi;

import javax.telephony.Address;

public class AddressUtil
{
	public static String dumpObservers(Address addr)
	{
		StringBuilder b = new StringBuilder();
		b.append("---------- CallObservers -----------\n");
		//CallObserver[] cobs = addr.getCallObservers();
/*		for (CallObserver cob : cobs) {
			b.append("    " + cob.)
		}*/
		
		return b.toString();
	}
	
	public static String dump(Address addr)
	{
		StringBuilder b = new StringBuilder();
		b.append("Address(" + addr.getName() + ")\n");
		b.append("    AddressObservers: " + addr.getObservers().length);
		b.append("    CallObservers: " + addr.getCallObservers().length);
		//addr.getTerminals()
		
		
		return b.toString();
		
	}
}
