package org.ftoth.general.util.jtapi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.telephony.Address;
import javax.telephony.Call;
import javax.telephony.Connection;
import javax.telephony.Provider;
import javax.telephony.ResourceUnavailableException;
import javax.telephony.Terminal;
import javax.telephony.TerminalConnection;

import com.cisco.jtapi.extensions.CiscoMediaTerminal;
import com.cisco.jtapi.extensions.CiscoProvider;

/**
 * <p>Description: </p>
 *
 * @author ftoth
 */
public class JtapiInfo
{
	private static final int ITEM_PER_ROW = 4;
	
    private static JtapiInfo singleton;

    //----------------------- properties -----------------------
    private boolean providerObservable;
    private boolean addressObservable;
    private boolean callObservable;
    private boolean connectionCanDisconnect;

    private List<Address> addresses;
    private List<Call> calls;
    private Map<Call, List<Connection>> connections;
    private List<Terminal> terminals;
    private Map<Terminal, List<TerminalConnection>> terminalConnections;

    /**
     * To fetch and refresh JTAPI information.
     *
     */
    public synchronized void refresh(Provider provider) throws ResourceUnavailableException
    {
        /*ProviderCapabilit	ies pCaps = provider.getCapabilities();
        AddressCapabilities aCaps = provider.getAddressCapabilities();
        CallCapabilities cCaps = provider.getCallCapabilities();
        ConnectionCapabilities connCaps = provider.getConnectionCapabilities();*/

        // flags
        providerObservable = provider.getCapabilities().isObservable();
        addressObservable = provider.getAddressCapabilities().isObservable();
        callObservable = provider.getCallCapabilities().isObservable();
        connectionCanDisconnect = provider.getConnectionCapabilities().canDisconnect();

        // addresses
        addresses = storeIntoList(provider.getAddresses());

        // calls
        calls = storeIntoList(provider.getCalls());

        // connections
        connections = new HashMap<Call, List<Connection>>();
        for (Call c : calls) {
            //List<Connection> conns = new ArrayList<Connection>();
            connections.put(c, storeIntoList(c.getConnections()));
        }

        // terminals
        terminals = storeIntoList(provider.getTerminals());

        // terminalConnections
        terminalConnections = new HashMap<Terminal, List<TerminalConnection>>();
        for (Terminal t : terminals) {
            //List<TerminalConnection> tcs = new ArrayList<TerminalConnection>();
            terminalConnections.put(t, storeIntoList(t.getTerminalConnections()));
        }
    }

    public static String dumpProvider(Provider provider) throws ResourceUnavailableException
    {
        JtapiInfo inf = getInstance();
        inf.refresh(provider);

        boolean first = false;

        StringBuffer b = new StringBuffer();
        b.append("Provider[" + provider.getName() + "] - " + ProviderUtil.getStateName(provider.getState()) + "\n");
        b.append("------------------------------------------------------------------\n");
        b.append("Provider observable: " + inf.providerObservable + "\n");
        b.append("Address observable: " + inf.addressObservable + "\n");
        b.append("Call observable: " + inf.callObservable + "\n");
        b.append("Connection can disconnect: " + inf.connectionCanDisconnect + "\n");
        b.append("\n");

        b.append("Addresses:\n");
        first = true;
        if (inf.addresses != null) {
            for (int n=0; n<inf.addresses.size(); n++) {
            	Address addr = inf.addresses.get(n);
            	
                if (!first) {
                    b.append(", ");
                }
            	if (n % ITEM_PER_ROW == 0) {
            		if (!first) {
            			b.append("\n");
            		}
            		b.append("    ");
            	}
            	
                b.append(addr.getName() + "(");
                
                // terminals
                dumpTerms(b, addr.getTerminals());
                b.append(")");
                
                // observers
                int addrObsCount = (addr.getObservers() == null) ? 0 : addr.getObservers().length; 
                int callObsCount = (addr.getCallObservers() == null) ? 0 : addr.getCallObservers().length;
                b.append("(addrObs:" + addrObsCount + ", callObs:" + callObsCount + ")");
                
                
                first = false;
            }
        }
        b.append("\n\n");

        b.append("Calls:\n");
        first = true;
        if (inf.calls != null) {
            for (Call call : inf.calls) {
                if (!first) {
                    b.append(", ");
                }
                b.append(CallUtil.getStateName(call.getState()));
                first = false;
            }
        }
        b.append("\n\n");

        b.append("Connections:\n");
        int idx = 0;
        for (Call call : inf.calls) {
            b.append("Call " + idx + ": ");
            List<Connection> conns = inf.connections.get(call);
            if (conns != null) {
                first = true;
                for (int n=0; n<conns.size(); n++) {
                	Connection conn = conns.get(n);
                    if (!first) {
                        b.append(", ");
                    }
                	if (n % ITEM_PER_ROW == 0) {
                		if (!first) {
                			b.append("\n");
                		}
                		b.append("    ");
                	}
                    b.append(conn.getAddress().getName() + "(" + ConnectionUtil.getStateName(conn.getState()) + ")");
                    first = false;
                }
            }
            idx++;
            b.append("\n");
        }
        b.append("\n");


        b.append("Terminals:\n");
        Terminal[] terms = provider.getTerminals();
        first = true;
        dumpTerms(b, terms, true);
        b.append("\n\n");

        b.append("Media Terminals:\n");
        CiscoMediaTerminal[] mediaTerms = ((CiscoProvider)provider).getMediaTerminals();
        first = true;
        dumpTerms(b, mediaTerms, true);
        b.append("\n\n");
        
        b.append("Terminal connections:\n");
        for (Terminal t : inf.terminals) {
            List<TerminalConnection> tcs = inf.terminalConnections.get(t);
            if (tcs.size() > 0) {
                b.append("[" + t.getName() + "]: ");
                first = true;
                if (tcs != null) {
                    for (TerminalConnection tc : tcs) {
                        if (!first) {
                            b.append(", ");
                        }
                        first = false;
                        b.append(TerminalConnectionUtil.getStateName(tc.getState()));
                    }
                }
                b.append("\n");
            }
        }
        b.append("\n\n");
        b.append("------------------------------------------------------------------\n");

        return b.toString();
    }

    //----------------------- helpers -----------------------
    private static JtapiInfo getInstance()
    {
        if (singleton == null) {
            singleton = new JtapiInfo();
        }
        return singleton;
    }


    private static void dumpTerms(StringBuffer b, Terminal[] terms)
    {
    	dumpTerms(b, terms, false);
    }
   
    private static void dumpTerms(StringBuffer b, Terminal[] terms, boolean indent)
    {
        boolean first = true;
        if (terms != null) {
            for (int n=0; n<terms.length; n++) {
            	Terminal term = terms[n];
            	
                if (!first) {
                    b.append(", ");
                }
                
            	if (indent && n % ITEM_PER_ROW == 0) {
            		if (!first) {
            			b.append("\n");
            		}
            		b.append("    ");
            	}
                b.append(term.getName());
                first = false;
            }
        }
    }

    private <T> List<T> storeIntoList(T[] items)
    {
        List<T> list = new ArrayList<T>();
        if (items == null) {
            return list;
        }

        for (T i : items) {
            list.add(i);
        }
        return list;
    }
}
