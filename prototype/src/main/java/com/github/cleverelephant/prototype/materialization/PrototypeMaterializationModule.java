package com.github.cleverelephant.prototype.materialization;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.Module;

public class PrototypeMaterializationModule extends Module
{
    private final PrototypeMaterializer materializer;

    public PrototypeMaterializationModule()
    {
        materializer = new PrototypeMaterializer();
    }

    @Override
    public String getModuleName()
    {
        return getClass().getCanonicalName();
    }

    @Override
    public Version version()
    {
        return Version.unknownVersion();
    }

    @Override
    public void setupModule(SetupContext context)
    {
        context.addAbstractTypeResolver(materializer);
    }

}
