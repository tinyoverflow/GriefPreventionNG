package me.tinyoverflow.griefprevention.configurations;

import org.bukkit.Material;
import org.bukkit.World;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.List;

@ConfigSerializable
public class SiegeConfiguration
{
    @Setting
    @Comment("A list of worlds in which /siege is enabled.")
    private List<String> enabledWorlds = List.of("world");

    @Setting
    @Comment("A list of block types that can be broken down while in Siege mode.")
    private List<Material> breakableBlocks = List.of(
            Material.DIRT,
            Material.GRASS_BLOCK,
            Material.GRASS,
            Material.FERN,
            Material.DEAD_BUSH,
            Material.COBBLESTONE,
            Material.GRAVEL,
            Material.SAND,
            Material.GLASS,
            Material.GLASS_PANE,
            Material.OAK_PLANKS,
            Material.SPRUCE_PLANKS,
            Material.BIRCH_PLANKS,
            Material.JUNGLE_PLANKS,
            Material.ACACIA_PLANKS,
            Material.DARK_OAK_PLANKS,
            Material.WHITE_WOOL,
            Material.ORANGE_WOOL,
            Material.MAGENTA_WOOL,
            Material.LIGHT_BLUE_WOOL,
            Material.YELLOW_WOOL,
            Material.LIME_WOOL,
            Material.PINK_WOOL,
            Material.GRAY_WOOL,
            Material.LIGHT_GRAY_WOOL,
            Material.CYAN_WOOL,
            Material.PURPLE_WOOL,
            Material.BLUE_WOOL,
            Material.BROWN_WOOL,
            Material.GREEN_WOOL,
            Material.RED_WOOL,
            Material.BLACK_WOOL,
            Material.SNOW
    );

    @Setting
    @Comment("The amount of seconds the doors are accessible after winning the siege.")
    private int doorsOpenDelay = 300;

    @Setting
    @Comment("The amount of minutes after which the Siege mode ends.")
    private int cooldownEnd = 60;

    public List<String> getEnabledWorlds()
    {
        return enabledWorlds;
    }

    public boolean isEnabledForWorld(World world) {
        return enabledWorlds.contains(world.getName());
    }

    public List<Material> getBreakableBlocks()
    {
        return breakableBlocks;
    }

    public boolean isBreakableBlock(Material material) {
        return breakableBlocks.contains(material);
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
