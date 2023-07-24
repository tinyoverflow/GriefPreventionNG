package me.tinyoverflow.griefprevention.listeners.block;

import me.tinyoverflow.griefprevention.GriefPrevention;
import me.tinyoverflow.griefprevention.TextMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class BlockBreakListener implements Listener {
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockBreakEvent breakEvent)
    {
        Player player = breakEvent.getPlayer();
        Block block = breakEvent.getBlock();

        //make sure the player is allowed to break at the location
        String noBuildReason = GriefPrevention.instance.allowBreak(player, block, block.getLocation(), breakEvent);
        if (noBuildReason != null)
        {
            GriefPrevention.sendMessage(player, TextMode.Err, noBuildReason);
            breakEvent.setCancelled(true);
        }
    }
}
