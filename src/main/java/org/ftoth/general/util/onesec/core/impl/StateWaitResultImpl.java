package org.ftoth.general.util.onesec.core.impl;

import org.ftoth.general.util.onesec.core.State;
import org.ftoth.general.util.onesec.core.StateWaitResult;


/**
 *
 * @author Mikhail Titov
 */
public class StateWaitResultImpl<T extends State> implements StateWaitResult<T> 
{
    public final static StateWaitResult WAIT_INTERRUPTED = new StateWaitResultImpl(true, null);
    
    private boolean waitInterrupted;
    private T state;

    public StateWaitResultImpl(boolean waitInterrupted, T state) {
        this.waitInterrupted = waitInterrupted;
        this.state = state;
    }

    public boolean isWaitInterrupted() {
        return waitInterrupted;
    }
    
    public T getState() {
        return state;
    }

}
