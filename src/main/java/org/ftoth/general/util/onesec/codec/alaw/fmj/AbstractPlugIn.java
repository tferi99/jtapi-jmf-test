package org.ftoth.general.util.onesec.codec.alaw.fmj;

import javax.media.PlugIn;
import javax.media.ResourceUnavailableException;

/**
 * Abstract implementation of PlugIn, useful for subclassing.
 * @author Ken Larson
 *
 */
public abstract class AbstractPlugIn extends AbstractControls implements PlugIn
{

	private boolean opened = false;
	
	public void close()
	{	opened = false;
	}

	public String getName()
	{
		return this.getClass().getSimpleName();
	}

	public void open() throws ResourceUnavailableException
	{	opened = true;
	}

	public void reset()
	{	// TODO
	}
	
}
