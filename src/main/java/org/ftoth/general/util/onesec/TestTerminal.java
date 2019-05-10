package org.ftoth.general.util.onesec;

import org.ftoth.general.util.onesec.ivr.IvrTerminal;
import org.ftoth.general.util.onesec.ivr.RtpStreamManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("testTerminal")
public class TestTerminal implements IvrTerminal
{
	// ------------------------- properties -------------------------
	private String address;
	Codec codec;
	private int rtpMaxSendAheadPacketsCount;
	private int rtpPacketSize;
	
	@Autowired
	private RtpStreamManager rtpStreamManager;

	public TestTerminal() 
	{
		
	}
	
	public void config(String address, Codec codec, int rtpPacketSize, int rtpMaxSendAheadPacketsCount)
	{
		this.address = address;
		this.codec = codec;
		this.rtpPacketSize = rtpPacketSize;
		this.rtpMaxSendAheadPacketsCount = rtpMaxSendAheadPacketsCount;
	}
	
	
	@Override
	public String getObjectName()
	{
		return "testTerminal";
	}

	@Override
	public String getObjectDescription()
	{
		return "This is a test terminal";
	}

	@Override
	public String getAddress()
	{
		return address;
	}

	@Override
	public RtpStreamManager getRtpStreamManager()
	{
		return rtpStreamManager;
	}


	@Override
	public Codec getCodec()
	{
		return codec;
	}

	@Override
	public Integer getRtpPacketSize()
	{
		return rtpPacketSize;
	}

	@Override
	public Integer getRtpMaxSendAheadPacketsCount()
	{
		return rtpMaxSendAheadPacketsCount;
	}

	@Override
	public Boolean getEnableIncomingRtp()
	{
		return true;
	}

	@Override
	public Boolean getEnableIncomingCalls()
	{
		return true;
	}
}
