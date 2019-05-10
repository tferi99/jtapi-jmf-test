package org.ftoth.general.util.onesec.ivr.impl;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.media.Buffer;
import javax.media.Format;
import javax.media.protocol.BufferTransferHandler;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.PullBufferStream;
import javax.media.protocol.PushBufferStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class PullToPushConverterDataStream implements PushBufferStream
{
	private static final Log log = LogFactory.getLog(PullToPushConverterDataStream.class);
	
    private final PullBufferStream sourceStream;
    private volatile BufferTransferHandler transferHandler;
    private volatile Buffer bufferToSend;
    private volatile boolean stop = false;
    private volatile boolean endOfStream = false;
    private volatile boolean pause = true;

    public PullToPushConverterDataStream(PullBufferStream sourceStream) {
        this.sourceStream = sourceStream;
    }

    public Format getFormat() {
        return sourceStream.getFormat();
    }

    public void setTransferHandler(BufferTransferHandler transferHandler) {
        this.transferHandler = transferHandler;
    }

    public ContentDescriptor getContentDescriptor() {
        return sourceStream.getContentDescriptor();
    }

    public long getContentLength() {
        return sourceStream.getContentLength();
    }

    public boolean endOfStream() {
        return sourceStream.endOfStream();
    }

    public Object[] getControls() {
        return sourceStream.getControls();
    }

    public Object getControl(String controlType) {
        return sourceStream.getControl(controlType);
    }

    public String getStatusMessage() {
        return "Converting PullDataSource to PushDataSource";
    }
    
    void stop() {
        stop = true;
    }
    
    void reset() {
        stop = false;
    }
    
    void pause() {
        pause = true;
    }
    
    void cont() {
        pause = false;
    }

    public void read(Buffer buffer) throws IOException {
        buffer.copy(bufferToSend);
    }

    public void run() {
        try {
            while (!stop && !sourceStream.endOfStream() && !endOfStream) {
                if (pause) 
                    TimeUnit.MILLISECONDS.sleep(1);
                else {
                    bufferToSend = new Buffer();
                    sourceStream.read(bufferToSend);
                    if (bufferToSend.isEOM())
                        endOfStream = true;
                    if (!bufferToSend.isDiscard()) {
                        BufferTransferHandler _handler = transferHandler;
                        if (_handler!=null)
                            _handler.transferData(this);
                    }
                }
            }
        } catch (Throwable e) {
            log.error("Error converting PullBufferDataSource to PushBufferDataSource", e);
        }
    }
}
