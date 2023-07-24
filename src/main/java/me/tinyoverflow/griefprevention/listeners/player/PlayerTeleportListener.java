package me.tinyoverflow.griefprevention.listeners.player;

import me.tinyoverflow.griefprevention.*;
import me.tinyoverflow.griefprevention.datastore.DataStore;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;

import java.util.function.Supplier;

public class PlayerTeleportListener implements Listener {
    private final GriefPrevention plugin;
    private final DataStore dataStore;

    public PlayerTeleportListener(GriefPrevention plugin, DataStore dataStore) {
        this.plugin = plugin;
        this.dataStore = dataStore;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        PlayerData playerData = dataStore.getPlayerData(player.getUniqueId());

        //FEATURE: prevent players from using ender pearls to gain access to secured claims
        PlayerTeleportEvent.TeleportCause cause = event.getCause();
        if (cause == PlayerTeleportEvent.TeleportCause.CHORUS_FRUIT || (cause == PlayerTeleportEvent.TeleportCause.ENDER_PEARL && plugin.getPluginConfig().getClaimConfiguration().getProtectionConfiguration().isPreventEnderPearlsEnabled())) {
            Claim toClaim = dataStore.getClaimAt(event.getTo(), false, playerData.lastClaim);
            if (toClaim != null) {
                playerData.lastClaim = toClaim;
                Supplier<String> noAccessReason = toClaim.checkPermission(player, ClaimPermission.Access, event);
                if (noAccessReason != null) {
                    GriefPrevention.sendMessage(player, TextMode.Err, noAccessReason.get());
                    event.setCancelled(true);
                    if (cause == PlayerTeleportEvent.TeleportCause.ENDER_PEARL)
                        player.getInventory().addItem(new ItemStack(Material.ENDER_PEARL));
                }
            }
        }

        //FEATURE: prevent teleport abuse to win sieges

        //these rules only apply to siege worlds only
        if (!plugin.getPluginConfig().getSiegeConfiguration().isEnabledForWorld(player.getWorld())) return;

        //these rules do not apply to admins
        if (player.hasPermission("griefprevention.siegeteleport")) return;

        //Ignore vanilla teleports (usually corrective teleports? See issue #210)
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.UNKNOWN) return;

        Location source = event.getFrom();
        Claim sourceClaim = dataStore.getClaimAt(source, false, playerData.lastClaim);
        if (sourceClaim != null && sourceClaim.siegeData != null) {
            GriefPrevention.sendMessage(player, TextMode.Err, Messages.SiegeNoTeleport);
            event.setCancelled(true);
            return;
        }

        Location destination = event.getTo();
        Claim destinationClaim = dataStore.getClaimAt(destination, false, null);
        if (destinationClaim != null && destinationClaim.siegeData != null) {
            GriefPrevention.sendMessage(player, TextMode.Err, Messages.BesiegedNoTeleport);
            event.setCancelled(true);
        }
    }
}
