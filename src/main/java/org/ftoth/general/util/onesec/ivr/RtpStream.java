package org.ftoth.general.util.onesec.ivr;

/**
 * The base contract for {@link OutgoingRtpStream} and {@link IncomingRtpStream} rtp streams.
 * @author Mikhail Titov
 */
public interface RtpStream extends RtpAddress
{
    /**
     * Do not call this method direct. This method must be used by {@link RtpStreamManager}.
     * @param rtpStat the object that aggregates the global statistics
     */
//    public void init(RtpStat rtpStat);
    /**
     * Releases rtp stream
     */
    public void release();
    /**
     * Returns amount of bytes handled by stream.
     */
    public long getHandledBytes();
    /**
     * Returns amount of packets handled by stream.
     */
    public long getHandledPackets();
    /**
     * Returns the address of the remote side
     */
    public String getRemoteHost();
    /**
     * Returns the port on the remote side
     */
    public int getRemotePort();
    /**
     * Returns the stream creation time
     */
    public long getCreationTime();
    
    public void setLogPrefix(String prefix);
}
