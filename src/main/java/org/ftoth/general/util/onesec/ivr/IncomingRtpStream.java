package org.ftoth.general.util.onesec.ivr;

import javax.media.format.AudioFormat;

public interface IncomingRtpStream extends RtpStream
{
    /**
     * Open the incoming input stream
     * @param remoteHost the host, from which audio stream incoming
     * @param remotePort the port from which
     * @throws RtpStreamException
     */
    public void open(String remoteHost) throws RtpStreamException;
    /**
     * Returns <b>true</b> if listener was successfully added or <b>false</b> if incoming rtp stream
     * is already closed.
     * @param listener the listener
     * @param format the audio format 
     * @throws RtpStreamException
     */
    public boolean addDataSourceListener(IncomingRtpStreamDataSourceListener listener, AudioFormat format)
            throws RtpStreamException;
}
