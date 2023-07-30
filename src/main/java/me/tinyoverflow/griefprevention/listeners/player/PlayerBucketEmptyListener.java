package me.tinyoverflow.griefprevention.listeners.player;

import me.tinyoverflow.griefprevention.*;
import me.tinyoverflow.griefprevention.datastore.DataStore;
import me.tinyoverflow.griefprevention.logger.ActivityType;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBucketEmptyEvent;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class PlayerBucketEmptyListener implements Listener
{
    private final GriefPrevention plugin;
    private final DataStore dataStore;

    public PlayerBucketEmptyListener(GriefPrevention plugin, DataStore dataStore)
    {
        this.plugin = plugin;
        this.dataStore = dataStore;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerBucketEmpty(PlayerBucketEmptyEvent bucketEvent)
    {
        if (!plugin.claimsEnabledForWorld(bucketEvent.getBlockClicked().getWorld())) return;

        Player player = bucketEvent.getPlayer();
        Block block = bucketEvent.getBlockClicked().getRelative(bucketEvent.getBlockFace());
        int minLavaDistance = 10;

        // Fixes #1155:
        // Prevents waterlogging blocks placed on a claim's edge.
        // Waterlogging a block affects the clicked block, and NOT the adjacent location relative to it.
        if (bucketEvent.getBucket() == Material.WATER_BUCKET &&
            bucketEvent.getBlockClicked().getBlockData() instanceof Waterlogged)
        {
            block = bucketEvent.getBlockClicked();
        }

        //make sure the player is allowed to build at the location
        String noBuildReason = plugin.allowBuild(player, block.getLocation(), Material.WATER);
        if (noBuildReason != null)
        {
            GriefPrevention.sendMessage(player, TextMode.ERROR, noBuildReason);
            bucketEvent.setCancelled(true);
            return;
        }

        //if the bucket is being used in a claim, allow for dumping lava closer to other players
        PlayerData playerData = dataStore.getPlayerData(player.getUniqueId());
        Claim claim = dataStore.getClaimAt(block.getLocation(), false, playerData.lastClaim);
        if (claim != null)
        {
            minLavaDistance = 3;
        }

        //otherwise no wilderness dumping in creative mode worlds
        else if (plugin.creativeRulesApply(block.getLocation()))
        {
            if (block.getY() >= plugin.getSeaLevel(block.getWorld()) - 5 &&
                !player.hasPermission("griefprevention.lava"))
            {
                if (bucketEvent.getBucket() == Material.LAVA_BUCKET)
                {
                    GriefPrevention.sendMessage(player, TextMode.ERROR, Messages.NoWildernessBuckets);
                    bucketEvent.setCancelled(true);
                    return;
                }
            }
        }

        //lava buckets can't be dumped near other players unless pvp is on
        if (!doesAllowLavaProximityInWorld(block.getWorld()) && !player.hasPermission("griefprevention.lava"))
        {
            if (bucketEvent.getBucket() == Material.LAVA_BUCKET)
            {
                List<Player> players = block.getWorld().getPlayers();
                for (Player otherPlayer : players)
                {
                    Location location = otherPlayer.getLocation();
                    if (!otherPlayer.equals(player) && otherPlayer.getGameMode() == GameMode.SURVIVAL && player.canSee(
                            otherPlayer) && block.getY() >= location.getBlockY() - 1 &&
                        location.distanceSquared(block.getLocation()) < minLavaDistance * minLavaDistance)
                    {
                        GriefPrevention.sendMessage(
                                player,
                                TextMode.ERROR,
                                Messages.NoLavaNearOtherPlayer,
                                "another player"
                        );
                        bucketEvent.setCancelled(true);
                        return;
                    }
                }
            }
        }

        //log any suspicious placements (check sea level, world type, and adjacent blocks)
        if (block.getY() >= plugin.getSeaLevel(block.getWorld()) - 5 && !player.hasPermission("griefprevention.lava") &&
            block.getWorld().getEnvironment() != World.Environment.NETHER)
        {
            //if certain blocks are nearby, it's less suspicious and not worth logging
            Set<Material> exclusionAdjacentTypes = bucketEvent.getBucket() == Material.WATER_BUCKET
                    ? EnumSet.of(Material.WATER, Material.FARMLAND, Material.DIRT, Material.STONE)
                    : EnumSet.of(Material.LAVA, Material.DIRT, Material.STONE);

            boolean makeLogEntry = true;
            BlockFace[] adjacentDirections = new BlockFace[]{BlockFace.EAST, BlockFace.WEST, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.DOWN};
            for (BlockFace direction : adjacentDirections)
            {
                Material adjacentBlockType = block.getRelative(direction).getType();
                if (exclusionAdjacentTypes.contains(adjacentBlockType))
                {
                    makeLogEntry = false;
                    break;
                }
            }

            if (makeLogEntry)
            {
                GriefPrevention.AddLogEntry(
                        player.getName() + " placed suspicious " + bucketEvent.getBucket().name() + " @ " +
                        GriefPrevention.getFriendlyLocationString(
                                block.getLocation()), ActivityType.SUSPICIOUS, true);
            }
        }
    }

    private boolean doesAllowLavaProximityInWorld(World world)
    {
        return GriefPrevention.instance.pvpRulesApply(world)
                ? GriefPrevention.instance.getPluginConfig().getPvpConfiguration().isAllowLavaNearPlayers()
                : GriefPrevention.instance.getPluginConfig().getPvpConfiguration().isAllowLavaNearPlayersNonPvP();
    }
}
