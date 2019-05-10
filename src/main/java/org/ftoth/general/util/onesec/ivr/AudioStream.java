package org.ftoth.general.util.onesec.ivr;

import javax.media.protocol.DataSource;

public interface AudioStream
{
    public void addSource(InputStreamSource source);
    public void addSource(DataSource source);
    public void addSource(String key, long checksum, DataSource source);
    public void addSource(String key, long checksum, InputStreamSource source);
    /**
     * Returns true if audio stream has buffers that not played yet.
     */
    public boolean isPlaying();
    /**
     * Returns audio source
     */
    public DataSource getDataSource();
    /**
     * Resets the stream. This means that all audio data is not played for now will be cleared.
     */
    public void reset();
    /**
     * Closes audio source.
     */
    public void close();
}
