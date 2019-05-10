package org.ftoth.general.util.onesec.ivr;

/**
 * 
 * @author Mikhail Titov
 */
public class IvrEndpointConversationException extends Exception
{

	private static final long serialVersionUID = 5085651495012782025L;

	public IvrEndpointConversationException(String msg)
	{
		super(msg);
	}

	public IvrEndpointConversationException(String message, Throwable cause)
	{
		super(message, cause);
	}
}
