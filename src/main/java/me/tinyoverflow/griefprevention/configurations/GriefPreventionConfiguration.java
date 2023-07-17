package me.tinyoverflow.griefprevention.configurations;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public class GriefPreventionConfiguration
{
    public ClaimConfiguration claims = new ClaimConfiguration();
}
