package me.tinyoverflow.griefprevention.configurations;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@ConfigSerializable
public class GriefPreventionConfiguration
{
    @Setting("claims")
    private ClaimConfiguration claimConfiguration = new ClaimConfiguration();

    @Setting("siege")
    private SiegeConfiguration siegeConfiguration = new SiegeConfiguration();

    public ClaimConfiguration getClaimConfiguration() {
        return claimConfiguration;
    }

    public SiegeConfiguration getSiegeConfiguration()
    {
        return siegeConfiguration;
    }
}
