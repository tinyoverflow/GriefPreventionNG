package me.tinyoverflow.griefprevention.listeners.player;

import me.tinyoverflow.griefprevention.GriefPrevention;
import me.tinyoverflow.griefprevention.PlayerData;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.Calendar;

public class PlayerRespawnListener implements Listener
{
    private final GriefPrevention plugin;

    public PlayerRespawnListener(GriefPrevention plugin)
    {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    void onPlayerRespawn(PlayerRespawnEvent event)
    {
        Player player = event.getPlayer();
        PlayerData playerData = plugin.dataStore.getPlayerData(player.getUniqueId());
        playerData.lastSpawn = Calendar.getInstance().getTimeInMillis();
        playerData.lastPvpTimestamp = 0;

        // Resend GriefPrevention messages that would have been sent while the player was dead.
        if (playerData.messageOnRespawn != null)
        {
            GriefPrevention.sendMessage(player, ChatColor.RESET, playerData.messageOnRespawn, 40L);
            playerData.messageOnRespawn = null;
        }

        // Apply PvP Protection if needed.
        plugin.checkPvpProtectionNeeded(player);
    }
}
