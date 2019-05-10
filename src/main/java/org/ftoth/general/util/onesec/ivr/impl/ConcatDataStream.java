package org.ftoth.general.util.onesec.ivr.impl;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.media.Buffer;
import javax.media.Format;
import javax.media.protocol.BufferTransferHandler;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.PushBufferStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ftoth.general.util.onesec.Codec;

public class ConcatDataStream implements PushBufferStream
{
	private static final Log log = LogFactory.getLog(ConcatDataStream.class);

	public static final int MAX_QUEUE_SIZE = 7;
	public static int MAX_SILENCE_BUFFER_COUNT = 1500;
	public static final int MAX_TIME_SKEW = 300;

	private final Buffer silentBuffer;
	private final Queue<Buffer> bufferQueue;
	private final ConcatDataSource dataSource;
	private final ContentDescriptor contentDescriptor;
	private final int packetLength; // ms?

	private BufferTransferHandler transferHandler;
	private Buffer bufferToSend;
	private String action;
	private long packetNumber;
	private long sleepTime;
	private AtomicInteger silencePacketCount = new AtomicInteger(0);
	private String logPrefix;
	private AtomicReference<ConcatDataSource.SourceProcessor> sourceInfo = new AtomicReference<ConcatDataSource.SourceProcessor>();
	private AtomicInteger emptyQueueEvents = new AtomicInteger(0);

	public ConcatDataStream(Queue<Buffer> bufferQueue, ConcatDataSource dataSource, int packetSize, Codec codec, int maxSendAheadPacketsCount, Buffer silentBuffer)
	{
		this.bufferQueue = bufferQueue;
		this.dataSource = dataSource;
		this.contentDescriptor = new ContentDescriptor(dataSource.getContentType());
		this.packetLength = (int) codec.getMillisecondsForPacketSize(packetSize);
		// this.packetLength = (int) dataSource.getPacketSizeInMillis();
		this.silentBuffer = silentBuffer;
	}

	void sourceInitialized(ConcatDataSource.SourceProcessor source)
	{
		sourceInfo.set(source);
		emptyQueueEvents.set(0);
	}

	void sourceClosed(ConcatDataSource.SourceProcessor source)
	{
		sourceInfo.compareAndSet(source, null);
	}

	public String getLogPrefix()
	{
		return logPrefix;
	}

	public void setLogPrefix(String logPrefix)
	{
		this.logPrefix = logPrefix;
	}

	public Format getFormat()
	{
		return dataSource.getFormat();
	}

	public void read(Buffer buffer)
			throws IOException
	{
		action = "reading buffer";
		if (bufferToSend == null) {
			silencePacketCount.incrementAndGet();
			buffer.copy(silentBuffer);
		}
		else {
			silencePacketCount.set(0);
			buffer.copy(bufferToSend);
		}
	}

	@Override
	public void setTransferHandler(BufferTransferHandler transferHandler)
	{
		this.transferHandler = transferHandler;
	}

	public ContentDescriptor getContentDescriptor()
	{
		return contentDescriptor;
	}

	public long getContentLength()
	{
		return LENGTH_UNKNOWN;
	}

	public boolean endOfStream()
	{
		return bufferQueue.size() == 0 && dataSource.isClosed();
	}

	public Object[] getControls()
	{
		return new Object[0];
	}

	public Object getControl(String controlType)
	{
		return null;
	}

	private void sendBuffer()
	{
		if (transferHandler != null)
			transferHandler.transferData(this);
	}

	public String getStatusMessage()
	{
		return String.format(logMess("Transfering buffers to rtp session. Action: %s. packetCount: %s; sleepTime: %s", action, packetNumber, sleepTime));
	}

	public void run()
	{
		dataSource.setStreamThreadRunning(true);
		try {
			long startTime = System.currentTimeMillis();
			packetNumber = 0;
			ConcatDataSource.SourceProcessor si = null;
			if (log.isDebugEnabled()) {
				log.debug(logMess("Concat stream started with time quant %s ms", packetLength));
			}
			long prevTime = System.currentTimeMillis();
			long maxTransferTime = 0;
			long droppedPacketCount = 0;
			long transferTimeSum = 0;
			long emptyBufferEventCount = 0;
			while ((!dataSource.isClosed() || !bufferQueue.isEmpty()) && silencePacketCount.get() < MAX_SILENCE_BUFFER_COUNT) {
				try {
					long cycleStartTs = System.currentTimeMillis();
					action = "getting new buffer from queue";
					si = sourceInfo.get();
					bufferToSend = bufferQueue.poll();
					if (bufferToSend != null && si != null && si.isRealTime()) {
						if (bufferToSend.getTimeStamp() + MAX_TIME_SKEW < cycleStartTs || bufferQueue.size() > MAX_QUEUE_SIZE) {
							droppedPacketCount++;
							continue;
						}
					}
					action = "sending transfer event";
					if (bufferToSend != null || si == null || !si.isRealTime()) {
						sendBuffer();
						long tt = System.currentTimeMillis() - cycleStartTs;
						transferTimeSum += tt;
						if (tt > maxTransferTime)
							maxTransferTime = tt;
					}
					++packetNumber;
					action = "sleeping";
					long curTime = System.currentTimeMillis();
					long timeDiff = curTime - startTime;
					long expectedPacketNumber = timeDiff / packetLength;
					long correction = timeDiff % packetLength;
					emptyBufferEventCount = expectedPacketNumber - packetNumber;
					if (si != null && log.isDebugEnabled() && curTime - prevTime > 30000) {
						prevTime = curTime;
						log.debug(logMess("Empty buffers events count: %s; " + "avgTransferTime: %s; maxTransferTime: %s; buffers size: %s; " + " dropped packets count: %s", emptyBufferEventCount,
								transferTimeSum / packetNumber, maxTransferTime, bufferQueue.size(), droppedPacketCount));
					}
					sleepTime = (packetNumber - expectedPacketNumber) * packetLength - correction;
					if (sleepTime > 0)
						TimeUnit.MILLISECONDS.sleep(sleepTime);
				}
				catch (InterruptedException ex) {
					log.error(logMess("Transfer buffers to rtp session task was interrupted"), ex);
					Thread.currentThread().interrupt();
					break;
				}
			}
			if (log.isDebugEnabled()) {
				log.debug(logMess("Transfer buffers to rtp session task was finished"));
				log.debug(logMess("Empty buffers events count: %s; " + "avgTransferTime: %s; maxTransferTime: %s; buffers size: %s; " + " dropped packets count: %s", emptyBufferEventCount,
						transferTimeSum / packetNumber, maxTransferTime, bufferQueue.size(), droppedPacketCount));
			}
		}
		finally {
			dataSource.setStreamThreadRunning(false);
		}
	}

	private String logMess(String mess, Object... args)
	{
		return dataSource.logMess(mess, args);
	}

	private class SourceInfo
	{
		private long expectedSourceBufferNumber = 0;
		private long sourceBufferNumber = 0;
	}
}
