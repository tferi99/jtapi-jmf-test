package org.ftoth.general.util.onesec.ivr.impl;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ftoth.general.util.onesec.ivr.BufferCache;
import org.ftoth.general.util.onesec.ivr.CodecManager;
import org.ftoth.general.util.onesec.ivr.IncomingRtpStream;
import org.ftoth.general.util.onesec.ivr.OutgoingRtpStream;
import org.ftoth.general.util.onesec.ivr.RTPManagerService;
import org.ftoth.general.util.onesec.ivr.RtpAddress;
import org.ftoth.general.util.onesec.ivr.RtpStream;
import org.ftoth.general.util.onesec.ivr.RtpStreamManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("rtpStreamManager")
public class RtpStreamManagerImpl implements RtpStreamManager
{
	private static final Log log = LogFactory.getLog(RtpStreamManagerImpl.class);
	
	private Integer maxStreamCount = 20;

    private Map<InetAddress, NavigableMap<Integer, RtpStream>> streams;
    private Map<String, RtpAddress> reservedAddresses;
    private Map<InetAddress, Set<Integer>> busyPorts;
    private ReentrantReadWriteLock streamsLock;

    private AtomicLong sendedBytes;
    private AtomicLong recievedBytes;
    private AtomicLong sendedPackets;
    private AtomicLong recievedPackets;
    private AtomicLong rejectedStreamCreations;
    private AtomicLong streamCreations;

    private static String busyPortsMessage;
    private static String statMessage;
    private static String sendedBytesMessage;
    private static String sendedPacketsMessage;
    private static String recievedBytesMessage;
    private static String recievedPacketsMessage;
    private static String createdStreamsMessage;
    private static String rejectedStreamsMessage;
    private static String incomingStreamMessage ;
    private static String outgoingStreamMessage ;
    private static String localAddressMessage;
    private static String localPortMessage;
    private static String remoteAddressMessage;
    private static String remotePortMessage;
    private static String creationTimeMessage;
    private static String durationMessage;
    private static String managerBusyMessage;

    @Autowired
    RTPManagerService rtpManagerService;

    @Autowired
    BufferCache bufferCache;
    
    @Autowired
    CodecManager codecManager;
    
    public CodecManager getCodecManager()
	{
		return codecManager;
	}

	public BufferCache getBufferCache()
	{
		return bufferCache;
	}

	public static final String DUMMY_OWNER_NAME = "dummy_owner";
    
    public RtpStreamManagerImpl()
    {
    	initFields();	
    }
    
    protected void initFields()
    {
        streams = new HashMap<InetAddress, NavigableMap<Integer, RtpStream>>();
        busyPorts =  new HashMap<InetAddress, Set<Integer>>();
        reservedAddresses = new HashMap<String, RtpAddress>();
        streamsLock = new ReentrantReadWriteLock();
        sendedBytes = new AtomicLong();
        recievedBytes = new AtomicLong();
        recievedPackets = new AtomicLong();
        sendedPackets = new AtomicLong();
        rejectedStreamCreations = new AtomicLong();
        streamCreations = new AtomicLong();
    }

    protected void doStart() throws Exception
    {
        sendedBytes.set(0);
        recievedBytes.set(0);
        sendedPackets.set(0);
        recievedPackets.set(0);
        rejectedStreamCreations.set(0);
        streamCreations.set(0);
    }

    protected void doStop() throws Exception
    {
        releaseStreams(streams);
        streams.clear();
        busyPorts.clear();
    }

/*    public Boolean getAutoRefresh() {
        return true;
    }

    public Map<String, NodeAttribute> getRefreshAttributes() throws Exception {
        return null;
    }

    public List<ViewableObject> getViewableObjects(Map<String, NodeAttribute> refreshAttributes) 
            throws Exception
    {
        if (!Status.STARTED.equals(getStatus()))
            return null;
        
        List<ViewableObject> vos = new ArrayList<ViewableObject>();
        if (streamsLock.readLock().tryLock(500, TimeUnit.MILLISECONDS)){
            try {
                TableImpl statTable = new TableImpl(new String[]{
                    createdStreamsMessage, rejectedStreamsMessage, sendedBytesMessage, sendedPacketsMessage,
                    recievedBytesMessage, recievedPacketsMessage});
                statTable.addRow(new Object[]{streamCreations, rejectedStreamCreations,
                    sendedBytes, sendedPackets, recievedBytes, recievedPackets});
                
                TableImpl busyPortsTable = new TableImpl(new String[]{localAddressMessage, localPortMessage});
                for (Entry<InetAddress, Set<Integer>> addr: busyPorts.entrySet())
                    for (Integer port: addr.getValue())
                        busyPortsTable.addRow(new Object[]{addr.getKey().getHostAddress(), port});

                String[] colnames = {
                    localAddressMessage, localPortMessage, remoteAddressMessage, remotePortMessage,
                    creationTimeMessage, durationMessage};
                TableImpl inStreams = new TableImpl(colnames);
                TableImpl outStreams = new TableImpl(colnames);
                SimpleDateFormat fmt = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
                for (Map.Entry<InetAddress, NavigableMap<Integer, RtpStream>> addr: streams.entrySet()){
                    NavigableMap<Integer, RtpStream> ports = addr.getValue();
                    if (ports!=null)
                        for (RtpStream stream: ports.values())
                            if (stream instanceof OutgoingRtpStream)
                                outStreams.addRow(createRowFromStream(stream, fmt));
                            else
                                inStreams.addRow(createRowFromStream(stream, fmt));
                }
                vos = new ArrayList<ViewableObject>();
                vos.add(new ViewableObjectImpl(Viewable.RAVEN_TEXT_MIMETYPE, statMessage));
                vos.add(new ViewableObjectImpl(Viewable.RAVEN_TABLE_MIMETYPE, statTable));
                vos.add(new ViewableObjectImpl(Viewable.RAVEN_TEXT_MIMETYPE, busyPortsMessage));
                vos.add(new ViewableObjectImpl(Viewable.RAVEN_TABLE_MIMETYPE, busyPortsTable));
                vos.add(new ViewableObjectImpl(Viewable.RAVEN_TEXT_MIMETYPE, outgoingStreamMessage));
                vos.add(new ViewableObjectImpl(Viewable.RAVEN_TABLE_MIMETYPE, outStreams));
                vos.add(new ViewableObjectImpl(Viewable.RAVEN_TEXT_MIMETYPE, incomingStreamMessage));
                vos.add(new ViewableObjectImpl(Viewable.RAVEN_TABLE_MIMETYPE, inStreams));
            } finally {
                streamsLock.readLock().unlock();
            }
        } else
            vos.add(new ViewableObjectImpl(Viewable.RAVEN_TEXT_MIMETYPE, managerBusyMessage));
        
        return vos;
    }*/

    private Object[] createRowFromStream(RtpStream stream, SimpleDateFormat fmt)
    {
        return new Object[]{
            stream.getAddress().toString(), stream.getPort(),
            stream.getRemoteHost(), stream.getRemotePort(),
            fmt.format(new Date(stream.getCreationTime())),
            (System.currentTimeMillis()-stream.getCreationTime())/1000};
    }

    public Integer getMaxStreamCount()
    {
        return maxStreamCount;
    }

    public void setMaxStreamCount(Integer maxStreamCount)
    {
        this.maxStreamCount = maxStreamCount;
    }

    public IncomingRtpStream getIncomingRtpStream(String owner)
    {
        return (IncomingRtpStream)createStreamOrReserveAddress(true, owner, false);
    }

    public OutgoingRtpStream getOutgoingRtpStream(String owner)
    {
        return (OutgoingRtpStream)createStreamOrReserveAddress(false, owner, false);
    }

    public RtpAddress reserveAddress(String node)
    {
        return createStreamOrReserveAddress(false, node, true);
    }

    public void unreserveAddress(String node)
    {
        streamsLock.writeLock().lock();
        try{
            RtpAddress rtpAddress = reservedAddresses.remove(node);
            if (rtpAddress!=null) {
                if (log.isDebugEnabled()) {
                    log.debug("Unreserving rtp address for node (" + node + ")");
                }
                releaseStream(rtpAddress);
            }
        } finally {
            streamsLock.writeLock().unlock();
        }
    }

    void incHandledBytes(RtpStream stream, long bytes)
    {
        if (stream instanceof OutgoingRtpStream)
            sendedBytes.addAndGet(bytes);
        else
            recievedBytes.addAndGet(bytes);
    }

    void incHandledPackets(RtpStream stream, long packets)
    {
        if (stream instanceof OutgoingRtpStream)
            sendedPackets.addAndGet(packets);
        else
            recievedPackets.addAndGet(packets);
    }

    void releaseStream(RtpAddress stream) {
        streamsLock.writeLock().lock();
        try{
            Map portStreams = streams.get(stream.getAddress());
            if (portStreams != null) {
                portStreams.remove(stream.getPort());
            }
        }
        finally{
            streamsLock.writeLock().unlock();
        }
    }

    Map<InetAddress, NavigableMap<Integer, RtpStream>> getStreams()
    {
        return streams;
    }

    private void releaseStreams(Map<InetAddress, NavigableMap<Integer, RtpStream>> streams)
    {
        streamsLock.writeLock().lock();
        try
        {
            for (Map<Integer, RtpStream> portStreams: streams.values())
            {
                if (portStreams.size()>0)
                {
                    Collection<RtpStream> list = new ArrayList<RtpStream>(portStreams.values());
                    for (RtpStream stream: list)
                        stream.release();
                }
                portStreams.clear();
            }
        }
        finally
        {
            streamsLock.writeLock().unlock();
        }
    }

    private RtpAddress createStreamOrReserveAddress(
            boolean incomingStream, String owner, boolean reserve)
    {
        try
        {
            streamsLock.writeLock().lock();
            try
            {
            	RtpStream stream = null;
                InetAddress address = null;
                int portNumber = 0;
                NavigableMap<Integer, RtpStream> portStreams = null;
                if (reserve && reservedAddresses.containsKey(owner)) {
                    log.warn("The node ({}) is already reserved the address and port ");
                    return reservedAddresses.get(owner);
                }
                if (getStreamsCount() >= maxStreamCount) {
                    throw new Exception("Max streams count exceded");
                }
                
/*                Map<InetAddress, RtpAddressNode> avalAddresses = getAvailableAddresses();
                for (Map.Entry<InetAddress, RtpAddressNode> addr: avalAddresses.entrySet()) {
                    if (!streams.containsKey(addr.getKey())) {
                        address = addr.getKey();
                        portStreams = new TreeMap<Integer, RtpStream>();
//                        portNumber = addr.getValue().getStartingPort();
                        portNumber = getPortNumber(address, addr.getValue().getStartingPort()
                                , addr.getValue().getMaxPortNumber(), portStreams);
                        streams.put(addr.getKey(), portStreams);
                        break;
                    }
                }

                if (portStreams==null) {
                    for (Map.Entry<InetAddress, NavigableMap<Integer, RtpStream>> streamEntry: streams.entrySet())
                        if (portStreams==null || streamEntry.getValue().size()<portStreams.size()) {
                            portStreams = streamEntry.getValue();
                            address = streamEntry.getKey();
                            int startingPort = avalAddresses.get(streamEntry.getKey()).getStartingPort();
                            int maxPortNumber = avalAddresses.get(streamEntry.getKey()).getMaxPortNumber();
                            portNumber = getPortNumber(address, startingPort, maxPortNumber, portStreams);
                        }
				}*/
                
                portStreams = new TreeMap<Integer, RtpStream>();				
				address = InetAddress.getByName("192.168.5.113");
                int startingPort = 40000; 
                int maxPortNumber = 100;  
                portNumber = getPortNumber(address, startingPort, maxPortNumber, portStreams);
				

                if (!reserve) {
                    if (incomingStream) {
                        stream = new IncomingRtpStreamImpl(address, portNumber, rtpManagerService);
                    }
                    else {
                        stream = new OutgoingRtpStreamImpl(address, portNumber, rtpManagerService);
                    }

                    ((AbstractRtpStream)stream).setManager(this);
                    portStreams.put(portNumber, stream);

                    streamCreations.incrementAndGet();

                    return stream;
                }
                else {
                    portStreams.put(portNumber, null);
                    RtpAddress rtpAddress = new RtpAddressImpl(address, portNumber);
                    reservedAddresses.put(owner, rtpAddress);

                    return rtpAddress;
                }
            } finally {
                streamsLock.writeLock().unlock();
            }
        } catch (Exception e) {
            rejectedStreamCreations.incrementAndGet();
                log.error(
                    String.format(
                        "Error creating %s RTP stream. %s"
                        , incomingStream? "incoming" : "outgoing", e.getMessage())
                    , e);
            return null;
        }
    }
    
    private int getPortNumber(InetAddress addr, int startingPort, int maxPortNumber
            , NavigableMap<Integer, RtpStream> portStreams) throws Exception 
    {
        int port = portStreams.isEmpty()? startingPort : portStreams.firstKey()-2;
        while (startingPort<=port)
            if (checkPort(addr, port)) 
                return port;
            else
                port-=2;
        port = portStreams.isEmpty()? startingPort+2 : portStreams.lastKey()+2;
        while (maxPortNumber>=port)
            if (checkPort(addr, port))
                return port;
            else
                port+=2;
        //fullscan
        int fromPort = portStreams.isEmpty()? startingPort : portStreams.firstKey()+2;
        int toPort = portStreams.isEmpty()? maxPortNumber : portStreams.lastKey()-2;
        for (port = fromPort; port<=toPort; port+=2) 
            if (!portStreams.containsKey(port) && checkPort(addr, port))
                return port;
        throw new Exception("No free port");
    }
    
    private boolean checkPort(InetAddress addr, int port) {
        Set<Integer> ports = busyPorts.get(addr);
        if (ports!=null && (ports.contains(port) || ports.contains(port+1)))
                return false;
        for (int p=port; p<=port+1; ++p) {
            DatagramSocket socket = null;
            try {
                socket = new DatagramSocket(p);
                socket.setReuseAddress(true);
            } catch (IOException e) {
                if (ports==null) {
                    ports = new HashSet<Integer>();
                    busyPorts.put(addr, ports);
                }
                ports.add(p);
                return false;
            } finally { 
                if (socket != null) 
                    socket.close(); 
            }            
        }
        return true;
    }

    public Integer getStreamsCount()
    {
        streamsLock.readLock().lock();
        try
        {
            int count = 0;
            for (Map portStreams: streams.values())
                count += portStreams==null? 0 : portStreams.size();

            return count;
        }
        finally
        {
            streamsLock.readLock().unlock();
        }
    }

/*    private Map<InetAddress, RtpAddress> getAvailableAddresses() throws UnknownHostException
    {
        Collection<Node> childs = getChildrens();
        Map<InetAddress, RtpAddressNode> res = null;
        if (childs!=null && !childs.isEmpty())
        {
            for (Node child: childs)
                if (child instanceof RtpAddressNode && Status.STARTED.equals(child.getStatus()))
                {
                    if (res==null)
                        res = new HashMap<InetAddress, RtpAddressNode>();
                    res.put(InetAddress.getByName(child.getName()), (RtpAddressNode)child);
                }
        }

        return res==null? Collections.EMPTY_MAP : res;
    }*/
    
}
