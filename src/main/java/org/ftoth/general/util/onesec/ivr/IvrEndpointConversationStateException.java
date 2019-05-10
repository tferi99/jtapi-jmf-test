
package org.ftoth.general.util.onesec.ivr;

/**
 *
 * @author Mikhail Titov
 */
public class IvrEndpointConversationStateException extends IvrEndpointConversationException {

	private static final long serialVersionUID = 4898096117691149869L;

	public IvrEndpointConversationStateException(String mess, String expectedStates, String currentState)
    {
        super(String.format("%s. Invalid conversation STATE. Expected one of (%s) but was (%s)"
                , mess, expectedStates, currentState));
    }
}
