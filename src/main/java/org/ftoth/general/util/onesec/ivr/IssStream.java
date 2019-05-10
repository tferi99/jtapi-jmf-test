package org.ftoth.general.util.onesec.ivr;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.PullSourceStream;
import javax.media.protocol.Seekable;

/**
 *
 * @author Mikhail Titov
 */
public class IssStream implements PullSourceStream, Seekable
{
    private ByteBuffer inputBuffer;

    public void setInputBuffer(ByteBuffer inputBuffer)
    {
        this.inputBuffer = inputBuffer;
    }

    public boolean endOfStream()
    {
        return !inputBuffer.hasRemaining();
    }
    
    public void reset() {
        inputBuffer.position(0);
    }

    public ContentDescriptor getContentDescriptor()
    {
        return null;
    }

    public long getContentLength()
    {
        return inputBuffer.capacity();
    }

    public Object getControl(String controlType)
    {
        return null;
    }

    public Object[] getControls() {
        return new Object[0];
    }

    public boolean isRandomAccess() {
        return true;
    }

    public int read(byte[] buffer, int offset, int length) throws IOException {
        if (length == 0)
            return 0;
        try {
            inputBuffer.get(buffer, offset, length);
            return length;
        } catch (BufferUnderflowException E) {
            return -1;
        }
    }

    public void close() {
//        inputBuffer = null;
    }

    public long seek(long where) {
        try {
            inputBuffer.position((int) (where));
            return where;
        } catch (IllegalArgumentException E) {
            return tell();
        }
    }

    public long tell() {
        return inputBuffer.position();
    }

    public boolean willReadBlock() {
        return (inputBuffer.remaining() == 0);
    }
}