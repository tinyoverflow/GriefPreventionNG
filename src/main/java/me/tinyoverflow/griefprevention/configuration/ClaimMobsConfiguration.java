package me.tinyoverflow.griefprevention.configuration;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@ConfigSerializable
public class ClaimMobsConfiguration
{
    @Setting
    @Comment("Whether a player without permission can hurt claimed animals.")
    public boolean protectCreatures = true;

    @Setting
    @Comment("Whether horses on a claim should be protected by that claims rules.")
    public boolean protectHorses = true;

    @Setting
    @Comment("Whether donkeys on a claim should be protected by that claims rules.")
    public boolean protectDonkeys = true;

    @Setting
    @Comment("Whether llamas on a claim should be protected by that claims rules.")
    public boolean protectLlamas = true;
}
