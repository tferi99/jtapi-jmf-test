package org.ftoth.general.util.onesec.ivr.impl;

import java.io.IOException;

import javax.media.Buffer;
import javax.media.Format;
import javax.media.protocol.BufferTransferHandler;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.PushBufferStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class RealTimeDataStream implements PushBufferStream, BufferTransferHandler
{
	private static final Log log = LogFactory.getLog(RealTimeDataStream.class);    
	
    public static final int MAX_TIME_SKEW = 150;
    
    private final PushBufferStream stream;
    private long packetLengthInMillis=0;
    private BufferTransferHandler transferHandler;
    private RealTimeDataSource source;
    private long counter = 0;
    private long startTs = 0;
    private volatile long discardedBuffersCount = 0;
//    private volatile boolean disconnected = false;

    public RealTimeDataStream(RealTimeDataSource source, PushBufferStream stream) {
        this.stream = stream;
        this.source = source;
        this.stream.setTransferHandler(this);
    }

    public long getDiscardedBuffersCount() {
        return discardedBuffersCount;
    }

    public Format getFormat() {
        return stream.getFormat();
    }
    
    public void disconnect() {
        System.out.println("!! disconnecting !!");
        transferHandler = null;
    }

    public void read(Buffer buffer) throws IOException {
        stream.read(buffer);
//        if (packetLengthInMillis!=-1) {
//            if (packetLengthInMillis==0) {
//                packetLengthInMillis = Codec.getMillisecondsForFormat(stream.getFormat(), buffer.getLength());
//                if (packetLengthInMillis==0) {
//                    if (logger.isWarnEnabled()) 
//                        logger.warn(source.logMess(
//                                "Can't detect packet size in milliseconds for format (%s)"
//                                , buffer.getFormat()));
//                    packetLengthInMillis=-1;
//                } else if (logger.isDebugEnabled())
//                    logger.debug(source.logMess(
//                            "The incoming stream packet length in millisecods is (%s)", packetLengthInMillis));
//            }
//            if (packetLengthInMillis>0) {
//                if (counter==0) 
//                    buffer.setTimeStamp(startTs=System.currentTimeMillis());
//                else
//                    buffer.setTimeStamp(startTs+counter*packetLengthInMillis);
//                if (buffer.getTimeStamp()+MAX_TIME_SKEW < System.currentTimeMillis()) {
//                    buffer.setDiscard(true);
//                    ++discardedBuffersCount;
//                }
//                counter++;
//            }
//        }
    }

    public void setTransferHandler(BufferTransferHandler transferHandler) {
        this.transferHandler = transferHandler;
    }

    public ContentDescriptor getContentDescriptor() {
        return stream.getContentDescriptor();
    }

    public long getContentLength() {
        return stream.getContentLength();
    }

    public boolean endOfStream() {
        return stream.endOfStream();
    }

    public Object[] getControls() {
        return stream.getControls();
    }

    public Object getControl(String controlType) {
        return stream.getControl(controlType);
    }

    public void transferData(PushBufferStream stream) {
        if (transferHandler!=null)
            transferHandler.transferData(this);
    }
}
