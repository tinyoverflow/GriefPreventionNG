package me.tinyoverflow.griefprevention.configurations;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@ConfigSerializable
public class ClaimBlocksConfiguration
{
    @Setting
    @Comment("The amount of claim blocks a new player starts with.")
    public int initial = 100;

    @Setting
    @Comment("The ratio of claim blocks the player will get refunded if they abandon a claim.")
    public double abandonReturnRatio = 0.75d;

    @Setting
    public ClaimBlocksAccruedConfiguration accrued = new ClaimBlocksAccruedConfiguration();
}
