package me.tinyoverflow.griefprevention.listeners.player;

import me.tinyoverflow.griefprevention.*;
import me.tinyoverflow.griefprevention.datastore.DataStore;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.function.Supplier;

public class PlayerCommandPreprocessListener implements Listener {
    private final GriefPrevention plugin;
    private final DataStore dataStore;

    public PlayerCommandPreprocessListener(GriefPrevention plugin, DataStore dataStore) {
        this.plugin = plugin;
        this.dataStore = dataStore;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    synchronized void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();
        String[] args = message.split(" ");

        String command = args[0].toLowerCase();

        Player player = event.getPlayer();
        PlayerData playerData = dataStore.getPlayerData(event.getPlayer().getUniqueId());

        boolean isCommandBlocked = plugin.config_pvp_blockedCommands.contains(command);
        if ((playerData.inPvpCombat() || playerData.siegeData != null) && isCommandBlocked) {
            event.setCancelled(true);
            GriefPrevention.sendMessage(event.getPlayer(), TextMode.Err, Messages.CommandBannedInPvP);
            return;
        }

        // if requires access trust, check for permission
        boolean isMonitoredCommand = false;
        String lowerCaseMessage = message.toLowerCase();
        for (String monitoredCommand : plugin.getPluginConfig().getClaimConfiguration().getCommandTrustLimitsConfiguration().accessTrust) {
            if (lowerCaseMessage.startsWith(monitoredCommand)) {
                isMonitoredCommand = true;
                break;
            }
        }

        if (isMonitoredCommand) {
            Claim claim = dataStore.getClaimAt(player.getLocation(), false, playerData.lastClaim);
            if (claim != null) {
                playerData.lastClaim = claim;
                Supplier<String> reason = claim.checkPermission(player, ClaimPermission.Access, event);
                if (reason != null) {
                    GriefPrevention.sendMessage(player, TextMode.Err, reason.get());
                    event.setCancelled(true);
                }
            }
        }
    }
}
