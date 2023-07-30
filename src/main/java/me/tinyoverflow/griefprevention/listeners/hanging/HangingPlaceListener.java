package me.tinyoverflow.griefprevention.listeners.hanging;

import me.tinyoverflow.griefprevention.Claim;
import me.tinyoverflow.griefprevention.GriefPrevention;
import me.tinyoverflow.griefprevention.PlayerData;
import me.tinyoverflow.griefprevention.TextMode;
import me.tinyoverflow.griefprevention.datastore.DataStore;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.hanging.HangingPlaceEvent;

public class HangingPlaceListener implements Listener
{
    private final GriefPrevention plugin;
    private final DataStore dataStore;

    public HangingPlaceListener(GriefPrevention plugin, DataStore dataStore)
    {
        this.plugin = plugin;
        this.dataStore = dataStore;
    }

    //when a painting is placed...
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPaintingPlace(HangingPlaceEvent event)
    {
        //don't track in worlds where claims are not enabled
        if (!plugin.claimsEnabledForWorld(event.getBlock().getWorld())) return;

        //FEATURE: similar to above, placing a painting requires build permission in the claim

        //if the player doesn't have permission, don't allow the placement
        String noBuildReason = plugin.allowBuild(event.getPlayer(), event.getEntity().getLocation(), Material.PAINTING);
        if (noBuildReason != null)
        {
            event.setCancelled(true);
            GriefPrevention.sendMessage(event.getPlayer(), TextMode.ERROR, noBuildReason);
        }

        //otherwise, apply entity-count limitations for creative worlds
        else if (plugin.creativeRulesApply(event.getEntity().getLocation()))
        {
            PlayerData playerData = dataStore.getPlayerData(event.getPlayer().getUniqueId());
            Claim claim = dataStore.getClaimAt(event.getBlock().getLocation(), false, playerData.lastClaim);
            if (claim == null) return;

            String noEntitiesReason = claim.allowMoreEntities(false);
            if (noEntitiesReason != null)
            {
                GriefPrevention.sendMessage(event.getPlayer(), TextMode.ERROR, noEntitiesReason);
                event.setCancelled(true);
            }
        }
    }
}
