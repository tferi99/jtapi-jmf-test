package org.ftoth.general.util.onesec.ivr;

import org.ftoth.general.util.onesec.core.State;

public interface IvrEndpointConversationState 
    extends State<IvrEndpointConversationState, IvrEndpointConversation>
{
    public final static int INVALID = 1;
    public final static int READY = 2;
    public final static int CONNECTING = 3;
    public final static int TALKING = 4;
    public final static int TRANSFERING = 5;
}
