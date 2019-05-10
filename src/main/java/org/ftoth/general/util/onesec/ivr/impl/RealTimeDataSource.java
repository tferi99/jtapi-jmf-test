package org.ftoth.general.util.onesec.ivr.impl;

import java.io.IOException;

import javax.media.Time;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;

/**
 *
 * @author Mikhail Titov
 */
public class RealTimeDataSource extends PushBufferDataSource {

    private final PushBufferDataSource source;
    private final RealTimeDataStream[] streams;
    private final String logPrefix;

    public RealTimeDataSource(PushBufferDataSource source, String logPrefix) {
        this.source = source;
        this.logPrefix = logPrefix;
        streams = new RealTimeDataStream[]{new RealTimeDataStream(this, source.getStreams()[0])};
    }
    
    public long getDiscardedBuffersCount() {
        return streams[0].getDiscardedBuffersCount();
    }

    @Override
    public PushBufferStream[] getStreams() {
        return streams;
    }

    @Override
    public String getContentType() {
        return source.getContentType();
    }

    @Override
    public void connect() throws IOException {
        source.connect();
    }

    @Override
    public void disconnect() {
        source.disconnect();
    }

    @Override
    public void start() throws IOException {
        source.start();
    }

    @Override
    public void stop() throws IOException {
        source.stop();
    }

    @Override
    public Object getControl(String control) {
        return source.getControl(control);
    }

    @Override
    public Object[] getControls() {
        return source.getControls();
    }

    @Override
    public Time getDuration() {
        return source.getDuration();
    }
    
    String logMess(String mess, Object... args) {
        return (logPrefix==null? "" : logPrefix)+" RealTimeSource. "+String.format(mess, args);
    }
}
