package org.ftoth.jtapijmftest.exception;

public class JtapiRuntimeException extends RuntimeException
{
	private static final long serialVersionUID = -8826665576838813784L;

	public JtapiRuntimeException(String msg)
	{
		super(msg);
	}

	public JtapiRuntimeException(String msg, Throwable cause)
	{
		super(msg, cause);
	}
	
	public JtapiRuntimeException(Throwable cause)
	{
		super(cause);
	}
}
