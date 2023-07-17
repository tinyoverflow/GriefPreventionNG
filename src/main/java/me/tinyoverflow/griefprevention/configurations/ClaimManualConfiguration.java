package me.tinyoverflow.griefprevention.configurations;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@ConfigSerializable
public class ClaimManualConfiguration
{
    @Setting
    @Comment("Whether the manual book should be available.")
    public boolean enabled = true;

    @Setting
    @Comment("The delay in seconds after which a new player will receive the book.")
    public int delay = 30;
}
