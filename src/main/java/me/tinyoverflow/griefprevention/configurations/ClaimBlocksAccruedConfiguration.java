package me.tinyoverflow.griefprevention.configurations;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@ConfigSerializable
public class ClaimBlocksAccruedConfiguration
{
    @Setting
    @Comment("The maximum number of claim blocks a player can gain through time.")
    public int limit = 80000;

    @Setting
    @Comment("The amount of claim blocks a player receives every hour.")
    public int accruePerHour = 100;

    @Setting
    @Comment("If the player is considered idle, it will gain the amount of claim blocks per hour multiplied with this ratio.")
    public double idleRatio = 0.5d;

    @Setting
    @Comment("The distance in blocks a player has to move between payouts to be considered active.")
    public int idleDistanceThreshold = 0;
}
