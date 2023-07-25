package me.tinyoverflow.griefprevention.listeners.player;

import me.tinyoverflow.griefprevention.GriefPrevention;
import me.tinyoverflow.griefprevention.PlayerData;
import me.tinyoverflow.griefprevention.datastore.DataStore;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class PlayerQuitListener implements Listener
{
    private final GriefPrevention plugin;
    private final DataStore dataStore;

    public PlayerQuitListener(GriefPrevention plugin, DataStore dataStore)
    {
        this.plugin = plugin;
        this.dataStore = dataStore;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    void onPlayerQuit(PlayerQuitEvent event)
    {
        Player player = event.getPlayer();
        UUID playerID = player.getUniqueId();
        PlayerData playerData = dataStore.getPlayerData(playerID);

        //If player is not trapped in a portal and has a pending rescue task, remove the associated metadata
        //Why 9? No idea why, but this is decremented by 1 when the player disconnects.
        if (player.getPortalCooldown() < 9)
        {
            player.removeMetadata("GP_PORTALRESCUE", plugin);
        }

        dataStore.savePlayerData(player.getUniqueId(), playerData);

        //FEATURE: players in pvp combat when they log out will die
        if (plugin.getPluginConfig().getPvpConfiguration().isPunishLogouts() && playerData.inPvpCombat())
        {
            player.setHealth(0);
        }

        //FEATURE: during a siege, any player who logs out dies and forfeits the siege

        //if player was involved in a siege, he forfeits
        if (playerData.siegeData != null)
        {
            if (player.getHealth() > 0)
                player.setHealth(0);  //might already be zero from above, this avoids a double death message
        }

        //drop data about this player
        dataStore.clearCachedPlayerData(playerID);
    }
}
