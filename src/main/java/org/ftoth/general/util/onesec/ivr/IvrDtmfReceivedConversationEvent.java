package org.ftoth.general.util.onesec.ivr;

public interface IvrDtmfReceivedConversationEvent extends IvrEndpointConversationEvent {
    /**
     * Returns received DTMF
     */
    public char getDtmf();
}
