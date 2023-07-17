package me.tinyoverflow.griefprevention.configurations;

import org.bukkit.Material;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@ConfigSerializable
public class ClaimToolsConfiguration
{
    @Setting
    @Comment("The material to use to investigate claims.")
    public Material investigation = Material.STICK;

    @Setting
    @Comment("The material to use to create and modify claims.")
    public Material modification = Material.GOLDEN_SHOVEL;
}
