package org.ftoth.general.util.onesec.ivr;

public interface RtpStreamManager
{
    /**
     * Return the incoming rtp stream (the object that can recieve the incoming rtp stream).
     * The stream must be {@link RtpStreamHandler#release() released}
     * after use.
     */
    public IncomingRtpStream getIncomingRtpStream(String owner);
    /**
     * Returns the outgoing rtp stream. The stream must be {@link RtpStream#release() released}
     * after use.
     */
    public OutgoingRtpStream getOutgoingRtpStream(String owner);
    /**
     * Reserves the address by then node passed in the parameter. Next time the call of the method
     * {@link #getOutgoingRtpStream(Node)} returns the stream with the address and port reserved by
     * this method.
     * @param node the node which reserves the pair of the ip address and the port
     * @return the reserved address and the port
     */
    public RtpAddress reserveAddress(String owner);
    /**
     * Unreserve the address and the port reserved for this node
     * @param node
     */
    public void unreserveAddress(String owner);
    
    public CodecManager getCodecManager();
    public BufferCache getBufferCache();
}
