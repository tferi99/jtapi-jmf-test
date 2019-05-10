package org.ftoth.general.util.onesec.ivr.impl;

import org.ftoth.general.util.onesec.core.impl.BaseState;
import org.ftoth.general.util.onesec.ivr.IvrTerminal;
import org.ftoth.general.util.onesec.ivr.IvrTerminalState;

public class IvrTerminalStateImpl
        extends BaseState<IvrTerminalState, IvrTerminal>
        implements IvrTerminalState
{
    public IvrTerminalStateImpl(IvrTerminal observableObject) {
        super(observableObject);
        addIdName(OUT_OF_SERVICE, "OUT_OF_SERVICE");
        addIdName(IN_SERVICE, "IN_SERVICE");
    }
}
