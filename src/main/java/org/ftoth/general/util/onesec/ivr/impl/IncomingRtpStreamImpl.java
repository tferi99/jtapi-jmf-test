package org.ftoth.general.util.onesec.ivr.impl;

import java.io.IOException;
import java.net.InetAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.media.Controller;
import javax.media.DataSink;
import javax.media.MediaLocator;
import javax.media.Processor;
import javax.media.format.AudioFormat;
import javax.media.protocol.DataSource;
import javax.media.protocol.PushBufferDataSource;
import javax.media.rtp.GlobalReceptionStats;
import javax.media.rtp.RTPManager;
import javax.media.rtp.ReceiveStream;
import javax.media.rtp.ReceiveStreamListener;
import javax.media.rtp.SessionAddress;
import javax.media.rtp.event.ByeEvent;
import javax.media.rtp.event.NewReceiveStreamEvent;
import javax.media.rtp.event.ReceiveStreamEvent;
import javax.media.rtp.event.RemotePayloadChangeEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ftoth.general.util.onesec.ivr.IncomingRtpStream;
import org.ftoth.general.util.onesec.ivr.IncomingRtpStreamDataSourceListener;
import org.ftoth.general.util.onesec.ivr.RTPManagerService;
import org.ftoth.general.util.onesec.ivr.RtpStreamException;

public class IncomingRtpStreamImpl extends AbstractRtpStream 
        implements IncomingRtpStream, ReceiveStreamListener
{
	private static final Log log = LogFactory.getLog(IncomingRtpStreamImpl.class);
	
    public enum Status {INITIALIZING, OPENED, CLOSED}
    
    public final static AudioFormat FORMAT = new AudioFormat(
            AudioFormat.LINEAR, 8000, 16, 1, AudioFormat.LITTLE_ENDIAN, AudioFormat.SIGNED) ;

    private RTPManagerService rtpManagerService;

    private RTPManager rtpManager;
    private SessionAddress destAddress;
    private ReceiveStream stream;
//    private DataSource source; //SourceClonable
    private DataSourceCloneBuilder sourceCloneBuilder; //SourceClonable
    private boolean firstConsumerAdded;
    private List<Consumer> consumers;
    private Lock lock;
    private Status status;

    public IncomingRtpStreamImpl(InetAddress address, int port, RTPManagerService rtpManagerService)
    {
        super(address, port, "Incoming RTP");
        
        this.rtpManagerService = rtpManagerService;
        status = Status.INITIALIZING;
        consumers = new LinkedList<Consumer>();
        lock = new ReentrantLock();
        firstConsumerAdded = false;
    }

    public long getHandledBytes()
    {
        return 0;
    }

    public long getHandledPackets()
    {
        return 0;
    }

    @Override
    public void doRelease() throws Exception
    {
        try{
            try{
                try {
                    if (sourceCloneBuilder!=null)
                        sourceCloneBuilder.close();
                } finally {
                    if (!consumers.isEmpty())
                        for (Consumer consumer: consumers)
                            consumer.fireStreamClosingEvent();
                }
            }finally{
                if (rtpManager!=null) {
                    GlobalReceptionStats stats = rtpManager.getGlobalReceptionStats();
                    incHandledBytesBy(stats.getBytesRecd());
                    incHandledPacketsBy(stats.getPacketsRecd());
                    rtpManager.removeTargets("Disconnected");
                }
            }
        }finally{
            if (rtpManager!=null)
                rtpManager.dispose();
        }
    }

    public void open(String remoteHost) throws RtpStreamException
    {
        try
        {
            this.remoteHost = remoteHost;
            if (log.isDebugEnabled()) {
                log.debug(logMess(
                        "Trying to open incoming RTP stream from the remote host (%s)"
                        , remoteHost));
            }
            
            rtpManager = rtpManagerService.createRtpManager();
            rtpManager.addReceiveStreamListener(this);
            rtpManager.initialize(new SessionAddress(address, port));
            InetAddress dest = InetAddress.getByName(remoteHost);
            destAddress = new SessionAddress(dest, SessionAddress.ANY_PORT);
            rtpManager.addTarget(destAddress);
        }
        catch(Exception e)
        {
            throw new RtpStreamException(logMess(
                        "Error creating receiver for RTP stream from remote host (%s)"
                        , remoteHost)
                    , e);
        }
    }

    public boolean addDataSourceListener(IncomingRtpStreamDataSourceListener listener, AudioFormat format)
        throws RtpStreamException
    {
        try{
            if (lock.tryLock(100, TimeUnit.MILLISECONDS)){
                try{
                    if (status==Status.CLOSED)
                        return false;
                    else if (status==Status.OPENED || status==Status.INITIALIZING){
                        Consumer consumer = new Consumer(listener, format);
                        consumers.add(consumer);
                        if (status==Status.OPENED)
                            consumer.fireDataSourceCreatedEvent();
                    } 
                }finally{
                    lock.unlock();
                }
                return true;
            } else {
                log.error(logMess("Error adding listener. Lock wait timeout"));
                throw new RtpStreamException("Error adding listener. Lock wait timeout");
            }
        }
        catch(InterruptedException e){
            log.error(logMess("Error adding listener"), e);
            throw new RtpStreamException("Error adding listener to the IncomingRtpStream", e);
        }
    }

    public void update(final ReceiveStreamEvent event)
    {
        try {
        	if (log.isDebugEnabled()) {
                log.debug(logMess("Received stream event (%s)", event.getClass().getName()));
        	}

            if (event instanceof NewReceiveStreamEvent) {
                initStream(event);
            } else if (event instanceof ByeEvent) {
                lock.lock();
                try {
                    status = Status.CLOSED;
                } finally {
                    lock.unlock();
                }
            } else if (event instanceof RemotePayloadChangeEvent) {
                RemotePayloadChangeEvent payloadEvent = (RemotePayloadChangeEvent) event;
                if (log.isDebugEnabled()) {
                	log.debug(logMess("Payload changed to %d", payloadEvent.getNewPayload()));
                }
                if (payloadEvent.getNewPayload()<19) {
                	if (log.isDebugEnabled()) {
                        log.debug("Trying to handle received stream");
                	}
                    initStream(event);
                }
            }
        } catch (Exception e) {
            log.error(logMess("Error initializing rtp data source"), e);
            status = Status.CLOSED;
        }
    }
    
    private void initStream(final ReceiveStreamEvent event) throws IOException {
        stream = event.getReceiveStream();
        if (log.isDebugEnabled()) {
            log.debug(logMess("Received new stream"));
        }

        sourceCloneBuilder = new DataSourceCloneBuilder(
                (PushBufferDataSource)stream.getDataSource(), logMess(""));
        sourceCloneBuilder.open();
        lock.lock();
        try{
        	if (log.isDebugEnabled()) {
        		log.debug(logMess("Sending dataSourceCreatedEvent to consumers"));
        	}
            status = Status.OPENED;
            if (!consumers.isEmpty())
                for (Consumer consumer: consumers)
                    consumer.fireDataSourceCreatedEvent();

        }finally{
            lock.unlock();
        }
    }

    private void saveToFile(DataSource ds, String filename, final long closeAfter) throws Exception
    {
//
//            new Thread(){
//                @Override
//                public void run(){
//                    try{
//                        Thread.sleep(4000);
//                        DataSource ds = stream.getDataSource();
//
//                        RTPControl ctl = (RTPControl)ds.getControl("javax.media.rtp.RTPControl");
//                        if (ctl!=null)
//                            if (owner.isLogLevelEnabled(LogLevel.DEBUG))
//                                owner.getLogger().debug(logMess("The format of the stream: %s", ctl.getFormat()));
//
//                        // create a player by passing datasource to the Media Manager
//
//                        ds = javax.media.Manager.createCloneableDataSource(ds);
//                        SourceCloneable cloneable = (SourceCloneable) ds;
//                        saveToFile(ds,"test.wav", 10000);
//                        Thread.sleep(5000);
//                        saveToFile(cloneable.createClone(),"test2.wav", 5000);
//                    }
//                    catch(Exception e){
//                            owner.getLogger().error(logMess("Error."), e);
//                    }
//                }
//            }.start();

        Processor p = javax.media.Manager.createProcessor(ds);
        p.configure();
        waitForState(p, Processor.Configured);
        p.getTrackControls()[0].setFormat(new AudioFormat(
                AudioFormat.LINEAR, 8000, 16, 1, AudioFormat.LITTLE_ENDIAN, AudioFormat.SIGNED));
//        p.setContentDescriptor(new FileTypeDescriptor(FileTypeDescriptor.WAVE));
        p.realize();
        waitForState(p, Processor.Realized);
        final DataSink fileWriter = javax.media.Manager.createDataSink(
                p.getDataOutput(), new MediaLocator("file:///home/tim/tmp/"+filename));
        fileWriter.open();
        p.start();
        fileWriter.start();
        new Thread(){
            @Override
            public void run() {
                try {
                    Thread.sleep(closeAfter);
                    fileWriter.stop();
                    fileWriter.close();
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private void fireDataSourceCreated()
    {
        for (Consumer consumer: consumers)
            if (!consumer.createEventFired)
                consumer.fireDataSourceCreatedEvent();
    }

    private static void waitForState(Controller p, int state) throws Exception
    {
        long startTime = System.currentTimeMillis();
        while (p.getState()!=state)
        {
            TimeUnit.MILLISECONDS.sleep(5);
            if (System.currentTimeMillis()-startTime>200)
                throw new Exception("Processor state wait timeout");
        }
    }

    private class Consumer {
        private final IncomingRtpStreamDataSourceListener listener;
        private final AudioFormat format;

        private boolean createEventFired = false;
        private Processor processor;
        private DataSource inDataSource;
        private DataSource outDataSource;
        private boolean closed = false;

        public Consumer(IncomingRtpStreamDataSourceListener listener, AudioFormat format) {
            this.listener = listener;
            this.format = format==null? FORMAT : format; 
        }

        private void fireDataSourceCreatedEvent() {
            createEventFired = true;
            try {
                synchronized(this) {
                    if (closed) {
                    	if (log.isDebugEnabled()) {
                            log.debug(logMess("Can't create data source for consumer because "
                                    + "of consumer already closed"));
                    	}
                        return;
                    }
                    inDataSource = sourceCloneBuilder.createClone();
                    inDataSource = new RealTimeDataSource((PushBufferDataSource)inDataSource, logPrefix);
                }
                listener.dataSourceCreated(IncomingRtpStreamImpl.this, inDataSource);
            } catch(Exception e) {
                log.error(logMess("Error creating data source for consumer"), e);
                listener.dataSourceCreated(IncomingRtpStreamImpl.this, null);
            }
        }

        private void fireStreamClosingEvent() {
            try{
                try {
                    listener.streamClosing(IncomingRtpStreamImpl.this);
                } finally{
                    closeResources();
                }
            }catch(Exception e){
                log.error(logMess("Error closing data source consumer resources"), e);
            }
        }
        
        private synchronized void closeResources() throws Exception {
            closed = true;
            try {
                try {
                    try {
                        if (inDataSource!=null)
                            inDataSource.disconnect();
                    } finally {
                        if (processor!=null)
                            processor.stop();
                    }
                } finally {
                    if (processor!=null)
                        processor.close();
                }
            } finally {
                if (outDataSource!=null)
                    outDataSource.stop();
            }
        }
    }
}