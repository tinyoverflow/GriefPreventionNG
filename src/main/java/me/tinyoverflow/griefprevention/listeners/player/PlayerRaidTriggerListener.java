package me.tinyoverflow.griefprevention.listeners.player;

import me.tinyoverflow.griefprevention.Claim;
import me.tinyoverflow.griefprevention.ClaimPermission;
import me.tinyoverflow.griefprevention.GriefPrevention;
import me.tinyoverflow.griefprevention.PlayerData;
import me.tinyoverflow.griefprevention.datastore.DataStore;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.raid.RaidTriggerEvent;

public class PlayerRaidTriggerListener implements Listener {
    private final GriefPrevention plugin;
    private final DataStore dataStore;

    public PlayerRaidTriggerListener(GriefPrevention plugin, DataStore dataStore) {
        this.plugin = plugin;
        this.dataStore = dataStore;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerTriggerRaid(RaidTriggerEvent event) {
        if (!plugin.getPluginConfig().getClaimConfiguration().getProtectionConfiguration().isPreventRaidTriggersEnabled())
            return;

        Player player = event.getPlayer();
        PlayerData playerData = dataStore.getPlayerData(player.getUniqueId());

        Claim claim = dataStore.getClaimAt(player.getLocation(), false, playerData.lastClaim);
        if (claim == null) return;

        playerData.lastClaim = claim;
        if (claim.checkPermission(player, ClaimPermission.Build, event) == null) return;

        event.setCancelled(true);
    }
}
