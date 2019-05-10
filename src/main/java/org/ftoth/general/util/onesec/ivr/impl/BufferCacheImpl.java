package org.ftoth.general.util.onesec.ivr.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import javax.media.Buffer;
import javax.media.control.PacketSizeControl;
import javax.media.protocol.FileTypeDescriptor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ftoth.general.util.onesec.Codec;
import org.ftoth.general.util.onesec.ivr.BufferCache;
import org.ftoth.general.util.onesec.ivr.BuffersCacheEntity;
import org.ftoth.general.util.onesec.ivr.CodecManager;
import org.ftoth.general.util.onesec.ivr.RTPManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("bufferCache")
public class BufferCacheImpl implements BufferCache
{
	private static final Log log = LogFactory.getLog(BufferCacheImpl.class);

	//public final static String SILENCE_RESOURCE_NAME = "/org/ftoth/general/onesec/ivr/silence.wav";
	public final static String SILENCE_RESOURCE_NAME = "/org/ftoth/general/onesec/ivr/tada.wav";
	
	public final static int WAIT_STATE_TIMEOUT = 2000;
	public final static long DEFAULT_MAX_CACHE_IDLE_TIME = 3600l;

	private final Map<String, Buffer> silentBuffers = new ConcurrentHashMap<String, Buffer>();
	private final Map<String, BuffersCacheEntity> buffersCache = new ConcurrentHashMap<String, BuffersCacheEntity>();

	@Autowired
	private RTPManagerService rtpManagerService;

	@Autowired
	private CodecManager codecManager;

	private AtomicLong maxCacheIdleTime = new AtomicLong(DEFAULT_MAX_CACHE_IDLE_TIME);

	public BufferCacheImpl()
	{
	}

	public Buffer getSilentBuffer(Codec codec, int packetSize)
	{
		String key = codec.toString() + "_" + packetSize;
		Buffer res = silentBuffers.get(key);
		if (res == null)
			synchronized (silentBuffers) {
				res = silentBuffers.get(key);
				if (res == null)
					res = createSilentBuffer(codec, packetSize, key);
			}
		return res;
	}

	public List<String> getSilentBuffersKeys()
	{
		return new ArrayList<String>(silentBuffers.keySet());
	}

	public List<BuffersCacheEntity> getCacheEntities()
	{
		return new ArrayList<BuffersCacheEntity>(buffersCache.values());
	}

	public long getMaxCacheIdleTime()
	{
		return maxCacheIdleTime.get();
	}

	public void removeOldCaches()
	{
		Iterator<Map.Entry<String, BuffersCacheEntity>> it = buffersCache.entrySet().iterator();
		while (it.hasNext())
			if (it.next().getValue().isInvalid())
				it.remove();
	}

	public void setMaxCacheIdleTime(long time)
	{
		maxCacheIdleTime.set(time);
	}

	public void cacheBuffers(String key, long checksum, Codec codec, int packetSize, Collection<Buffer> buffers)
	{
		buffersCache.put(formCacheKey(key, codec, packetSize), new CacheEntity(key, codec, packetSize, checksum, buffers));
	}

	public Buffer[] getCachedBuffers(String key, long checksum, Codec codec, int packetSize)
	{
		BuffersCacheEntity entity = buffersCache.get(formCacheKey(key, codec, packetSize));
		return entity != null && entity.getChecksum() == checksum ? entity.getBuffers() : null;
	}

	private String formCacheKey(String key, Codec codec, int packetSize)
	{
		return key + "_" + codec + "_" + packetSize;
	}

	private Buffer createSilentBuffer(Codec codec, int packetSize, String key)
	{
		try {
			TranscoderDataSource transcoder = null;
			try {
				ResourceInputStreamSource silenceSource = new ResourceInputStreamSource(SILENCE_RESOURCE_NAME);
				ContainerParserDataSource parser = new ContainerParserDataSource(codecManager, silenceSource, FileTypeDescriptor.WAVE);
				PullToPushConverterDataSource converter = new PullToPushConverterDataSource(parser);
				transcoder = new TranscoderDataSource(codecManager, converter, codec.getAudioFormat(), null);
				transcoder.connect();
				PacketSizeControl packetSizeControl = (PacketSizeControl) transcoder.getControl(PacketSizeControl.class.getName());
				if (packetSizeControl != null) {
					packetSizeControl.setPacketSize(packetSize);
				}
/*				final AtomicReference<Buffer> buf = new AtomicReference<Buffer>();
				final AtomicReference<Exception> error = new AtomicReference();
				transcoder.getStreams()[0].setTransferHandler(new BufferTransferHandler()
				{
					public void transferData(PushBufferStream stream)
					{
						if (buf.get() != null) {
							return;
						}
						
						Buffer buffer = new Buffer();
						try {
							stream.read(buffer);
							if (buffer.getData() != null && !buffer.isDiscard()) {
								buf.compareAndSet(null, buffer);							// buf will be set here
							}
						}
						catch (IOException ex) {
							error.set(ex);
							log.error("Error getting silent buffer", ex);
						}
					}
				});
				transcoder.start();
				
				while (buf.get() == null && error.get() == null) {		// checking buf has a value
					TimeUnit.MILLISECONDS.sleep(1);
				}
				
				if (error.get() != null) {
					throw error.get();
				}
				
				silentBuffers.put(key, buf.get());
				return buf.get();*/
				
				// replacing previous code by FTtoth
				Buffer buffer = new Buffer();
				silentBuffers.put(key, buffer);
				return buffer;
			}
			finally {
				if (transcoder != null) {
					transcoder.stop();
					transcoder.disconnect();
				}
			}
		}
		catch (Exception ex) {
			log.error(String.format("Error initializing silent buffer for codec (%s) and rtp packet size (%s)", codec, packetSize), ex);
			return null;
		}
	}

	private class CacheEntity implements BuffersCacheEntity
	{
		private final String key;
		private final Codec codec;
		private final int packetSize;
		private final long checksum;
		private final Buffer[] buffers;
		private final AtomicLong ts = new AtomicLong(System.currentTimeMillis());
		private final AtomicLong usageCount = new AtomicLong(0);

		public CacheEntity(String key, Codec codec, int packetSize, long checksum, Collection<Buffer> buffers)
		{
			this.key = key;
			this.codec = codec;
			this.packetSize = packetSize;
			this.checksum = checksum;
			this.buffers = new Buffer[buffers.size()];
			buffers.toArray(this.buffers);
		}

		public int getBuffersCount()
		{
			return buffers.length;
		}

		public long getChecksum()
		{
			return checksum;
		}

		public Codec getCodec()
		{
			return codec;
		}

		public long getIdleTime()
		{
			return (System.currentTimeMillis() - ts.get()) / 1000;
		}

		public String getKey()
		{
			return key;
		}

		public int getPacketSize()
		{
			return packetSize;
		}

		public boolean isInvalid()
		{
			return (System.currentTimeMillis() - ts.get()) / 1000 > maxCacheIdleTime.get();
		}

		public Buffer[] getBuffers()
		{
			ts.set(System.currentTimeMillis());
			usageCount.incrementAndGet();
			return buffers;
		}

		public long getUsageCount()
		{
			return usageCount.get();
		}
	}
}
