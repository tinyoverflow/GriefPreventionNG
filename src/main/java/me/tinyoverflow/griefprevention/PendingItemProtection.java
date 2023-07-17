package me.tinyoverflow.griefprevention;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class PendingItemProtection
{
    public Location location;
    public UUID owner;
    public long expirationTimestamp;
    public ItemStack itemStack;

    public PendingItemProtection(Location location, UUID owner, long expirationTimestamp, ItemStack itemStack)
    {
        this.location = location;
        this.owner = owner;
        this.expirationTimestamp = expirationTimestamp;
        this.itemStack = itemStack;
    }
}
