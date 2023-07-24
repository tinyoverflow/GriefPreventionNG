package me.tinyoverflow.griefprevention.listeners.player;

import me.tinyoverflow.griefprevention.PlayerData;
import me.tinyoverflow.griefprevention.datastore.DataStore;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

public class PlayerLoginListener implements Listener
{
    private final DataStore dataStore;

    public PlayerLoginListener(DataStore dataStore)
    {
        this.dataStore = dataStore;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    void onPlayerLogin(PlayerLoginEvent event)
    {
        PlayerData playerData = dataStore.getPlayerData(event.getPlayer().getUniqueId());
        playerData.ipAddress = event.getAddress();
    }
}
