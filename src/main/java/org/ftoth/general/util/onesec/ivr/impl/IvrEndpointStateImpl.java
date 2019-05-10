package org.ftoth.general.util.onesec.ivr.impl;

import org.ftoth.general.util.onesec.core.impl.BaseState;
import org.ftoth.general.util.onesec.ivr.IvrEndpoint;
import org.ftoth.general.util.onesec.ivr.IvrEndpointState;


public class IvrEndpointStateImpl 
        extends BaseState<IvrEndpointState, IvrEndpoint>
        implements IvrEndpointState
{
    public IvrEndpointStateImpl(IvrEndpoint observableObject)
    {
        super(observableObject);
        addIdName(OUT_OF_SERVICE, "OUT_OF_SERVICE");
        addIdName(IN_SERVICE, "IN_SERVICE");
        addIdName(ACCEPTING_CALL, "ACCEPTING_CALL");
        addIdName(INVITING, "INVITING");
        addIdName(TALKING, "TALKING");
    }
}
