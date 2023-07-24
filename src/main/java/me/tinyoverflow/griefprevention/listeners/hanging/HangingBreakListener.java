package me.tinyoverflow.griefprevention.listeners.hanging;

import me.tinyoverflow.griefprevention.GriefPrevention;
import me.tinyoverflow.griefprevention.TextMode;
import me.tinyoverflow.griefprevention.datastore.DataStore;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;

public class HangingBreakListener implements Listener
{
    private final GriefPrevention plugin;
    private final DataStore dataStore;

    public HangingBreakListener(GriefPrevention plugin, DataStore dataStore)
    {
        this.plugin = plugin;
        this.dataStore = dataStore;
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemFrameBrokenByBoat(HangingBreakEvent event)
    {
        // Checks if the event is caused by physics - 90% of cases caused by a boat (other 10% would be a block,
        // however since it's in a claim, unless you use a TNT block we don't need to worry about it).
        if (event.getCause() != HangingBreakEvent.RemoveCause.PHYSICS) {
            return;
        }

        // Cancels the event if in a claim, as we can not efficiently retrieve the person/entity who broke the Item Frame/Hangable Item.
        if (dataStore.getClaimAt(event.getEntity().getLocation(), false, null) != null) {
            event.setCancelled(true);
        }
    }

    //when a painting is broken
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onBreakPainting(HangingBreakEvent event)
    {
        //don't track in worlds where claims are not enabled
        if (!plugin.claimsEnabledForWorld(event.getEntity().getWorld())) return;

        //Ignore cases where item frames should break due to no supporting blocks
        if (event.getCause() == HangingBreakEvent.RemoveCause.PHYSICS) return;

        //FEATURE: claimed paintings are protected from breakage

        //explosions don't destroy hangings
        if (event.getCause() == HangingBreakEvent.RemoveCause.EXPLOSION) {
            event.setCancelled(true);
            return;
        }

        //only allow players to break paintings, not anything else (like water and explosions)
        if (!(event instanceof HangingBreakByEntityEvent entityEvent)) {
            event.setCancelled(true);
            return;
        }

        //who is removing it?
        Entity remover = entityEvent.getRemover();

        //again, making sure the breaker is a player
        if (remover.getType() != EntityType.PLAYER) {
            event.setCancelled(true);
            return;
        }

        //if the player doesn't have build permission, don't allow the breakage
        Player playerRemover = (Player) entityEvent.getRemover();
        String noBuildReason = plugin.allowBuild(playerRemover, event.getEntity().getLocation(), Material.AIR);
        if (noBuildReason != null) {
            event.setCancelled(true);
            GriefPrevention.sendMessage(playerRemover, TextMode.Err, noBuildReason);
        }
    }
}
