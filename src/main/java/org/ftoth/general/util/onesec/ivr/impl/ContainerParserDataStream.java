package org.ftoth.general.util.onesec.ivr.impl;

import java.io.IOException;

import javax.media.Buffer;
import javax.media.Format;
import javax.media.Track;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.PullBufferStream;


public class ContainerParserDataStream implements PullBufferStream {
    
    private Track track;
    private final ContentDescriptor contentDescriptor;
//    private final ContainerParserDataSource dataSource;
    private boolean endOfStream = false;

    public ContainerParserDataStream(ContainerParserDataSource dataSource) {
        this.contentDescriptor = new ContentDescriptor(dataSource.getContentType());
    }
    
    void setTrack(Track track) {
        this.track = track;
    }

    public boolean willReadBlock() {
        return track!=null && track.isEnabled() && !endOfStream;
    }

    public void read(Buffer buffer) throws IOException {
        track.readFrame(buffer);
        if (buffer!=null && buffer.isEOM())
            endOfStream = true;
    }
    
    public Format getFormat() {
        return track==null? contentDescriptor : track.getFormat();
    }

    public ContentDescriptor getContentDescriptor() {
        return contentDescriptor;
    }

    public long getContentLength() {
        return LENGTH_UNKNOWN;
    }

    public boolean endOfStream() {
        return endOfStream;
    }

    public Object[] getControls() {
        return null;
    }

    public Object getControl(String controlType) {
        return null;
    }
}
