package me.tinyoverflow.griefprevention.listeners.player;

import me.tinyoverflow.griefprevention.GriefPrevention;
import me.tinyoverflow.griefprevention.Messages;
import me.tinyoverflow.griefprevention.PlayerData;
import me.tinyoverflow.griefprevention.TextMode;
import me.tinyoverflow.griefprevention.datastore.DataStore;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;

public class PlayerDropItemListener implements Listener
{
    private final GriefPrevention plugin;
    private final DataStore dataStore;

    public PlayerDropItemListener(GriefPrevention plugin, DataStore dataStore)
    {
        this.plugin = plugin;
        this.dataStore = dataStore;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerDropItem(PlayerDropItemEvent event)
    {
        Player player = event.getPlayer();

        //in creative worlds, dropping items is blocked
        if (plugin.creativeRulesApply(player.getLocation()))
        {
            event.setCancelled(true);
            return;
        }

        PlayerData playerData = dataStore.getPlayerData(player.getUniqueId());

        //FEATURE: players under siege or in PvP combat, can't throw items on the ground to hide
        //them or give them away to other players before they are defeated

        //if in combat, don't let him drop it
        if (!plugin.config_pvp_allowCombatItemDrop && playerData.inPvpCombat())
        {
            GriefPrevention.sendMessage(player, TextMode.Err, Messages.PvPNoDrop);
            event.setCancelled(true);
        }

        //if he's under siege, don't let him drop it
        else if (playerData.siegeData != null)
        {
            GriefPrevention.sendMessage(player, TextMode.Err, Messages.SiegeNoDrop);
            event.setCancelled(true);
        }
    }
}
