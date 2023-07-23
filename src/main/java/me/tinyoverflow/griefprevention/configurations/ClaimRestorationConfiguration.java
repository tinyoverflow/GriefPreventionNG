package me.tinyoverflow.griefprevention.configurations;

import lombok.Data;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@ConfigSerializable
@Data
public class ClaimRestorationConfiguration
{
    @Setting("enabled")
    @Comment("Whether to restore nature when a claim gets removed.")
    private final boolean enabled = false;
}
