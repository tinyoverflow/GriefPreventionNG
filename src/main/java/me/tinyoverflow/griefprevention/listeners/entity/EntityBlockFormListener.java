package me.tinyoverflow.griefprevention.listeners.entity;

import me.tinyoverflow.griefprevention.GriefPrevention;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.EntityBlockFormEvent;

public class EntityBlockFormListener implements Listener {
    private final GriefPrevention plugin;

    public EntityBlockFormListener(GriefPrevention plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onEntityFormBlock(EntityBlockFormEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof Player player) {
            String noBuildReason = plugin.allowBuild(
                    player,
                    event.getBlock().getLocation(),
                    event.getNewState().getType()
            );

            if (noBuildReason != null) {
                event.setCancelled(true);
            }
        }
    }
}
