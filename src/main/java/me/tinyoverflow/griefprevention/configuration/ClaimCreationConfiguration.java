package me.tinyoverflow.griefprevention.configuration;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@ConfigSerializable
public class ClaimCreationConfiguration
{
    @Setting
    @Comment("Whether the player needs WorldGuard build permissions to create a claim inside the region.")
    public boolean requireWorldGuard = true;

    @Setting
    @Comment("The maximum amount of claims a single player can have.")
    public int maximumClaims = 5;

    @Setting
    @Comment("The minimum width a claim must have to be created.")
    public int minimumWidth = 5;

    @Setting
    @Comment("The minimum area a claim must have to be created.")
    public int minimumArea = 100;

    @Setting
    @Comment("The limit on how deep a claim can go. Claims cannot extend below this height.")
    public int maximumDepth = -64;

    @Setting
    @Comment("How far a newly created claim should go into the ground.")
    public int extendIntoGroundDistance = 5;

    @Setting
    @Comment("The preferred radius for an automatically created claim (eg. by a chest).")
    public int automaticPreferredRadius = 4;

    @Setting
    @Comment("A claim would not be automatically be created if it cannot have the minimum radius.")
    public int automaticMinimumRadius = 0;
}
