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
    private Material investigation = Material.STICK;

    @Setting
    @Comment("The material to use to create and modify claims.")
    private Material modification = Material.GOLDEN_SHOVEL;

    public Material getInvestigationTool()
    {
        return this.investigation;
    }

    public Material getModificationTool()
    {
        return this.modification;
    }
}
