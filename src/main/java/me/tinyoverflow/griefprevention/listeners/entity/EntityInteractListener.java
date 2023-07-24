package me.tinyoverflow.griefprevention.listeners.entity;

import me.tinyoverflow.griefprevention.GriefPrevention;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityInteractEvent;

public class EntityInteractListener implements Listener
{
    private final GriefPrevention plugin;

    public EntityInteractListener(GriefPrevention plugin)
    {
        this.plugin = plugin;
    }

    //don't allow entities to trample crops
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onEntityInteract(EntityInteractEvent event)
    {
        Material material = event.getBlock().getType();
        if (material == Material.FARMLAND) {
            if (!plugin.getPluginConfig().getClaimConfiguration().getProtectionConfiguration().isPreventCreaturesTramplingCropsEnabled()) {
                event.setCancelled(true);
            }
            else {
                Entity rider = event.getEntity().getPassenger();
                if (rider != null && rider.getType() == EntityType.PLAYER) {
                    event.setCancelled(true);
                }
            }
        }
    }
}
