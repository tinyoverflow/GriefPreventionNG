package me.tinyoverflow.griefprevention.listeners.block;

import com.griefprevention.visualization.BoundaryVisualization;
import com.griefprevention.visualization.VisualizationType;
import me.tinyoverflow.griefprevention.*;
import me.tinyoverflow.griefprevention.datastore.DataStore;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.type.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

public class BlockPlaceListener implements Listener
{
    private static final BlockFace[] HORIZONTAL_DIRECTIONS = new BlockFace[]{BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};
    private final DataStore dataStore;
    private final EnumSet<Material> trashBlocks = EnumSet.of(
            Material.COBBLESTONE,
            Material.TORCH,
            Material.DIRT,
            Material.OAK_SAPLING,
            Material.SPRUCE_SAPLING,
            Material.BIRCH_SAPLING,
            Material.JUNGLE_SAPLING,
            Material.ACACIA_SAPLING,
            Material.DARK_OAK_SAPLING,
            Material.GRAVEL,
            Material.SAND,
            Material.TNT,
            Material.CRAFTING_TABLE,
            Material.TUFF,
            Material.COBBLED_DEEPSLATE
    );

    public BlockPlaceListener(DataStore dataStore)
    {
        this.dataStore = dataStore;
    }

    //when a player places a block...

    public static boolean isActiveBlock(@NotNull BlockState blockState)
    {
        Material type = blockState.getType();
        return type == Material.HOPPER || type == Material.BEACON || type == Material.SPAWNER;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onBlockPlace(BlockPlaceEvent placeEvent)
    {
        Player player = placeEvent.getPlayer();
        Block block = placeEvent.getBlock();

        //FEATURE: limit fire placement, to prevent PvP-by-fire

        //if placed block is fire and pvp is off, apply rules for proximity to other players
        if (block.getType() == Material.FIRE && !doesAllowFireProximityInWorld(block.getWorld()))
        {
            List<Player> players = block.getWorld().getPlayers();
            for (Player otherPlayer : players)
            {
                // Ignore players in creative or spectator mode to avoid users from checking if someone is spectating near them
                if (otherPlayer.getGameMode() == GameMode.CREATIVE || otherPlayer.getGameMode() == GameMode.SPECTATOR)
                {
                    continue;
                }

                Location location = otherPlayer.getLocation();
                if (!otherPlayer.equals(player) && location.distanceSquared(block.getLocation()) < 9 && player.canSee(
                        otherPlayer))
                {
                    GriefPrevention.sendMessage(player, TextMode.ERROR, Messages.PlayerTooCloseForFire2);
                    placeEvent.setCancelled(true);
                    return;
                }
            }
        }

        //don't track in worlds where claims are not enabled
        if (!GriefPrevention.instance.claimsEnabledForWorld(placeEvent.getBlock().getWorld())) return;

        //make sure the player is allowed to build at the location
        String noBuildReason = GriefPrevention.instance.allowBuild(player, block.getLocation(), block.getType());
        if (noBuildReason != null)
        {
            // Allow players with container trust to place books in lecterns
            PlayerData playerData = dataStore.getPlayerData(player.getUniqueId());
            Claim claim = dataStore.getClaimAt(block.getLocation(), true, playerData.lastClaim);
            if (block.getType() == Material.LECTERN && placeEvent.getBlockReplacedState().getType() == Material.LECTERN)
            {
                if (claim != null)
                {
                    playerData.lastClaim = claim;
                    Supplier<String> noContainerReason = claim.checkPermission(
                            player,
                            ClaimPermission.Inventory,
                            placeEvent
                    );
                    if (noContainerReason == null) return;

                    placeEvent.setCancelled(true);
                    GriefPrevention.sendMessage(player, TextMode.ERROR, noContainerReason.get());
                    return;
                }
            }
            GriefPrevention.sendMessage(player, TextMode.ERROR, noBuildReason);
            placeEvent.setCancelled(true);
            return;
        }

        //if the block is being placed within or under an existing claim
        PlayerData playerData = dataStore.getPlayerData(player.getUniqueId());
        Claim claim = dataStore.getClaimAt(block.getLocation(), true, playerData.lastClaim);

        //If block is a chest, don't allow a DoubleChest to form across a claim boundary
        denyConnectingDoubleChestsAcrossClaimBoundary(claim, block, player);

        if (claim != null)
        {
            playerData.lastClaim = claim;

            //warn about TNT not destroying claimed blocks
            if (block.getType() == Material.TNT && !claim.areExplosivesAllowed && playerData.siegeData == null)
            {
                GriefPrevention.sendMessage(player, TextMode.WARNING, Messages.NoTNTDamageClaims);
                GriefPrevention.sendMessage(player, TextMode.INSTRUCTION, Messages.ClaimExplosivesAdvertisement);
            }

            //if the player has permission for the claim and he's placing UNDER the claim
            if (block.getY() <= claim.lesserBoundaryCorner.getBlockY() && claim.checkPermission(
                    player,
                    ClaimPermission.Build,
                    placeEvent
            ) == null)
            {
                //extend the claim downward
                dataStore.extendClaim(
                        claim,
                        block.getY() -
                        GriefPrevention.instance.getPluginConfig().getClaimConfiguration().getCreationConfiguration().extendIntoGroundDistance
                );
            }

            //allow for a build warning in the future
            playerData.warnedAboutBuildingOutsideClaims = false;
        }

        //FEATURE: automatically create a claim when a player who has no claims places a chest

        //otherwise if there's no claim, the player is placing a chest, and new player automatic claims are enabled
        else if (
                GriefPrevention.instance.getPluginConfig().getClaimConfiguration().getCreationConfiguration().automaticPreferredRadius >
                -1 && player.hasPermission(
                        "griefprevention.createclaims") && block.getType() == Material.CHEST)
        {
            //if the chest is too deep underground, don't create the claim and explain why
            if (GriefPrevention.instance.getPluginConfig().getClaimConfiguration().getProtectionConfiguration().isLockContainersEnabled() &&
                block.getY() <
                GriefPrevention.instance.getPluginConfig().getClaimConfiguration().getCreationConfiguration().maximumDepth)
            {
                GriefPrevention.sendMessage(player, TextMode.WARNING, Messages.TooDeepToClaim);
                return;
            }

            int radius = GriefPrevention.instance.getPluginConfig().getClaimConfiguration().getCreationConfiguration().automaticPreferredRadius;

            //if the player doesn't have any claims yet, automatically create a claim centered at the chest
            if (playerData.getClaims().size() == 0 && player.getGameMode() == GameMode.SURVIVAL)
            {
                //radius == 0 means protect ONLY the chest
                if (GriefPrevention.instance.getPluginConfig().getClaimConfiguration().getCreationConfiguration().automaticPreferredRadius ==
                    0)
                {
                    dataStore.createClaim(
                            block.getWorld(),
                            block.getX(),
                            block.getX(),
                            block.getY(),
                            block.getY(),
                            block.getZ(),
                            block.getZ(),
                            player.getUniqueId(),
                            null,
                            null,
                            player
                    );
                    GriefPrevention.sendMessage(player, TextMode.SUCCESS, Messages.ChestClaimConfirmation);
                }

                //otherwise, create a claim in the area around the chest
                else
                {
                    //if failure due to insufficient claim blocks available
                    if (playerData.getRemainingClaimBlocks() < Math.pow(
                            1 + 2 *
                                GriefPrevention.instance.getPluginConfig().getClaimConfiguration().getCreationConfiguration().automaticMinimumRadius,
                            2
                    ))
                    {
                        GriefPrevention.sendMessage(player, TextMode.WARNING, Messages.NoEnoughBlocksForChestClaim);
                        return;
                    }

                    //as long as the automatic claim overlaps another existing claim, shrink it
                    //note that since the player had permission to place the chest, at the very least, the automatic claim will include the chest
                    CreateClaimResult result = null;
                    while (radius >=
                           GriefPrevention.instance.getPluginConfig().getClaimConfiguration().getCreationConfiguration().automaticMinimumRadius)
                    {
                        int area = (radius * 2 + 1) * (radius * 2 + 1);
                        if (playerData.getRemainingClaimBlocks() >= area)
                        {
                            result = dataStore.createClaim(
                                    block.getWorld(),
                                    block.getX() - radius,
                                    block.getX() + radius,
                                    block.getY() -
                                    GriefPrevention.instance.getPluginConfig().getClaimConfiguration().getCreationConfiguration().extendIntoGroundDistance,
                                    block.getY(),
                                    block.getZ() - radius,
                                    block.getZ() + radius,
                                    player.getUniqueId(),
                                    null,
                                    null,
                                    player
                            );

                            if (result.succeeded) break;
                        }

                        radius--;
                    }

                    if (result != null && result.claim != null)
                    {
                        if (result.succeeded)
                        {
                            //notify and explain to player
                            GriefPrevention.sendMessage(player, TextMode.SUCCESS, Messages.AutomaticClaimNotification);

                            //show the player the protected area
                            BoundaryVisualization.visualizeClaim(
                                    player,
                                    result.claim,
                                    com.griefprevention.visualization.VisualizationType.CLAIM,
                                    block
                            );
                        }
                        else
                        {
                            //notify and explain to player
                            GriefPrevention.sendMessage(
                                    player,
                                    TextMode.ERROR,
                                    Messages.AutomaticClaimOtherClaimTooClose
                            );

                            //show the player the protected area
                            BoundaryVisualization.visualizeClaim(
                                    player,
                                    result.claim,
                                    com.griefprevention.visualization.VisualizationType.CONFLICT_ZONE,
                                    block
                            );
                        }
                    }
                }

                GriefPrevention.sendMessage(
                        player,
                        TextMode.INSTRUCTION,
                        Messages.SurvivalBasicsVideo2,
                        DataStore.SURVIVAL_VIDEO_URL
                );
            }

            //check to see if this chest is in a claim, and warn when it isn't
            if (GriefPrevention.instance.getPluginConfig().getClaimConfiguration().getProtectionConfiguration().isLockContainersEnabled() &&
                dataStore.getClaimAt(
                        block.getLocation(),
                        false,
                        playerData.lastClaim
                ) == null)
            {
                GriefPrevention.sendMessage(player, TextMode.WARNING, Messages.UnprotectedChestWarning);
            }
        }

        //FEATURE: limit wilderness tree planting to grass, or dirt with more blocks beneath it
        else if (Tag.SAPLINGS.isTagged(block.getType()) && GriefPrevention.instance.config_blockSkyTrees &&
                 GriefPrevention.instance.claimsEnabledForWorld(
                         player.getWorld()))
        {
            Block earthBlock = placeEvent.getBlockAgainst();
            if (earthBlock.getType() != Material.GRASS)
            {
                if (earthBlock.getRelative(BlockFace.DOWN).getType() == Material.AIR ||
                    earthBlock.getRelative(BlockFace.DOWN).getRelative(
                            BlockFace.DOWN).getType() == Material.AIR)
                {
                    placeEvent.setCancelled(true);
                }
            }
        }

        //FEATURE: warn players when they're placing non-trash blocks outside of their claimed areas
        else if (!trashBlocks.contains(block.getType()) &&
                 GriefPrevention.instance.claimsEnabledForWorld(block.getWorld()))
        {
            if (!playerData.warnedAboutBuildingOutsideClaims && !player.hasPermission("griefprevention.adminclaims") &&
                player.hasPermission(
                        "griefprevention.createclaims") &&
                ((playerData.lastClaim == null && playerData.getClaims().size() == 0) ||
                 (playerData.lastClaim != null && playerData.lastClaim.isNear(
                         player.getLocation(),
                         15
                 ))))
            {
                Long now = null;
                if (playerData.buildWarningTimestamp == null ||
                    (now = System.currentTimeMillis()) - playerData.buildWarningTimestamp >
                    600000)  //10 minute cooldown
                {
                    GriefPrevention.sendMessage(player, TextMode.WARNING, Messages.BuildingOutsideClaims);
                    playerData.warnedAboutBuildingOutsideClaims = true;

                    if (now == null) now = System.currentTimeMillis();
                    playerData.buildWarningTimestamp = now;

                    if (playerData.getClaims().size() < 2)
                    {
                        GriefPrevention.sendMessage(
                                player,
                                TextMode.INSTRUCTION,
                                Messages.SurvivalBasicsVideo2,
                                DataStore.SURVIVAL_VIDEO_URL
                        );
                    }

                    if (playerData.lastClaim != null)
                    {
                        BoundaryVisualization.visualizeClaim(
                                player,
                                playerData.lastClaim,
                                VisualizationType.CLAIM,
                                block
                        );
                    }
                }
            }
        }

        //warn players when they place TNT above sea level, since it doesn't destroy blocks there
        if (GriefPrevention.instance.config_blockSurfaceOtherExplosions && block.getType() == Material.TNT &&
            block.getWorld().getEnvironment() != World.Environment.NETHER &&
            block.getY() > GriefPrevention.instance.getSeaLevel(
                    block.getWorld()) - 5 && claim == null && playerData.siegeData == null)
        {
            GriefPrevention.sendMessage(player, TextMode.WARNING, Messages.NoTNTDamageAboveSeaLevel);
        }

        //warn players about disabled pistons outside of land claims
        if (GriefPrevention.instance.config_pistonMovement == PistonMode.CLAIMS_ONLY &&
            (block.getType() == Material.PISTON || block.getType() == Material.STICKY_PISTON) && claim == null)
        {
            GriefPrevention.sendMessage(player, TextMode.WARNING, Messages.NoPistonsOutsideClaims);
        }

        //limit active blocks in creative mode worlds
        if (!player.hasPermission("griefprevention.adminclaims") &&
            GriefPrevention.instance.creativeRulesApply(block.getLocation()) && isActiveBlock(
                block.getState()))
        {
            String noPlaceReason = claim.allowMoreActiveBlocks();
            if (noPlaceReason != null)
            {
                GriefPrevention.sendMessage(player, TextMode.ERROR, noPlaceReason);
                placeEvent.setCancelled(true);
            }
        }
    }

    private void denyConnectingDoubleChestsAcrossClaimBoundary(Claim claim, Block block, Player player)
    {
        UUID claimOwner = null;
        if (claim != null) claimOwner = claim.getOwnerID();

        // Check for double chests placed just outside the claim boundary
        if (block.getBlockData() instanceof Chest)
        {
            for (BlockFace face : HORIZONTAL_DIRECTIONS)
            {
                Block relative = block.getRelative(face);
                if (!(relative.getBlockData() instanceof Chest relativeChest)) continue;

                Claim relativeClaim = dataStore.getClaimAt(relative.getLocation(), true, claim);
                UUID relativeClaimOwner = relativeClaim == null ? null : relativeClaim.getOwnerID();

                // Chests outside claims should connect (both null)
                // and chests inside the same claim should connect (equal)
                if (Objects.equals(claimOwner, relativeClaimOwner)) break;

                // Change both chests to singular chests
                Chest chest = (Chest) block.getBlockData();
                chest.setType(Chest.Type.SINGLE);
                block.setBlockData(chest);

                relativeChest.setType(Chest.Type.SINGLE);
                relative.setBlockData(relativeChest);

                // Resend relative chest block to prevent visual bug
                player.sendBlockChange(relative.getLocation(), relativeChest);
                break;
            }
        }
    }

    private boolean doesAllowFireProximityInWorld(World world)
    {
        if (GriefPrevention.instance.pvpRulesApply(world))
        {
            return GriefPrevention.instance.getPluginConfig().getPvpConfiguration().isAllowFireNearPlayers();
        }
        else
        {
            return GriefPrevention.instance.getPluginConfig().getPvpConfiguration().isAllowFireNearPlayersNonPvP();
        }
    }
}
