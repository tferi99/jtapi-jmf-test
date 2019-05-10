package org.ftoth.jtapijmftest.service;

import javax.telephony.Provider;

import org.ftoth.jtapijmftest.listener.ProviderShutdownListener;

/**
 * <p>Description: </p>
 *
 * @author ftoth
 */
public interface ProviderService
{
    String getHost();
    String getUser();
    Provider getProvider();
    void addProviderShutdownListener(ProviderShutdownListener psl);
}
