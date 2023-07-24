package me.tinyoverflow.griefprevention.listeners.player;

import me.tinyoverflow.griefprevention.PlayerData;
import me.tinyoverflow.griefprevention.datastore.DataStore;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;

public class PlayerKickListener implements Listener
{
    private final DataStore dataStore;

    public PlayerKickListener(DataStore dataStore)
    {
        this.dataStore = dataStore;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    void onPlayerKicked(PlayerKickEvent event)
    {
        Player player = event.getPlayer();
        PlayerData playerData = dataStore.getPlayerData(player.getUniqueId());
        playerData.wasKicked = true;
    }
}
