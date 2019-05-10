package org.ftoth.general.util.onesec.ivr;

import javax.media.Format;

public interface CodecConfig {
    public javax.media.Codec getCodec();
    public Format getOutputFormat();
    public Format getInputFormat();
}
