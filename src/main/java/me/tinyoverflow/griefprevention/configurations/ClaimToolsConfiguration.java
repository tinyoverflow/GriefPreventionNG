package me.tinyoverflow.griefprevention.configurations;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.bukkit.Material;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@ConfigSerializable
@Data
public class ClaimToolsConfiguration
{
    @Setting
    @Comment("The material to use to investigate claims.")
    private final Material investigationTool = Material.STICK;

    @Setting
    @Comment("The material to use to create and modify claims.")
    private final Material modificationTool = Material.GOLDEN_SHOVEL;
}
