
package org.ftoth.general.util.onesec.core;

public interface StateListener<T extends State> {

    public void stateChanged(T state);
}
