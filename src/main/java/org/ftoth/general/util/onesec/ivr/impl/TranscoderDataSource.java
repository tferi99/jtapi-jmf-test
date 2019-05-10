package org.ftoth.general.util.onesec.ivr.impl;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.media.Time;
import javax.media.format.AudioFormat;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ftoth.general.util.onesec.ivr.CodecConfig;
import org.ftoth.general.util.onesec.ivr.CodecManager;
import org.ftoth.general.util.onesec.ivr.CodecManagerException;

public class TranscoderDataSource extends PushBufferDataSource
{
	private static final Log log = LogFactory.getLog(TranscoderDataSource.class);

	private final PushBufferDataSource source;
	private final AudioFormat outputFormat;
	private final TranscoderDataStream[] streams;
	private final AtomicBoolean connected = new AtomicBoolean();
	private final CodecManager codecManager;
	private final PushBufferStream sourceStream;
	private final String logPrefix;

	public TranscoderDataSource(CodecManager codecManager, PushBufferDataSource source, AudioFormat outputFormat, String logPrefix) throws CodecManagerException
	{
		this.logPrefix = logPrefix;
		this.source = source;
		this.outputFormat = outputFormat;
		this.codecManager = codecManager;
		this.sourceStream = source.getStreams()[0];
		this.streams = new TranscoderDataStream[] { new TranscoderDataStream(sourceStream) };
	}

	@Override
	public PushBufferStream[] getStreams()
	{
		return streams;
	}

	@Override
	public String getContentType()
	{
		return ContentDescriptor.RAW;
	}

	@Override
	public void connect()
			throws IOException
	{
		if (connected.compareAndSet(false, true)) {
			source.connect();
			try {
				if (log.isDebugEnabled()) {
					log.debug(logMess("Initializing "));
					log.debug(logMess("Input format: " + sourceStream.getFormat()));
					log.debug(logMess("Output format: " + outputFormat));
					log.debug(logMess("Codec chain"));
				}
				CodecConfig[] codecChain = codecManager.buildCodecChain((AudioFormat) sourceStream.getFormat(), outputFormat);
				if (log.isDebugEnabled()) {
					for (int i = 0; i < codecChain.length; i++) {
						log.debug(logMess("[%s] Codec: %s", i, codecChain[i].getCodec()));
						log.debug(logMess("    in : %s", i, codecChain[i].getInputFormat()));
						log.debug(logMess("    out: %s", i, codecChain[i].getOutputFormat()));
					}
				}
				streams[0].init(codecChain, outputFormat);
			}
			catch (CodecManagerException ex) {
				log.error(logMess("Transcoder initializing error"), ex);
				throw new IOException(ex);
			}
		}
	}

	@Override
	public void disconnect()
	{
		source.disconnect();
	}

	@Override
	public void start()
			throws IOException
	{
		source.start();
	}

	@Override
	public void stop()
			throws IOException
	{
		source.stop();
	}

	@Override
	public Object getControl(String controlName)
	{
		return streams[0].getControl(controlName);
	}

	@Override
	public Object[] getControls()
	{
		return streams[0].getControls();
	}

	@Override
	public Time getDuration()
	{
		return DURATION_UNKNOWN;
	}

	String logMess(String mess, Object... args)
	{
		return (logPrefix == null ? "" : logPrefix) + "Transcoder. " + String.format(mess, args);
	}
}
