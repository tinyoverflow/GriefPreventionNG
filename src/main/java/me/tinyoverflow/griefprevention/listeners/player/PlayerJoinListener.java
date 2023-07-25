package me.tinyoverflow.griefprevention.listeners.player;

import me.tinyoverflow.griefprevention.*;
import me.tinyoverflow.griefprevention.datastore.DataStore;
import me.tinyoverflow.griefprevention.tasks.WelcomeTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Date;

public class PlayerJoinListener implements Listener
{
    private final GriefPrevention plugin;
    private final DataStore dataStore;

    public PlayerJoinListener(GriefPrevention plugin, DataStore dataStore)
    {
        this.plugin = plugin;
        this.dataStore = dataStore;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    void onPlayerJoin(PlayerJoinEvent event)
    {
        Player player = event.getPlayer();

        PlayerData playerData = dataStore.getPlayerData(player.getUniqueId());
        playerData.lastSpawn = new Date().getTime();

        //if newish, prevent chat until he's moved a bit to prove he's not a bot
        if (GriefPrevention.isNewToServer(player) && !player.hasPermission("griefprevention.premovementchat"))
        {
            playerData.noChatLocation = player.getLocation();
        }

        //if player has never played on the server before...
        if (!player.hasPlayedBefore())
        {
            //may need pvp protection
            plugin.checkPvpProtectionNeeded(player);

            //if in survival claims mode, send a message about the claim basics video (except for admins - assumed experts)
            if (plugin.getPluginConfig().getClaimConfiguration().getWorldMode(player.getWorld()) == ClaimsMode.Survival && !player.hasPermission("griefprevention.adminclaims") && dataStore.claims.size() > 10)
            {
                WelcomeTask task = new WelcomeTask(player);
                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, task, plugin.getPluginConfig().getClaimConfiguration().getManualConfiguration().delay * 20L);
            }
        }

        //in case player has changed his name, on successful login, update UUID > Name mapping
        GriefPrevention.cacheUUIDNamePair(player.getUniqueId(), player.getName());

        //is he stuck in a portal frame?
        if (player.hasMetadata("GP_PORTALRESCUE"))
        {
            //If so, let him know and rescue him in 10 seconds. If he is in fact not trapped, hopefully chunks will have loaded by this time so he can walk out.
            GriefPrevention.sendMessage(player, TextMode.Info, Messages.NetherPortalTrapDetectionMessage, 20L);
            new BukkitRunnable()
            {
                @Override
                public void run()
                {
                    if (player.getPortalCooldown() > 8 && player.hasMetadata("GP_PORTALRESCUE"))
                    {
                        GriefPrevention.AddLogEntry("Rescued " + player.getName() + " from a nether portal.\nTeleported from " + player.getLocation() + " to " + player.getMetadata("GP_PORTALRESCUE").get(0).value().toString(), CustomLogEntryTypes.Debug);
                        player.teleport((Location) player.getMetadata("GP_PORTALRESCUE").get(0).value());
                        player.removeMetadata("GP_PORTALRESCUE", plugin);
                    }
                }
            }.runTaskLater(plugin, 200L);
        }
        //Otherwise just reset cooldown, just in case they happened to logout again...
        else
        {
            player.setPortalCooldown(0);
        }
    }
}
