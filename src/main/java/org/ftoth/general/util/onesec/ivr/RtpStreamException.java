package org.ftoth.general.util.onesec.ivr;

public class RtpStreamException extends Exception
{
	private static final long serialVersionUID = -6765956871398768659L;

	public RtpStreamException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public RtpStreamException(String message)
    {
        super(message);
    }
}
