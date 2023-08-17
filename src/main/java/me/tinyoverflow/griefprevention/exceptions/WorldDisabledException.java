package me.tinyoverflow.griefprevention.exceptions;

import lombok.Getter;
import org.bukkit.World;

@Getter
public class WorldDisabledException extends Exception
{
    private final World world;

    public WorldDisabledException(World world)
    {
        this.world = world;
    }
}
