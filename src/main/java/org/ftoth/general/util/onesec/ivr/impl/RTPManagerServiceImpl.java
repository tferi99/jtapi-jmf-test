package org.ftoth.general.util.onesec.ivr.impl;

import javax.annotation.PostConstruct;
import javax.media.Format;
import javax.media.rtp.RTPManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ftoth.general.util.onesec.ivr.CodecManager;
import org.ftoth.general.util.onesec.ivr.RTPManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("rtpManagerService")
public class RTPManagerServiceImpl implements RTPManagerService
{
	private static final Log log = LogFactory.getLog(RTPManagerServiceImpl.class);
	
	@Autowired
	CodecManager codecManager;
	
    private Format alawRtpFormat;
    private Format g729RtpFormat;

    public RTPManagerServiceImpl()
    {
    }

    @PostConstruct
    public void init()
    {
        this.alawRtpFormat = codecManager.getAlawRtpFormat();
        this.g729RtpFormat = codecManager.getG729RtpFormat();
    }
    
    public RTPManager createRtpManager()
    {
        if (log.isDebugEnabled()) {
            log.debug("Creating new RTPManager");
        }
        RTPManager manager = RTPManager.newInstance();
        manager.addFormat(alawRtpFormat, 8);
        manager.addFormat(g729RtpFormat, 8);

        return manager;
    }
}
