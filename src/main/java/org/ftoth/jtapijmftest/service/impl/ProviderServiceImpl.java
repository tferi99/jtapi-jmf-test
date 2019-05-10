package org.ftoth.jtapijmftest.service.impl;

import java.util.ArrayList;
import java.util.List;

import javax.telephony.JtapiPeer;
import javax.telephony.JtapiPeerFactory;
import javax.telephony.JtapiPeerUnavailableException;
import javax.telephony.MethodNotSupportedException;
import javax.telephony.Provider;
import javax.telephony.ProviderObserver;
import javax.telephony.ResourceUnavailableException;
import javax.telephony.events.ProvEv;
import javax.telephony.events.ProvInServiceEv;
import javax.telephony.events.ProvShutdownEv;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ftoth.general.util.jtapi.ProviderUtil;
import org.ftoth.jtapijmftest.exception.JtapiRuntimeException;
import org.ftoth.jtapijmftest.listener.ProviderShutdownListener;
import org.ftoth.jtapijmftest.service.ProviderService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.cisco.cti.util.Condition;

/**
 * <p>Description: </p>
 *
 * @author ftoth
 */
@Component("providerService")
public class ProviderServiceImpl implements ProviderService, ProviderObserver
{
    private static final Log log = LogFactory.getLog(ProviderServiceImpl.class);

    Condition conditionInService = new Condition();
    private List<ProviderShutdownListener> providerShutdownListeners = new ArrayList<ProviderShutdownListener>();
    
    //----------------------- properties -----------------------
    @Value("${communicationManager.host}")
    private String host;

    @Value("${communicationManager.user}")
    private String user;

    @Value("${communicationManager.password}")
    private String password;

    @Value("${communicationManager.appInfo}")
    private String appInfo;
    
    private Provider provider;

    public String getHost()
    {
        return host;
    }

    public void setHost(String host)
    {
        this.host = host;
    }

    public String getUser()
    {
        return user;
    }

    public void setUser(String user)
    {
        this.user = user;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

    
    public String getAppInfo()
	{
		return appInfo;
	}

	public void setAppInfo(String appInfo)
	{
		this.appInfo = appInfo;
	}

	//----------------------- interface -----------------------
    @Override
    public Provider getProvider()
    {
        if (provider == null) {
            try {
				provider = loadProvider();
			}
			catch (Exception e) {
				throw new JtapiRuntimeException("Error during getting privider", e);				
			}
        }
        return provider;
    }

	@Override
	public void addProviderShutdownListener(ProviderShutdownListener psl)
	{
		providerShutdownListeners.add(psl);		
	}
    
    //----------------------- observer -----------------------
    @Override
    public void providerChangedEvent(ProvEv[] eventList)
    {
        if (eventList != null) {
            for (ProvEv ev : eventList) {
            	Provider prov = ev.getProvider();
				if (log.isTraceEnabled()) {
					log.trace("ProviderObserver EVENT [" +  ev.getID() + "] - " + ev + ", Call(" + ProviderUtil.getStateName(prov.getState()) + ")");
				}
            	
                if (ev instanceof ProvInServiceEv) {
                    conditionInService.set();
                }
                else if (ev instanceof ProvShutdownEv) {
                	// notifying about shutdown completed
                	for (ProviderShutdownListener l : providerShutdownListeners) {
                		l.providerShutdownCompleted();
                	}
                }
            }
        }
    }

    //----------------------- helpers -----------------------
    private Provider loadProvider() throws JtapiPeerUnavailableException, MethodNotSupportedException, ResourceUnavailableException
    {
        if (log.isDebugEnabled()) {
            log.debug("Getting provider from [" + host + "] for [" + user + "]");
        }
        String providerString = host + ";login=" + user + ";passwd=" + password;
        JtapiPeer peer = JtapiPeerFactory.getJtapiPeer(null);
        Provider p = peer.getProvider(providerString);

        // wait for service in
        p.addObserver(this);
        conditionInService.waitTrue();

        if (log.isDebugEnabled()) {
            log.debug("------------------- Provider is ready to use ----------------------");
        }
        return p;
    }


}
