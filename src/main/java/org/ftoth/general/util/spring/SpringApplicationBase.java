package org.ftoth.general.util.spring;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class SpringApplicationBase
{
	private final static Log log = LogFactory.getLog(SpringApplicationBase.class);

	protected ClassPathXmlApplicationContext appContext;

	// ----------------- startup -----------------
	public SpringApplicationBase(String configFile)
	{
		init(configFile);
	}
	
	// ----------------- utils -----------------
	protected Object getBean(String name)
	{
		return appContext.getBean(name);
	}
	
	// ----------------- helpers  -----------------
	void init(String configFile)
	{
		appContext = new ClassPathXmlApplicationContext(configFile);
		
		if (log.isDebugEnabled()) {
			log.debug("SPRING BEANS:");
			System.out.println(SpringContextDump.dumpSpringContext(appContext));
		}
	}
}
