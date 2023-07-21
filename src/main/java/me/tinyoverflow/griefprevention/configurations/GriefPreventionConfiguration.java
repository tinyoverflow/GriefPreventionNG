package me.tinyoverflow.griefprevention.configurations;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@ConfigSerializable
public class GriefPreventionConfiguration
{
    @Setting("claims")
    @Comment("Configures where and how claims should work and what to protect.")
    private ClaimConfiguration claimConfiguration = new ClaimConfiguration();

    @Setting("siege")
    @Comment("Configures where and how the /siege mode works.")
    private SiegeConfiguration siegeConfiguration = new SiegeConfiguration();

    public ClaimConfiguration getClaimConfiguration() {
        return claimConfiguration;
    }

    public SiegeConfiguration getSiegeConfiguration()
    {
        return siegeConfiguration;
    }
}
