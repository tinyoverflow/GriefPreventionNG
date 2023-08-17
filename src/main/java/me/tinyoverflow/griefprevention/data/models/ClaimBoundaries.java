package me.tinyoverflow.griefprevention.data.models;

import lombok.Data;
import org.bukkit.World;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

/**
 * Represents two coordinates as {@link Vector}s inside a {@link World} to be used as the boundaries for a claim.
 */
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

    /**
     * Calculates the 2D area of the current boundaries.
     *
     * @return The area in blocks as {@link Integer}.
     */
    public int area()
    {
        int width = Math.abs(greater.getBlockX() - lesser.getBlockX());
        int length = Math.abs(greater.getBlockZ() - lesser.getBlockZ());
        return width * length;
    }

    /**
     * Calculates the volume of the current boundaries.
     *
     * @return The volume in blocks as {@link Integer}.
     */
    public int volume()
    {
        int height = Math.abs(greater.getBlockY() - lesser.getBlockY());
        return area() * height;
    }

    /**
     * Checks whether another {@link ClaimBoundaries} object overlaps this one.
     *
     * @param other Other {@link ClaimBoundaries} object to check against.
     * @return {@code true} if they overlap, {@code false} if they don't.
     * @see <a href="https://silentmatt.com/rectangle-intersection/">Rectangle Intersection Visualization</a>
     */
    public boolean overlaps(ClaimBoundaries other)
    {
        return world.equals(other.world) &&
               lesser.getBlockX() <= other.getGreater().getBlockX() &&
               greater.getBlockX() >= other.getLesser().getBlockX() &&
               lesser.getBlockY() <= other.getGreater().getBlockY() &&
               greater.getBlockY() >= other.getLesser().getBlockY() &&
               lesser.getBlockZ() <= other.getGreater().getBlockZ() &&
               greater.getBlockZ() >= other.getLesser().getBlockZ();
    }
}
