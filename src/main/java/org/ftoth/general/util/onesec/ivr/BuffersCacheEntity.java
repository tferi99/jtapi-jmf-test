package org.ftoth.general.util.onesec.ivr;

import javax.media.Buffer;

import org.ftoth.general.util.onesec.Codec;

public interface BuffersCacheEntity {
    public String getKey();
    public Codec getCodec();
    public long getChecksum();
    public int getPacketSize();
    public int getBuffersCount();
    public long getIdleTime();
    public long getUsageCount();
    public boolean isInvalid();
    public Buffer[] getBuffers();
}
