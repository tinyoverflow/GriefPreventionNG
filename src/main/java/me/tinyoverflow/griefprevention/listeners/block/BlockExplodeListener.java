package me.tinyoverflow.griefprevention.listeners.block;

import me.tinyoverflow.griefprevention.listeners.entity.EntityExplodeListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;

public class BlockExplodeListener implements Listener
{
    //when a block explodes...
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onBlockExplode(BlockExplodeEvent explodeEvent)
    {
        EntityExplodeListener.handleExplosion(explodeEvent.getBlock().getLocation(), null, explodeEvent.blockList());
    }
}
