package me.tinyoverflow.griefprevention.configuration;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public class GriefPreventionConfiguration
{
    public ClaimConfiguration claims = new ClaimConfiguration();
}
