package org.ftoth.general.util.onesec.ivr.impl;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.media.Time;
import javax.media.protocol.PullBufferDataSource;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;

/**
 *
 * @author Mikhail Titov
 */
public class PullToPushConverterDataSource extends PushBufferDataSource {
    
    private final PullBufferDataSource source;
    private final PullToPushConverterDataStream[] streams;
    private AtomicBoolean connected = new AtomicBoolean();
    private AtomicBoolean started = new AtomicBoolean();

    public PullToPushConverterDataSource(PullBufferDataSource source) {
        this.source = source;
        this.streams = new PullToPushConverterDataStream[]{new PullToPushConverterDataStream(
                source.getStreams()[0])};
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
        if (!connected.compareAndSet(false, true))
            return;
        source.connect();
        streams[0].reset();
/*        try {
            executor.execute(streams[0]);
        } catch (ExecutorServiceException ex) {
            throw new IOException("Error starting Pull to Push BufferDataSource converter", ex);
        }*/
    }

    @Override
    public void disconnect() {
        if (connected.compareAndSet(true, false)) {
            try {
                source.disconnect();
            } finally {
                streams[0].stop();
            }
        }
    }

    @Override
    public void start() throws IOException {
        if (connected.get() && started.compareAndSet(false, true)) {
            streams[0].cont();
            source.start();
        }
    }

    @Override
    public void stop() throws IOException {
        if (started.compareAndSet(true, false))
            try {
                source.stop();
            } finally {
                streams[0].pause();
            }
    }

    @Override
    public Object getControl(String controlName) {
        return source.getControl(controlName);
    }

    @Override
    public Object[] getControls() {
        return source.getControls();
    }

    @Override
    public Time getDuration() {
        return source.getDuration();
    }
}
