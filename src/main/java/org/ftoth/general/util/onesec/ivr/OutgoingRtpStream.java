package org.ftoth.general.util.onesec.ivr;

public interface OutgoingRtpStream  extends RtpStream
{
    public void open(String remoteHost, int remotePort, AudioStream audioStream) throws RtpStreamException;
    public void start() throws RtpStreamException;
    public void setLogPrefix(String prefix);
    public String getLogPrefix();
}
