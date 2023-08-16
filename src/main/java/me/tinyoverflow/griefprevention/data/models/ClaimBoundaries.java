package me.tinyoverflow.griefprevention.data.models;

import lombok.Data;
import org.bukkit.World;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

@Data
public class ClaimBoundaries
{
    private World world;
    private Vector lesser;
    private Vector greater;

    public ClaimBoundaries(@Nullable World world, Vector lesser, Vector greater)
    {
        this.world = world;
        this.lesser = lesser;
        this.greater = greater;
    }
}
