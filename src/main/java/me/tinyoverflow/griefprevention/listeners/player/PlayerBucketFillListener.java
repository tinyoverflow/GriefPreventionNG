package me.tinyoverflow.griefprevention.listeners.player;

import me.tinyoverflow.griefprevention.GriefPrevention;
import me.tinyoverflow.griefprevention.TextMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBucketFillEvent;

public class PlayerBucketFillListener implements Listener {
    private final GriefPrevention plugin;

    public PlayerBucketFillListener(GriefPrevention plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerBucketFill(PlayerBucketFillEvent bucketEvent) {
        Player player = bucketEvent.getPlayer();
        Block block = bucketEvent.getBlockClicked();

        if (!plugin.claimsEnabledForWorld(block.getWorld())) return;

        //make sure the player is allowed to build at the location
        String noBuildReason = plugin.allowBuild(player, block.getLocation(), Material.AIR);
        if (noBuildReason != null) {
            //exemption for cow milking (permissions will be handled by player interact with entity event instead)
            Material blockType = block.getType();
            if (blockType == Material.AIR) return;
            if (blockType.isSolid()) {
                BlockData blockData = block.getBlockData();
                if (!(blockData instanceof Waterlogged) || !((Waterlogged) blockData).isWaterlogged()) return;
            }

            GriefPrevention.sendMessage(player, TextMode.Err, noBuildReason);
            bucketEvent.setCancelled(true);
        }
    }
}
