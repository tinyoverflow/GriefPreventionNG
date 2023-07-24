package me.tinyoverflow.griefprevention.listeners.player;

import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class PlayerInteractAtEntityListener implements Listener {

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
        if (event.getRightClicked().getType().equals(EntityType.ARMOR_STAND)) {
            // Replaces onPlayerInteractEntity((PlayerInteractEntityEvent) event);
            event.setCancelled(new PlayerInteractEntityEvent(
                    event.getPlayer(),
                    event.getRightClicked()
            ).callEvent());
        }
    }
}
