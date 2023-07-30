package me.tinyoverflow.griefprevention.listeners.block;

import me.tinyoverflow.griefprevention.GriefPrevention;
import me.tinyoverflow.griefprevention.TextMode;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockMultiPlaceEvent;

public class BlockMultiPlaceListener implements Listener
{

    //when a player places multiple blocks...
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onBlocksPlace(BlockMultiPlaceEvent placeEvent)
    {
        Player player = placeEvent.getPlayer();

        //don't track in worlds where claims are not enabled
        if (!GriefPrevention.instance.claimsEnabledForWorld(placeEvent.getBlock().getWorld())) return;

        //make sure the player is allowed to build at the location
        for (BlockState block : placeEvent.getReplacedBlockStates())
        {
            String noBuildReason = GriefPrevention.instance.allowBuild(player, block.getLocation(), block.getType());
            if (noBuildReason != null)
            {
                GriefPrevention.sendMessage(player, TextMode.ERROR, noBuildReason);
                placeEvent.setCancelled(true);
                return;
            }
        }
    }
}
