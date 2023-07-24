package me.tinyoverflow.griefprevention.listeners.player;

import me.tinyoverflow.griefprevention.GriefPrevention;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class PlayerPortalListener implements Listener {
    private final GriefPrevention plugin;

    public PlayerPortalListener(GriefPrevention plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    void onPlayerPortal(PlayerPortalEvent event) {
        if (event.getTo().getWorld() == null) return;

        Player player = event.getPlayer();
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) {
            //FEATURE: when players get trapped in a nether portal, send them back through to the other side
            plugin.startRescueTask(player, player.getLocation());
        }
    }
}
