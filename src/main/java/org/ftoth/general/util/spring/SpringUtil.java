package org.ftoth.general.util.spring;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.context.ApplicationContext;

public class SpringUtil
{
	public static String dumpSpringBeans(ApplicationContext springAppContext)
	{
		StringBuilder b = new StringBuilder();
		String[] beansArr = springAppContext.getBeanDefinitionNames();
		List<String> beans = Arrays.asList(beansArr);
		Collections.sort(beans);
		b.append("======================== Spring Beans =======================\n");
		for(String bean : beans) {
			Object o = springAppContext.getBean(bean);
			b.append(" - [" + bean + "] - (" + o.getClass().getName() + ")\n");
		}
		b.append("=============================================================\n");
		return b.toString();
	}
	
	
}
