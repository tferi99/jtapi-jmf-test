package org.ftoth.general.util.onesec.ivr.impl;

import java.io.InputStream;

import org.ftoth.general.util.onesec.ivr.InputStreamSource;

public class ResourceInputStreamSource implements InputStreamSource
{
    private final String resourceName;

    public ResourceInputStreamSource(String resourceName)
    {
        this.resourceName = resourceName;
    }

    public InputStream getInputStream()
    {
        return this.getClass().getResourceAsStream(resourceName);
    }
}
