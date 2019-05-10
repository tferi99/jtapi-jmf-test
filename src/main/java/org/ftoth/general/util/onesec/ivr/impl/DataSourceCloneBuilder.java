package org.ftoth.general.util.onesec.ivr.impl;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.media.Buffer;
import javax.media.Format;
import javax.media.Time;
import javax.media.protocol.BufferTransferHandler;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DataSourceCloneBuilder implements BufferTransferHandler
{
	private static final Log log = LogFactory.getLog(DataSourceCloneBuilder.class);
	
    private final PushBufferDataSource source;
    private final Set<DataSourceClone> clones = new HashSet<DataSourceClone>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final String logPrefix;
    private Buffer bufferToSend;

    public DataSourceCloneBuilder(PushBufferDataSource source, String logPrefix) {
        this.source = source;
        this.logPrefix = logPrefix;
        this.source.getStreams()[0].setTransferHandler(this);
        if (log.isDebugEnabled()) {
            log.debug(logMess("Created"));
        }
    }
    
    public PushBufferDataSource createClone() {
    	if (log.isDebugEnabled()) {
            log.debug(logMess("Creating new clone"));
    	}
        lock.writeLock().lock();
        try {
            DataSourceClone clone = new DataSourceClone();
            clones.add(clone);
            return clone;
        } finally  {
            lock.writeLock().unlock();
        }
    }
    
    private void removeClone(DataSourceClone clone) {
    	if (log.isDebugEnabled()) {
            log.debug(logMess("Removing clone"));
    	}
        lock.writeLock().lock();
        try {
            clones.remove(clone);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public void open() throws IOException {
    	if (log.isDebugEnabled()) {
            log.debug(logMess("Initializing"));
    	}
        source.connect();
        source.start();
    }
    
    public void close() {
    	if (log.isDebugEnabled()) {
            log.debug(logMess("Closing"));
    	}
        try {
            try {
                source.stop();
            } finally {
                source.disconnect();
            }
        } catch (Exception e) {
            
        }
    }

    public void transferData(PushBufferStream stream) {
        Buffer buffer = new Buffer();
        try {
            stream.read(buffer);
            lock.readLock().lock();
            try {
                for (DataSourceClone clone: clones) {
                    bufferToSend = new Buffer();
                    bufferToSend.copy(buffer);
                    clone.streams[0].sendData();
                }
            } finally {
                lock.readLock().unlock();
            }
        } catch (IOException ex) {
        }
    }
    
    String logMess(String mess, Object... args) {
        return (logPrefix==null? "" : logPrefix)+"DataSourceCloneBuilder. "+String.format(mess, args);
    }
    
    private class DataSourceClone extends PushBufferDataSource {
        
        private volatile boolean connected = false;
        private final DataStreamClone[] streams = new DataStreamClone[]{new DataStreamClone()};

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
            connected = true;
        }

        @Override
        public void disconnect() {
            connected = false;
            removeClone(this);
        }

        @Override
        public void start() throws IOException {
        }

        @Override
        public void stop() throws IOException {
        }

        @Override
        public Object getControl(String controlType) {
            return source.getControl(controlType);
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
    
    private class DataStreamClone implements PushBufferStream {
        
        private volatile BufferTransferHandler transferHandler;

        public Format getFormat() {
            return source.getStreams()[0].getFormat();
        }

        public void read(Buffer buffer) throws IOException {
            if (bufferToSend!=null)
                buffer.copy(bufferToSend);
            else
                buffer.setDiscard(true);
        }
        
        public void sendData(){
            BufferTransferHandler handler = transferHandler;
            if (handler!=null)
                handler.transferData(this);
        }

        public void setTransferHandler(BufferTransferHandler transferHandler) {
            this.transferHandler = transferHandler;
        }

        public ContentDescriptor getContentDescriptor() {
            return source.getStreams()[0].getContentDescriptor();
        }

        public long getContentLength() {
            return source.getStreams()[0].getContentLength();
        }

        public boolean endOfStream() {
            return source.getStreams()[0].endOfStream();
        }

        public Object[] getControls() {
            return source.getStreams()[0].getControls();
        }

        public Object getControl(String controlType) {
            return source.getStreams()[0].getControl(controlType);
        }
    }
}
