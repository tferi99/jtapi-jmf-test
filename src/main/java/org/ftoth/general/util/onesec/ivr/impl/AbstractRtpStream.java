package org.ftoth.general.util.onesec.ivr.impl;

import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ftoth.general.util.onesec.ivr.RtpStream;
import org.ftoth.general.util.onesec.ivr.RtpStreamManager;

/**
 *
 * @author Mikhail Titov
 */
public abstract class AbstractRtpStream implements RtpStream
{
	private static final Log log = LogFactory.getLog(AbstractRtpStream.class);
	
    protected final InetAddress address;
    protected final int port;
    private final String streamType;
    protected final long creationTime;

    protected String remoteHost;
    protected int remotePort;

    private AtomicLong handledPackets;
    private AtomicLong handledBytes;
    private RtpStreamManager manager;
    protected String logPrefix;
    private AtomicBoolean released;

    public AbstractRtpStream(InetAddress address, int port, String streamType)
    {
        this.address = address;
        this.port = port;
        this.streamType = streamType;
        this.creationTime = System.currentTimeMillis();

        handledBytes = new AtomicLong();
        handledPackets = new AtomicLong();
        released = new AtomicBoolean(false);
    }

    public long getCreationTime()
    {
        return creationTime;
    }

    public InetAddress getAddress()
    {
        return address;
    }

    public int getPort()
    {
        return port;
    }

    public String getRemoteHost() {
        return remoteHost;
    }

    public int getRemotePort() {
        return remotePort;
    }

    void setManager(RtpStreamManager manager)
    {
        this.manager = manager;
    }

    public String getLogPrefix() {
        return logPrefix;
    }

    public void setLogPrefix(String logPrefix) {
        this.logPrefix = logPrefix;
    }

    protected void incHandledPacketsBy(long packets)
    {
        handledPackets.addAndGet(packets);
        ///manager.incHandledPackets(this, packets);
    }

    protected void incHandledBytesBy(long bytes)
    {
        handledBytes.addAndGet(bytes);
        ///manager.incHandledBytes(this, bytes);
    }

    public void release() {
        if (!released.compareAndSet(false, true)) {
            if (log.isDebugEnabled()) {
                log.debug(logMess("Can't release stream because of already released"));
            }
            return;
        }
        try {
        	if (log.isDebugEnabled()) {
                log.debug(logMess("Releasing stream..."));
        	}
            doRelease();
            if (log.isDebugEnabled()) {
                log.debug(logMess("Stream realeased"));
            }
            //manager.releaseStream(this);
        } 
        catch(Exception e) {
            log.error("Error releasing RTP stream", e);
        }
    }
    
    protected String logMess(String mess, Object... args)
    {
        return (logPrefix==null? "" : logPrefix)+streamType+". "+String.format(mess, args);
    }

    public abstract void doRelease() throws Exception;
}
