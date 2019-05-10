package org.ftoth.general.util.onesec.ivr.impl;

import java.net.InetAddress;

import org.ftoth.general.util.onesec.ivr.RtpAddress;


public class RtpAddressImpl implements RtpAddress
{
    private final InetAddress address;
    private final int port;

    public RtpAddressImpl(InetAddress address, int port) {
        this.address = address;
        this.port = port;
    }

    public InetAddress getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }
}
