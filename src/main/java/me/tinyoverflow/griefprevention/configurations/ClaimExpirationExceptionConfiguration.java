package me.tinyoverflow.griefprevention.configurations;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@ConfigSerializable
public class ClaimExpirationExceptionConfiguration
{
    @Setting
    @Comment("Do not remove claim if the player has this amount of accrued claim blocks.")
    public int claimBlocks = 10000;

    @Setting
    @Comment("Do not remove claim if the player has this amount of bonus blocks.")
    public int bonusBlocks = 5000;
}
