package org.ftoth.jtapijmftest;

import org.ftoth.general.util.onesec.CiscoJtapiTerminal;
import org.ftoth.general.util.onesec.Codec;
import org.ftoth.general.util.onesec.TestTerminal;
import org.ftoth.general.util.onesec.core.State;
import org.ftoth.general.util.onesec.core.StateListener;

public class TestOneSecMediaTerminal extends JtapiAppBase implements StateListener
{

	public static String[] appArgs;
	
	private String address;
	
	public static void main(String[] args)
	{
		appArgs = args;
		
		TestOneSecMediaTerminal app = new TestOneSecMediaTerminal();
		app.address = "9042";
		app.dumpJtapi = false;
		app.start();

	}

	@Override
	protected void action()
			throws Exception
	{
		TestTerminal t = (TestTerminal) getBean("testTerminal");
		t.config(address, Codec.G711_MU_LAW, 160, 5);
		
		// calling OneSec media terminal 
        CiscoJtapiTerminal terminal = new CiscoJtapiTerminal(getProvider(), t);
        terminal.getState().addStateListener(this);
/*        term.set(terminal);
        terminalCreated(terminal);*/
        terminal.start();
        
		while (true) {
			Thread.yield();
			sleep(100);
		}
        
	}

	@Override
	public void stateChanged(State state)
	{
		System.out.println("STATE:" + state);
	}

}
