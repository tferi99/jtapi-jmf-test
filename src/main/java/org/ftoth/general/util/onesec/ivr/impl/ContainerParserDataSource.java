package org.ftoth.general.util.onesec.ivr.impl;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.media.Demultiplexer;
import javax.media.Time;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.DataSource;
import javax.media.protocol.PullBufferDataSource;
import javax.media.protocol.PullBufferStream;

import org.ftoth.general.util.onesec.ivr.CodecManager;
import org.ftoth.general.util.onesec.ivr.ContainerParserDataSourceException;
import org.ftoth.general.util.onesec.ivr.InputStreamSource;
import org.ftoth.general.util.onesec.ivr.IssDataSource;

public class ContainerParserDataSource extends PullBufferDataSource
{
	private final DataSource source;
	private final ContainerParserDataStream[] streams;
	private final AtomicBoolean connected = new AtomicBoolean();
	private final CodecManager codecManager;
	private Demultiplexer parser;

	public ContainerParserDataSource(CodecManager codecManager, DataSource source)
	{
		this.source = source;
		this.streams = new ContainerParserDataStream[] { new ContainerParserDataStream(this) };
		this.codecManager = codecManager;
	}

	public ContainerParserDataSource(CodecManager codecManager, InputStreamSource inputStreamSource, String contentType)
	{
		this(codecManager, new IssDataSource(inputStreamSource, contentType));
	}

	@Override
	public PullBufferStream[] getStreams()
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
		if (!connected.compareAndSet(false, true))
			return;
		source.connect();
		try {
			this.parser = codecManager.buildDemultiplexer(source.getContentType());
			if (parser == null) {
				throw new ContainerParserDataSourceException(String.format("Can't find parser for content type (%s)", source.getContentType()));
			}
			parser.setSource(source);
			streams[0].setTrack(parser.getTracks()[0]);
		}
		catch (Exception e) {
			throw new IOException(String.format("Error configuring parser (%s)", parser.getClass().getName()), e);
		}
	}

	@Override
	public void disconnect()
	{
		if (connected.compareAndSet(true, false)) {
			source.disconnect();
			parser.reset();
		}
	}

	@Override
	public void start()
			throws IOException
	{
		source.start();
		parser.start();
	}

	@Override
	public void stop()
			throws IOException
	{
		parser.stop();
		source.stop();
	}

	@Override
	public Object getControl(String arg0)
	{
		return null;
	}

	@Override
	public Object[] getControls()
	{
		return null;
	}

	@Override
	public Time getDuration()
	{
		return parser.getDuration();
	}
}