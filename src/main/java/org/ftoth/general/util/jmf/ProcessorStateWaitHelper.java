package org.ftoth.general.util.jmf;

import javax.media.ControllerClosedEvent;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.Processor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * It performs a state-related action and waits until specific processor state.
 * 
 * Supported states:
 * 		- Processor.Configured
 * 		- Processor.Realized
 * 
 * @author ftoth
 *
 */
public class ProcessorStateWaitHelper
{
	private static final Log log = LogFactory.getLog(ProcessorStateWaitHelper.class);	
	
	private boolean failed = false;
	
	// ------------------------- properties -------------------------
	private Integer stateLock = 1;
	
	public Object getStateLock()
	{
		return stateLock;
	}

	// ------------------------- action -------------------------
	public synchronized boolean waitForState(Processor p, int state)
	{
		if (log.isDebugEnabled()) {
			log.debug("Waiting for " + JmfUtil.getControllerStateName(state) + "...");
		}
		
		StateListener sl = new StateListener();
		p.addControllerListener(sl);
		failed = false;

		// Call the required method on the processor
		if (state == Processor.Configured) {
			p.configure();
		}
		else if (state == Processor.Realized) {
			p.realize();
		}

		// Wait until we get an event that confirms the
		// success of the method, or a failure event.
		// See StateListener inner class
		while (p.getState() < state && !failed) {
			synchronized (getStateLock()) {
				try {
					getStateLock().wait();
				}
				catch (InterruptedException ie) {
					return false;
				}
			}
		}
		p.removeControllerListener(sl);
		return !failed;
	}
	
	
	class StateListener implements ControllerListener
	{

		public void controllerUpdate(ControllerEvent ce)
		{
			// If there was an error during configure or
			// realize, the processor will be closed
			if (ce instanceof ControllerClosedEvent) {
				failed = true;
				if (log.isDebugEnabled()) {
					log.debug("Error during wait");
				}
			}
			
			// All controller events, send a notification
			// to the waiting thread in waitForState method.
			if (ce instanceof ControllerEvent) {
				synchronized (getStateLock()) {
					getStateLock().notifyAll();
					if (log.isDebugEnabled()) {
						log.debug("End of wait.");
					}
				}
			}
		}
	}
	
}
