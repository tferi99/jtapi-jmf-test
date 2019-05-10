package org.ftoth.general.util.onesec.ivr.impl;

import javax.media.Codec;
import javax.media.Format;
import javax.media.ResourceUnavailableException;

import org.ftoth.general.util.onesec.ivr.CodecConfig;

public class CodecConfigImpl implements CodecConfig {
    private final Codec codec;
    private final Format outputFormat;
    private final Format inputFormat;

    public CodecConfigImpl(Codec codec, Format outputFormat, Format inputFormat) 
            throws ResourceUnavailableException 
    {
        this.codec = codec;
        this.codec.setInputFormat(inputFormat);
        this.codec.setOutputFormat(outputFormat);
        this.codec.open();
        this.outputFormat = outputFormat;
        this.inputFormat = inputFormat;
    }

    public Format getInputFormat() {
        return inputFormat;
    }

    public Codec getCodec() {
        return codec;
    }

    public Format getOutputFormat() {
        return outputFormat;
    }
}
