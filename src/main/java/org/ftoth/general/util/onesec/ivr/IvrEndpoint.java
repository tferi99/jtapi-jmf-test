package org.ftoth.general.util.onesec.ivr;


public interface IvrEndpoint extends IvrTerminal
{
    public IvrEndpointState getEndpointState();

/*    public void invite(String opponentNum, int inviteTimeout, int maxCallDur
            , IvrEndpointConversationListener listener
            , IvrConversationScenario scenario, Map<String, Object> bindings);*/
//    public RtpAddress getRtpAddress();
    /**
     * Adds conversation listener
     */
    //public void addConversationListener(IvrEndpointConversationListener listener);
    /**
     * Removes conversation listener
     */
    //public void removeConversationListener(IvrEndpointConversationListener listener);
    /**
     * Returns active calls count for the terminal
     */
    public int getActiveCallsCount();
}