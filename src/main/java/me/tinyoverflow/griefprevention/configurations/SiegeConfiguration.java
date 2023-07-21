package me.tinyoverflow.griefprevention.configurations;

import org.bukkit.Material;
import org.bukkit.World;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.ArrayList;
import java.util.List;

@ConfigSerializable
public class SiegeConfiguration
{
    @Setting
    @Comment("A list of worlds in which /siege is enabled.")
    private List<World> enabledWorlds = new ArrayList<>();

    @Setting
    @Comment("A list of block types that can be broken down while in Siege mode.")
    private List<Material> breakableBlocks = new ArrayList<>();

    @Setting
    @Comment("The amount of seconds the doors are accessible after winning the siege.")
    private int doorsOpenDelay = 300;

    @Setting
    @Comment("The amount of minutes after which the Siege mode ends.")
    private int cooldownEnd = 60;

    public List<World> getEnabledWorlds()
    {
        return enabledWorlds;
    }

    public List<Material> getBreakableBlocks()
    {
        return breakableBlocks;
    }

    public int getDoorsOpenDelay()
    {
        return doorsOpenDelay;
    }

    public int getCooldownEnd()
    {
        return cooldownEnd;
    }
}
