package org.ftoth.general.util.jtapi;

import javax.telephony.Address;
import javax.telephony.InvalidArgumentException;
import javax.telephony.Provider;
import javax.telephony.Terminal;

public class TerminalUtil
{
	public static Terminal getByName(Provider provider, String name) throws InvalidArgumentException
	{
		return provider.getTerminal(name);
	}

	public static Terminal getTerminalOfDN(Provider provider, String dn) throws InvalidArgumentException
	{
		return getTerminalOfDN(provider, dn, 0);
		
	}
	public static Terminal getTerminalOfDN(Provider provider, String dn, int terminalIndex) throws InvalidArgumentException
	{
		Address addr = provider.getAddress(dn);
		Terminal[] terms = addr.getTerminals();
		if (terms == null || terms.length <= terminalIndex) {
			if (terms == null || terminalIndex == 0) {
				throw new InvalidArgumentException("Terminal not found for DN(" + dn + ")");
			}
			throw new InvalidArgumentException("Terminal[" + terminalIndex +"] not found for DN(" + dn + ") - there is only " + terms.length + " terminal(s)");
		}
		
		return terms[terminalIndex];
	}
}
