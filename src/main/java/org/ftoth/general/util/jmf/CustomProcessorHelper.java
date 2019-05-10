package org.ftoth.general.util.jmf;

import javax.media.Processor;

public interface CustomProcessorHelper
{
	/**
	 * Initialize custom processing for tracks of processor. 
	 * 
	 * @param processor
	 * @return initialization was successfully
	 */
	boolean initProcessing(Processor processor);
	
	/**
	 * Initialize custom renderer for tracks of processor.
	 *  
	 * @param processor
	 * @return
	 */
	boolean initRenderer(Processor processor);
}
