package org.ftoth.general.util.onesec.ivr;

import javax.media.protocol.DataSource;

public interface IncomingRtpStreamDataSourceListener
{
    /**
     * Fires when the data source was created for this listener (for each listener creating unique
     * data source)
     * @param dataSource unique data source for this listener.
     * @see IncomingRtpStream
     */
    public void dataSourceCreated(IncomingRtpStream stream, DataSource dataSource);
    /**
     * Fires when the incoming rtp stream closing.
     * @param dataSource
     * @see IncomingRtpStream
     */
    public void streamClosing(IncomingRtpStream stream);
}
