package org.ftoth.general.util.onesec.ivr.impl;

import javax.media.protocol.DataSource;
import javax.media.protocol.FileTypeDescriptor;

import org.ftoth.general.util.onesec.ivr.AudioStream;
import org.ftoth.general.util.onesec.ivr.CodecManager;
import org.ftoth.general.util.onesec.ivr.InputStreamSource;

public class TestAudioStream implements AudioStream
{
	private CodecManager codecManager;
	String resourcePath;
	
	DataSource ds;
	
	public TestAudioStream(CodecManager codecManager, String resourcePath, int packetSize)
	{
		this.codecManager = codecManager;
		this.resourcePath = resourcePath;
	}
	
	
	@Override
	public void addSource(InputStreamSource source)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addSource(DataSource source)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addSource(String key, long checksum, DataSource source)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addSource(String key, long checksum, InputStreamSource source)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isPlaying()
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public DataSource getDataSource()
	{
		if (ds == null) {
			ds = createDataSource();
		}
		return ds;
	}

	@Override
	public void reset()
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void close()
	{
		// TODO Auto-generated method stub
		
	}
	
	private DataSource createDataSource()
	{
		if (ds == null) {
			ResourceInputStreamSource silenceSource = new ResourceInputStreamSource(resourcePath);
			ContainerParserDataSource cpds = new ContainerParserDataSource(codecManager, silenceSource, FileTypeDescriptor.WAVE);
			
			PullToPushConverterDataSource converter = new PullToPushConverterDataSource(cpds);			
//			PushBufferDataSource pbds = (PushBufferDataSource) dataOutput;
		}
		return ds;
	}

	
}
