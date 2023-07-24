package me.tinyoverflow.griefprevention.listeners.entity;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.metadata.MetadataValue;

import java.util.List;

public class ItemMergeListener implements Listener
{
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onItemMerge(ItemMergeEvent event)
    {
        List<MetadataValue> data = event.getEntity().getMetadata("GP_ITEMOWNER");
        event.setCancelled(data.size() > 0);
    }
}
