package me.tinyoverflow.griefprevention.listeners.entity;

import me.tinyoverflow.griefprevention.GriefPrevention;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreakDoorEvent;

public class EntityBreakDoorListener implements Listener
{
    private final GriefPrevention plugin;

    public EntityBreakDoorListener(GriefPrevention plugin)
    {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onZombieBreakDoor(EntityBreakDoorEvent event)
    {
        if (plugin.getPluginConfig().getClaimConfiguration().getProtectionConfiguration().isPreventZombiesBreakingDoorsEnabled())
            event.setCancelled(true);
    }
}
