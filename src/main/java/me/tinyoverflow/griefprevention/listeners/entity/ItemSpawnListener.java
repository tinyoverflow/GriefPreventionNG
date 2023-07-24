package me.tinyoverflow.griefprevention.listeners.entity;

import me.tinyoverflow.griefprevention.GriefPrevention;
import me.tinyoverflow.griefprevention.PendingItemProtection;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.ArrayList;

public class ItemSpawnListener implements Listener
{
    private final GriefPrevention plugin;

    public ItemSpawnListener(GriefPrevention plugin)
    {
        this.plugin = plugin;
    }

    //when an item spawns...
    @EventHandler(priority = EventPriority.LOWEST)
    public void onItemSpawn(ItemSpawnEvent event)
    {
        //if in a creative world, cancel the event (don't drop items on the ground)
        if (plugin.creativeRulesApply(event.getLocation())) {
            event.setCancelled(true);
        }

        //if item is on watch list, apply protection
        ArrayList<PendingItemProtection> watchList = plugin.pendingItemWatchList;
        Item newItem = event.getEntity();
        Long now = null;
        for (int i = 0; i < watchList.size(); i++) {
            PendingItemProtection pendingProtection = watchList.get(i);
            //ignore and remove any expired pending protections
            if (now == null) now = System.currentTimeMillis();
            if (pendingProtection.expirationTimestamp < now) {
                watchList.remove(i--);
                continue;
            }
            //skip if item stack doesn't match
            if (pendingProtection.itemStack.getAmount() != newItem.getItemStack().getAmount() ||
                pendingProtection.itemStack.getType() != newItem.getItemStack().getType()) {
                continue;
            }

            //skip if new item location isn't near the expected spawn area
            Location spawn = event.getLocation();
            Location expected = pendingProtection.location;
            if (!spawn.getWorld().equals(expected.getWorld()) ||
                spawn.getX() < expected.getX() - 5 ||
                spawn.getX() > expected.getX() + 5 ||
                spawn.getZ() < expected.getZ() - 5 ||
                spawn.getZ() > expected.getZ() + 5 ||
                spawn.getY() < expected.getY() - 15 ||
                spawn.getY() > expected.getY() + 3) {
                continue;
            }

            //otherwise, mark item with protection information
            newItem.setMetadata("GP_ITEMOWNER", new FixedMetadataValue(plugin, pendingProtection.owner));

            //and remove pending protection data
            watchList.remove(i);
            break;
        }
    }
}
