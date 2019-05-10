package org.ftoth.general.util.onesec.ivr;

import java.util.Collection;
import java.util.List;

import javax.media.Buffer;

import org.ftoth.general.util.onesec.Codec;

public interface BufferCache
{
    /**
     * Returns the silent buffer for passed parameters
     * @param codec codec
     * @param packetSize of silent buffer
     */
    public Buffer getSilentBuffer(Codec codec, int packetSize);
    /**
     * Returns cached buffers or null if cache does not contains buffers for specified parameters
     * @param key the key of the cache
     * @param checksum checksum of the cache content
     * @param codec codec of the buffers
     * @param packetSize the packet size of buffers
     * @return
     */
    public Buffer[] getCachedBuffers(String key, long checksum, Codec codec, int packetSize);
    /**
     * Caches buffers
     * @param key the key of the cache
     * @param checksum checksum of the cache content
     * @param codec codec of the buffers
     * @param packetSize the packet size of buffers
     * @param buffers buffers that must be cached
     */
    public void cacheBuffers(String key, long checksum, Codec codec, int packetSize, Collection<Buffer> buffers);
    /**
     * Removes caches which idle time are more then {@link #getMaxCacheIdleTime()}
     */
    public void removeOldCaches();
    /**
     * Sets the max idle time of the cache in seconds
     */
    public void setMaxCacheIdleTime(long time);
    /**
     * Returns the max idle time of the cache in seconds
     */
    public long getMaxCacheIdleTime();

    public List<BuffersCacheEntity> getCacheEntities();

    public List<String> getSilentBuffersKeys();
}
