package me.tinyoverflow.griefprevention.configurations;

import lombok.Data;
import lombok.Getter;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@ConfigSerializable
@Data
public class GriefPreventionConfiguration
{
    @Setting("claims")
    @Comment("Configures where and how claims should work and what to protect.")
    private final ClaimConfiguration claimConfiguration = new ClaimConfiguration();

    @Setting("siege")
    @Comment("Configures where and how the /siege mode works.")
    private final SiegeConfiguration siegeConfiguration = new SiegeConfiguration();
}
