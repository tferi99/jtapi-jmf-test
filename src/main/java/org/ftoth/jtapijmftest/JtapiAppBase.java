package org.ftoth.jtapijmftest;

import javax.telephony.Provider;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ftoth.general.util.jtapi.JtapiInfo;
import org.ftoth.general.util.spring.SpringApplicationBase;
import org.ftoth.jtapijmftest.listener.ProviderShutdownListener;
import org.ftoth.jtapijmftest.service.ProviderService;

import com.cisco.cti.util.Condition;

public abstract class JtapiAppBase extends SpringApplicationBase implements ProviderShutdownListener
{
	private static final Log log = LogFactory.getLog(JtapiAppBase.class);

	private static final String CONFIG = "applicationContext.xml";
	
	protected ProviderService providerService;
	private Condition endOfAction;
	protected boolean dumpJtapi = true;
	// ------------------------- properties -------------------------
	private Provider provider;
	
	public Provider getProvider()
	{
		return provider;
	}

	// ------------------------- startup -------------------------
	public JtapiAppBase()
	{
		super(CONFIG);
	}
	
	protected void start()
	{
		try {
			System.out.println("====================================== START ================================================");			
			init();
			
			System.out.println("====================================== ACTION ================================================");
			action();
			
			System.out.println("====================================== CLEANUP ================================================");
			cleanup();
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	protected void init() throws Exception
	{
		providerService = (ProviderService) getBean("providerService");
		endOfAction = new Condition();
		provider = providerService.getProvider();
		
		if (dumpJtapi) {
			String pinfo = JtapiInfo.dumpProvider(provider);
			System.out.println("########################## provider dump ###############################");
			System.out.println(pinfo);
			System.out.println("########################################################################");
		}
		
		providerService.addProviderShutdownListener(this);
	}
	
	protected abstract void action() throws Exception;
	
	protected void waitOnEndOfAction()
	{
		// end of action - waiting before cleanup
		endOfAction.waitTrue();
	}
	
	protected void cleanup()
	{
		if (log.isDebugEnabled()) {
			log.debug("Shutting down provider");
		}
		providerService.getProvider().shutdown();
	}
	
	protected void fireEndOfAction()
	{
		endOfAction.set();		
	}
	
	// ------------------------- helpers -------------------------
	protected void sleep(int msecs)
	{
		try {
			Thread.sleep(msecs);
		}
		catch (InterruptedException e) {
		}
	}
	
	// ------------------------- implements -------------------------
	public void providerShutdownCompleted()
	{
		System.out.println("====================================== END ================================================");
		System.exit(0);
	}
}
