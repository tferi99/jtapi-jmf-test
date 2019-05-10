package org.ftoth.general.util.onesec.ivr;

import org.ftoth.general.util.onesec.core.State;

public interface IvrTerminalState extends State<IvrTerminalState, IvrTerminal>
{
	public final static int OUT_OF_SERVICE = 1;
	public final static int IN_SERVICE = 2;
}
