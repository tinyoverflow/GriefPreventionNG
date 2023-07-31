package me.tinyoverflow.griefprevention.listeners.player;

import com.griefprevention.visualization.BoundaryVisualization;
import com.griefprevention.visualization.VisualizationType;
import me.tinyoverflow.griefprevention.*;
import me.tinyoverflow.griefprevention.datastore.DataStore;
import me.tinyoverflow.griefprevention.events.ClaimInspectionEvent;
import me.tinyoverflow.griefprevention.tasks.AutoExtendClaimTask;
import me.tinyoverflow.griefprevention.utils.BoundingBox;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BlockIterator;

import java.util.*;
import java.util.function.Supplier;

public class PlayerInteractListener implements Listener
{
    private final List<Material> leftClickWatchedMaterialList = List.of(
            Material.OAK_BUTTON,
            Material.SPRUCE_BUTTON,
            Material.BIRCH_BUTTON,
            Material.JUNGLE_BUTTON,
            Material.ACACIA_BUTTON,
            Material.DARK_OAK_BUTTON,
            Material.STONE_BUTTON,
            Material.LEVER,
            Material.REPEATER,
            Material.CAKE,
            Material.DRAGON_EGG
    );
    private final GriefPrevention plugin;
    private final DataStore dataStore;

    public PlayerInteractListener(GriefPrevention plugin, DataStore dataStore)
    {
        this.plugin = plugin;
        this.dataStore = dataStore;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    void onPlayerInteract(PlayerInteractEvent event)
    {
        //not interested in left-click-on-air actions
        Action action = event.getAction();
        if (action == Action.LEFT_CLICK_AIR) return;

        Player player = event.getPlayer();
        Block clickedBlock = event.getClickedBlock(); //null returned here means interacting with air

        Material clickedBlockType = null;
        if (clickedBlock != null) {
            clickedBlockType = clickedBlock.getType();
        }
        else {
            clickedBlockType = Material.AIR;
        }

        PlayerData playerData = null;

        //Turtle eggs
        if (action == Action.PHYSICAL) {
            if (clickedBlockType != Material.TURTLE_EGG) return;
            playerData = dataStore.getPlayerData(player.getUniqueId());
            Claim claim = dataStore.getClaimAt(clickedBlock.getLocation(), false, playerData.lastClaim);
            if (claim != null) {
                playerData.lastClaim = claim;

                Supplier<String> noAccessReason = claim.checkPermission(player, ClaimPermission.Build, event);
                if (noAccessReason != null) {
                    event.setCancelled(true);
                    return;
                }
            }
            return;
        }

        //don't care about left-clicking on most blocks, this is probably a break action
        if (action == Action.LEFT_CLICK_BLOCK && clickedBlock != null) {
            if (clickedBlock.getY() < clickedBlock.getWorld().getMaxHeight() - 1 ||
                event.getBlockFace() != BlockFace.UP)
            {
                Block adjacentBlock = clickedBlock.getRelative(event.getBlockFace());
                byte lightLevel = adjacentBlock.getLightFromBlocks();
                if (lightLevel == 15 && adjacentBlock.getType() == Material.FIRE) {
                    if (playerData == null) playerData = dataStore.getPlayerData(player.getUniqueId());
                    Claim claim = dataStore.getClaimAt(clickedBlock.getLocation(), false, playerData.lastClaim);
                    if (claim != null) {
                        playerData.lastClaim = claim;

                        Supplier<String> noBuildReason = claim.checkPermission(player, ClaimPermission.Build, event);
                        if (noBuildReason != null) {
                            event.setCancelled(true);
                            GriefPrevention.sendMessage(player, TextMode.ERROR, noBuildReason.get());
                            player.sendBlockChange(
                                    adjacentBlock.getLocation(),
                                    adjacentBlock.getType(),
                                    adjacentBlock.getData()
                            );
                            return;
                        }
                    }
                }
            }

            //exception for blocks on a specific watch list
            if (!leftClickWatchedMaterialList.contains(clickedBlockType)) {
                return;
            }
        }

        //apply rules for containers and crafting blocks
        if (clickedBlock != null &&
            plugin.getPluginConfig().getClaimConfiguration().getProtectionConfiguration().isLockContainersEnabled() &&
            (event.getAction() == Action.RIGHT_CLICK_BLOCK &&
             ((clickedBlock.getState() instanceof InventoryHolder && clickedBlock.getType() != Material.LECTERN) ||
              clickedBlockType == Material.ANVIL || clickedBlockType == Material.BEACON ||
              clickedBlockType == Material.BEE_NEST || clickedBlockType == Material.BEEHIVE ||
              clickedBlockType == Material.BELL || clickedBlockType == Material.CAKE ||
              clickedBlockType == Material.CARTOGRAPHY_TABLE || clickedBlockType == Material.CAULDRON ||
              clickedBlockType == Material.WATER_CAULDRON || clickedBlockType == Material.LAVA_CAULDRON ||
              clickedBlockType == Material.CAVE_VINES || clickedBlockType == Material.CAVE_VINES_PLANT ||
              clickedBlockType == Material.CHIPPED_ANVIL || clickedBlockType == Material.DAMAGED_ANVIL ||
              clickedBlockType == Material.GRINDSTONE || clickedBlockType == Material.JUKEBOX ||
              clickedBlockType == Material.LOOM || clickedBlockType == Material.PUMPKIN ||
              clickedBlockType == Material.RESPAWN_ANCHOR || clickedBlockType == Material.ROOTED_DIRT ||
              clickedBlockType == Material.STONECUTTER || clickedBlockType == Material.SWEET_BERRY_BUSH)))
        {
            if (playerData == null) playerData = dataStore.getPlayerData(player.getUniqueId());

            //block container use while under siege, so players can't hide items from attackers
            if (playerData.siegeData != null) {
                GriefPrevention.sendMessage(player, TextMode.ERROR, Messages.SiegeNoContainers);
                event.setCancelled(true);
                return;
            }

            //block container use during pvp combat, same reason
            if (playerData.inPvpCombat()) {
                GriefPrevention.sendMessage(player, TextMode.ERROR, Messages.PvPNoContainers);
                event.setCancelled(true);
                return;
            }

            //otherwise check permissions for the claim the player is in
            Claim claim = dataStore.getClaimAt(clickedBlock.getLocation(), false, playerData.lastClaim);
            if (claim != null) {
                playerData.lastClaim = claim;

                Supplier<String> noContainersReason = claim.checkPermission(player, ClaimPermission.Inventory, event);
                if (noContainersReason != null) {
                    event.setCancelled(true);
                    GriefPrevention.sendMessage(player, TextMode.ERROR, noContainersReason.get());
                    return;
                }
            }

            //if the event hasn't been cancelled, then the player is allowed to use the container
            //so drop any pvp protection
            if (playerData.pvpImmune) {
                playerData.pvpImmune = false;
                GriefPrevention.sendMessage(player, TextMode.WARNING, Messages.PvPImmunityEnd);
            }
        }

        //otherwise apply rules for doors and beds, if configured that way
        else if (clickedBlock != null &&

                 (plugin.getPluginConfig()
                          .getClaimConfiguration()
                          .getProtectionConfiguration()
                          .isLockWoodenDoorsEnabled() &&
                  Tag.DOORS.isTagged(
                          clickedBlockType) ||

                  plugin.getPluginConfig()
                          .getClaimConfiguration()
                          .getProtectionConfiguration()
                          .isLockSwitchesEnabled() &&
                  Tag.BEDS.isTagged(
                          clickedBlockType) ||

                  plugin.getPluginConfig()
                          .getClaimConfiguration()
                          .getProtectionConfiguration()
                          .isLockTrapDoorsEnabled() &&
                  Tag.TRAPDOORS.isTagged(
                          clickedBlockType) ||

                  plugin.getPluginConfig()
                          .getClaimConfiguration()
                          .getProtectionConfiguration()
                          .isLockLecternsEnabled() &&
                  clickedBlockType == Material.LECTERN ||

                  plugin.getPluginConfig()
                          .getClaimConfiguration()
                          .getProtectionConfiguration()
                          .isLockFenceGatesEnabled() &&
                  Tag.FENCE_GATES.isTagged(
                          clickedBlockType)))
        {
            if (playerData == null) playerData = dataStore.getPlayerData(player.getUniqueId());
            Claim claim = dataStore.getClaimAt(clickedBlock.getLocation(), false, playerData.lastClaim);
            if (claim != null) {
                playerData.lastClaim = claim;

                Supplier<String> noAccessReason = claim.checkPermission(player, ClaimPermission.Access, event);
                if (noAccessReason != null) {
                    event.setCancelled(true);
                    GriefPrevention.sendMessage(player, TextMode.ERROR, noAccessReason.get());
                }
            }
        }

        //otherwise apply rules for buttons and switches
        else if (clickedBlock != null &&
                 plugin.getPluginConfig()
                         .getClaimConfiguration()
                         .getProtectionConfiguration()
                         .isLockSwitchesEnabled() &&
                 (Tag.BUTTONS.isTagged(
                         clickedBlockType) || clickedBlockType == Material.LEVER))
        {
            if (playerData == null) playerData = dataStore.getPlayerData(player.getUniqueId());
            Claim claim = dataStore.getClaimAt(clickedBlock.getLocation(), false, playerData.lastClaim);
            if (claim != null) {
                playerData.lastClaim = claim;

                Supplier<String> noAccessReason = claim.checkPermission(player, ClaimPermission.Access, event);
                if (noAccessReason != null) {
                    event.setCancelled(true);
                    GriefPrevention.sendMessage(player, TextMode.ERROR, noAccessReason.get());
                }
            }
        }

        //otherwise apply rule for cake
        else if (clickedBlock != null &&
                 plugin.getPluginConfig()
                         .getClaimConfiguration()
                         .getProtectionConfiguration()
                         .isLockContainersEnabled() &&
                 (clickedBlockType == Material.CAKE || Tag.CANDLE_CAKES.isTagged(
                         clickedBlockType)))
        {
            if (playerData == null) playerData = dataStore.getPlayerData(player.getUniqueId());
            Claim claim = dataStore.getClaimAt(clickedBlock.getLocation(), false, playerData.lastClaim);
            if (claim != null) {
                playerData.lastClaim = claim;

                Supplier<String> noContainerReason = claim.checkPermission(player, ClaimPermission.Access, event);
                if (noContainerReason != null) {
                    event.setCancelled(true);
                    GriefPrevention.sendMessage(player, TextMode.ERROR, noContainerReason.get());
                }
            }
        }

        //apply rule for redstone and various decor blocks that require full trust
        else if (clickedBlock != null &&
                 (clickedBlockType == Material.NOTE_BLOCK || clickedBlockType == Material.REPEATER ||
                  clickedBlockType == Material.DRAGON_EGG || clickedBlockType == Material.DAYLIGHT_DETECTOR ||
                  clickedBlockType == Material.COMPARATOR || clickedBlockType == Material.REDSTONE_WIRE ||
                  Tag.FLOWER_POTS.isTagged(
                          clickedBlockType) || Tag.CANDLES.isTagged(clickedBlockType) ||
                  // Only block interaction with un-editable signs to allow command signs to function.
                  // TODO: When we are required to update Spigot API to 1.20 to support a change, swap to Sign#isWaxed
                  Tag.SIGNS.isTagged(clickedBlockType) && clickedBlock.getState() instanceof Sign sign &&
                  sign.isEditable()))
        {
            if (playerData == null) playerData = dataStore.getPlayerData(player.getUniqueId());
            Claim claim = dataStore.getClaimAt(clickedBlock.getLocation(), false, playerData.lastClaim);
            if (claim != null) {
                Supplier<String> noBuildReason = claim.checkPermission(player, ClaimPermission.Build, event);
                if (noBuildReason != null) {
                    event.setCancelled(true);
                    GriefPrevention.sendMessage(player, TextMode.ERROR, noBuildReason.get());
                }
            }
        }

        //otherwise handle right click (shovel, string, bonemeal) //RoboMWM: flint and steel
        else {
            //ignore all actions except right-click on a block or in the air
            if (action != Action.RIGHT_CLICK_BLOCK && action != Action.RIGHT_CLICK_AIR) return;

            //what's the player holding?
            EquipmentSlot hand = event.getHand();
            ItemStack itemInHand = plugin.getItemInHand(player, hand);
            Material materialInHand = itemInHand.getType();

            Set<Material> spawn_eggs = new HashSet<>();
            Set<Material> dyes = new HashSet<>();

            for (Material material : Material.values()) {
                if (material.isLegacy()) continue;
                if (material.name().endsWith("_SPAWN_EGG")) {
                    spawn_eggs.add(material);
                }
                else if (material.name().endsWith("_DYE")) dyes.add(material);
            }

            // Require build permission for items that may have an effect on the world when used.
            if (clickedBlock != null &&
                (materialInHand == Material.BONE_MEAL || materialInHand == Material.ARMOR_STAND || (spawn_eggs.contains(
                        materialInHand) &&
                                                                                                    GriefPrevention.instance.getPluginConfig()
                                                                                                            .getClaimConfiguration()
                                                                                                            .getProtectionConfiguration()
                                                                                                            .isPreventMonsterEggsEnabled()) ||
                 materialInHand == Material.END_CRYSTAL || materialInHand == Material.FLINT_AND_STEEL ||
                 materialInHand == Material.INK_SAC || materialInHand == Material.GLOW_INK_SAC ||
                 materialInHand == Material.HONEYCOMB || dyes.contains(
                        materialInHand)))
            {
                String noBuildReason = plugin.allowBuild(player, clickedBlock.getLocation(), clickedBlockType);
                if (noBuildReason != null) {
                    GriefPrevention.sendMessage(player, TextMode.ERROR, noBuildReason);
                    event.setCancelled(true);
                }

                return;
            }
            else if (clickedBlock != null && Tag.ITEMS_BOATS.isTagged(materialInHand)) {
                if (playerData == null) playerData = dataStore.getPlayerData(player.getUniqueId());
                Claim claim = dataStore.getClaimAt(clickedBlock.getLocation(), false, playerData.lastClaim);
                if (claim != null) {
                    Supplier<String> reason = claim.checkPermission(player, ClaimPermission.Inventory, event);
                    if (reason != null) {
                        GriefPrevention.sendMessage(player, TextMode.ERROR, reason.get());
                        event.setCancelled(true);
                    }
                }

                return;
            }

            //survival world minecart placement requires container trust, which is the permission required to remove the minecart later
            else if (clickedBlock != null &&
                     (materialInHand == Material.MINECART || materialInHand == Material.FURNACE_MINECART ||
                      materialInHand == Material.CHEST_MINECART || materialInHand == Material.TNT_MINECART ||
                      materialInHand == Material.HOPPER_MINECART) && !plugin.creativeRulesApply(
                    clickedBlock.getLocation()))
            {
                if (playerData == null) playerData = dataStore.getPlayerData(player.getUniqueId());
                Claim claim = dataStore.getClaimAt(clickedBlock.getLocation(), false, playerData.lastClaim);
                if (claim != null) {
                    Supplier<String> reason = claim.checkPermission(player, ClaimPermission.Inventory, event);
                    if (reason != null) {
                        GriefPrevention.sendMessage(player, TextMode.ERROR, reason.get());
                        event.setCancelled(true);
                    }
                }

                return;
            }

            //if it's a spawn egg, minecart, or boat, and this is a creative world, apply special rules
            else if (clickedBlock != null &&
                     (materialInHand == Material.MINECART || materialInHand == Material.FURNACE_MINECART ||
                      materialInHand == Material.CHEST_MINECART || materialInHand == Material.TNT_MINECART ||
                      materialInHand == Material.ARMOR_STAND || materialInHand == Material.ITEM_FRAME ||
                      materialInHand == Material.GLOW_ITEM_FRAME || spawn_eggs.contains(
                             materialInHand) || materialInHand == Material.INFESTED_STONE ||
                      materialInHand == Material.INFESTED_COBBLESTONE ||
                      materialInHand == Material.INFESTED_STONE_BRICKS ||
                      materialInHand == Material.INFESTED_MOSSY_STONE_BRICKS ||
                      materialInHand == Material.INFESTED_CRACKED_STONE_BRICKS ||
                      materialInHand == Material.INFESTED_CHISELED_STONE_BRICKS ||
                      materialInHand == Material.HOPPER_MINECART) && plugin.creativeRulesApply(
                    clickedBlock.getLocation()))
            {
                //player needs build permission at this location
                String noBuildReason = plugin.allowBuild(player, clickedBlock.getLocation(), Material.MINECART);
                if (noBuildReason != null) {
                    GriefPrevention.sendMessage(player, TextMode.ERROR, noBuildReason);
                    event.setCancelled(true);
                    return;
                }

                //enforce limit on total number of entities in this claim
                if (playerData == null) playerData = dataStore.getPlayerData(player.getUniqueId());
                Claim claim = dataStore.getClaimAt(clickedBlock.getLocation(), false, playerData.lastClaim);
                if (claim == null) return;

                String noEntitiesReason = claim.allowMoreEntities(false);
                if (noEntitiesReason != null) {
                    GriefPrevention.sendMessage(player, TextMode.ERROR, noEntitiesReason);
                    event.setCancelled(true);
                    return;
                }

                return;
            }

            //if he's investigating a claim
            else if (materialInHand ==
                     plugin.getPluginConfig().getClaimConfiguration().getToolsConfiguration().getInvestigationTool() &&
                     hand == EquipmentSlot.HAND)
            {
                //if claims are disabled in this world, do nothing
                if (!plugin.claimsEnabledForWorld(player.getWorld())) return;

                //if holding shift (sneaking), show all claims in area
                if (player.isSneaking() && player.hasPermission("griefprevention.visualizenearbyclaims")) {
                    //find nearby claims
                    Set<Claim> claims = dataStore.getNearbyClaims(player.getLocation());

                    // alert plugins of a claim inspection, return if cancelled
                    ClaimInspectionEvent inspectionEvent = new ClaimInspectionEvent(player, null, claims, true);
                    Bukkit.getPluginManager().callEvent(inspectionEvent);
                    if (inspectionEvent.isCancelled()) return;

                    //visualize boundaries
                    BoundaryVisualization.visualizeNearbyClaims(
                            player,
                            inspectionEvent.getClaims(),
                            player.getEyeLocation().getBlockY()
                    );
                    GriefPrevention.sendMessage(
                            player,
                            TextMode.INFO,
                            Messages.ShowNearbyClaims,
                            String.valueOf(claims.size())
                    );

                    return;
                }

                //FEATURE: shovel and stick can be used from a distance away
                if (action == Action.RIGHT_CLICK_AIR) {
                    //try to find a far away non-air block along line of sight
                    clickedBlock = getTargetBlock(player, 100);
                    clickedBlockType = clickedBlock.getType();
                }

                //if no block, stop here
                if (clickedBlock == null) {
                    return;
                }

                playerData = dataStore.getPlayerData(player.getUniqueId());

                //air indicates too far away
                if (clickedBlockType == Material.AIR) {
                    GriefPrevention.sendMessage(player, TextMode.ERROR, Messages.TooFarAway);

                    // Remove visualizations
                    playerData.setVisibleBoundaries(null);
                    return;
                }

                Claim claim = dataStore.getClaimAt(clickedBlock.getLocation(),
                        false /*ignore height*/,
                        playerData.lastClaim
                );

                //no claim case
                if (claim == null) {
                    // alert plugins of a claim inspection, return if cancelled
                    ClaimInspectionEvent inspectionEvent = new ClaimInspectionEvent(player, clickedBlock, null);
                    Bukkit.getPluginManager().callEvent(inspectionEvent);
                    if (inspectionEvent.isCancelled()) return;

                    GriefPrevention.sendMessage(player, TextMode.INFO, Messages.BlockNotClaimed);

                    playerData.setVisibleBoundaries(null);
                }

                //claim case
                else {
                    // alert plugins of a claim inspection, return if cancelled
                    ClaimInspectionEvent inspectionEvent = new ClaimInspectionEvent(player, clickedBlock, claim);
                    Bukkit.getPluginManager().callEvent(inspectionEvent);
                    if (inspectionEvent.isCancelled()) return;

                    playerData.lastClaim = claim;
                    GriefPrevention.sendMessage(player, TextMode.INFO, Messages.BlockClaimed, claim.getOwnerName());

                    //visualize boundary
                    BoundaryVisualization.visualizeClaim(
                            player,
                            claim,
                            com.griefprevention.visualization.VisualizationType.CLAIM
                    );

                    if (player.hasPermission("griefprevention.seeclaimsize")) {
                        GriefPrevention.sendMessage(
                                player,
                                TextMode.INFO,
                                "  " + claim.getWidth() + "x" + claim.getHeight() + "=" + claim.getArea()
                        );
                    }

                    //if permission, tell about the player's offline time
                    if (!claim.isAdminClaim() &&
                        (player.hasPermission("griefprevention.deleteclaims") || player.hasPermission(
                                "griefprevention.seeinactivity")))
                    {
                        if (claim.parent != null) {
                            claim = claim.parent;
                        }
                        Date lastLogin = new Date(Bukkit.getOfflinePlayer(claim.ownerID).getLastPlayed());
                        Date now = new Date();
                        long daysElapsed = (now.getTime() - lastLogin.getTime()) / (1000 * 60 * 60 * 24);

                        GriefPrevention.sendMessage(
                                player,
                                TextMode.INFO,
                                Messages.PlayerOfflineTime,
                                String.valueOf(daysElapsed)
                        );

                        //drop the data we just loaded, if the player isn't online
                        if (plugin.getServer().getPlayer(claim.ownerID) == null) {
                            dataStore.clearCachedPlayerData(claim.ownerID);
                        }
                    }
                }

                return;
            }

            //if it's a golden shovel
            else if (materialInHand !=
                     plugin.getPluginConfig().getClaimConfiguration().getToolsConfiguration().getModificationTool() ||
                     hand != EquipmentSlot.HAND)
            {
                return;
            }

            event.setCancelled(true);  //GriefPrevention exclusively reserves this tool  (e.g. no grass path creation for golden shovel)

            //disable golden shovel while under siege
            if (playerData == null) playerData = dataStore.getPlayerData(player.getUniqueId());
            if (playerData.siegeData != null) {
                GriefPrevention.sendMessage(player, TextMode.ERROR, Messages.SiegeNoShovel);
                event.setCancelled(true);
                return;
            }

            //FEATURE: shovel and stick can be used from a distance away
            if (action == Action.RIGHT_CLICK_AIR) {
                //try to find a far away non-air block along line of sight
                clickedBlock = getTargetBlock(player, 100);
                clickedBlockType = clickedBlock.getType();
            }

            //if no block, stop here
            if (clickedBlock == null) {
                return;
            }

            //can't use the shovel from too far away
            if (clickedBlockType == Material.AIR) {
                GriefPrevention.sendMessage(player, TextMode.ERROR, Messages.TooFarAway);
                return;
            }

            //if the player is in restore nature mode, do only that
            UUID playerID = player.getUniqueId();
            playerData = dataStore.getPlayerData(player.getUniqueId());
            if (playerData.toolMode == ToolMode.RESTORE_NATURE ||
                playerData.toolMode == ToolMode.RESTORE_NATURE_AGGRESSIVE)
            {
                //if the clicked block is in a claim, visualize that claim and deliver an error message
                Claim claim = dataStore.getClaimAt(clickedBlock.getLocation(), false, playerData.lastClaim);
                if (claim != null) {
                    GriefPrevention.sendMessage(player, TextMode.ERROR, Messages.BlockClaimed, claim.getOwnerName());
                    BoundaryVisualization.visualizeClaim(
                            player,
                            claim,
                            com.griefprevention.visualization.VisualizationType.CONFLICT_ZONE,
                            clickedBlock
                    );
                    return;
                }

                //figure out which chunk to repair
                Chunk chunk = player.getWorld().getChunkAt(clickedBlock.getLocation());
                //start the repair process

                //set boundaries for processing
                int miny = clickedBlock.getY();

                //if not in aggressive mode, extend the selection down to a little below sea level
                if (!(playerData.toolMode == ToolMode.RESTORE_NATURE_AGGRESSIVE)) {
                    if (miny > plugin.getSeaLevel(chunk.getWorld()) - 10) {
                        miny = plugin.getSeaLevel(chunk.getWorld()) - 10;
                    }
                }

                plugin.restoreChunk(
                        chunk,
                        miny,
                        playerData.toolMode == ToolMode.RESTORE_NATURE_AGGRESSIVE,
                        0,
                        player
                );

                return;
            }

            //if in restore nature fill mode
            if (playerData.toolMode == ToolMode.RESTORE_NATURE_FILL) {
                ArrayList<Material> allowedFillBlocks = new ArrayList<>();
                World.Environment environment = clickedBlock.getWorld().getEnvironment();
                if (environment == World.Environment.NETHER) {
                    allowedFillBlocks.add(Material.NETHERRACK);
                }
                else if (environment == World.Environment.THE_END) {
                    allowedFillBlocks.add(Material.END_STONE);
                }
                else {
                    allowedFillBlocks.add(Material.GRASS);
                    allowedFillBlocks.add(Material.DIRT);
                    allowedFillBlocks.add(Material.STONE);
                    allowedFillBlocks.add(Material.SAND);
                    allowedFillBlocks.add(Material.SANDSTONE);
                    allowedFillBlocks.add(Material.ICE);
                }

                Block centerBlock = clickedBlock;

                int maxHeight = centerBlock.getY();
                int minx = centerBlock.getX() - playerData.fillRadius;
                int maxx = centerBlock.getX() + playerData.fillRadius;
                int minz = centerBlock.getZ() - playerData.fillRadius;
                int maxz = centerBlock.getZ() + playerData.fillRadius;
                int minHeight = maxHeight - 10;
                minHeight = Math.max(minHeight, clickedBlock.getWorld().getMinHeight());

                Claim cachedClaim = null;
                for (int x = minx; x <= maxx; x++) {
                    for (int z = minz; z <= maxz; z++) {
                        //circular brush
                        Location location = new Location(centerBlock.getWorld(), x, centerBlock.getY(), z);
                        if (location.distance(centerBlock.getLocation()) > playerData.fillRadius) continue;

                        //default fill block is initially the first from the allowed fill blocks list above
                        Material defaultFiller = allowedFillBlocks.get(0);

                        //prefer to use the block the player clicked on, if it's an acceptable fill block
                        if (allowedFillBlocks.contains(centerBlock.getType())) {
                            defaultFiller = centerBlock.getType();
                        }

                        //if the player clicks on water, try to sink through the water to find something underneath that's useful for a filler
                        else if (centerBlock.getType() == Material.WATER) {
                            Block block = centerBlock.getWorld().getBlockAt(centerBlock.getLocation());
                            while (!allowedFillBlocks.contains(block.getType()) &&
                                   block.getY() > centerBlock.getY() - 10) {
                                block = block.getRelative(BlockFace.DOWN);
                            }
                            if (allowedFillBlocks.contains(block.getType())) {
                                defaultFiller = block.getType();
                            }
                        }

                        //fill bottom to top
                        for (int y = minHeight; y <= maxHeight; y++) {
                            Block block = centerBlock.getWorld().getBlockAt(x, y, z);

                            //respect claims
                            Claim claim = dataStore.getClaimAt(block.getLocation(), false, cachedClaim);
                            if (claim != null) {
                                cachedClaim = claim;
                                break;
                            }

                            //only replace air, spilling water, snow, long grass
                            if (block.getType() == Material.AIR || block.getType() == Material.SNOW ||
                                (block.getType() == Material.WATER &&
                                 ((Levelled) block.getBlockData()).getLevel() != 0) ||
                                block.getType() == Material.GRASS)
                            {
                                //if the top level, always use the default filler picked above
                                if (y == maxHeight) {
                                    block.setType(defaultFiller);
                                }

                                //otherwise look to neighbors for an appropriate fill block
                                else {
                                    Block eastBlock = block.getRelative(BlockFace.EAST);
                                    Block westBlock = block.getRelative(BlockFace.WEST);
                                    Block northBlock = block.getRelative(BlockFace.NORTH);
                                    Block southBlock = block.getRelative(BlockFace.SOUTH);

                                    //first, check lateral neighbors (ideally, want to keep natural layers)
                                    if (allowedFillBlocks.contains(eastBlock.getType())) {
                                        block.setType(eastBlock.getType());
                                    }
                                    else if (allowedFillBlocks.contains(westBlock.getType())) {
                                        block.setType(westBlock.getType());
                                    }
                                    else if (allowedFillBlocks.contains(northBlock.getType())) {
                                        block.setType(northBlock.getType());
                                    }
                                    else if (allowedFillBlocks.contains(southBlock.getType())) {
                                        block.setType(southBlock.getType());
                                    }

                                    //if all else fails, use the default filler selected above
                                    else {
                                        block.setType(defaultFiller);
                                    }
                                }
                            }
                        }
                    }
                }

                return;
            }

            //if the player doesn't have claims permission, don't do anything
            if (!player.hasPermission("griefprevention.createclaims")) {
                GriefPrevention.sendMessage(player, TextMode.ERROR, Messages.NoCreateClaimPermission);
                return;
            }

            //if he's resizing a claim and that claim hasn't been deleted since he started resizing it
            if (playerData.claimResizing != null && playerData.claimResizing.inDataStore) {
                if (clickedBlock.getLocation().equals(playerData.lastShovelLocation)) return;

                //figure out what the coords of his new claim would be
                int newx1, newx2, newz1, newz2, newy1, newy2;
                if (playerData.lastShovelLocation.getBlockX() ==
                    playerData.claimResizing.getLesserBoundaryCorner().getBlockX())
                {
                    newx1 = clickedBlock.getX();
                    newx2 = playerData.claimResizing.getGreaterBoundaryCorner().getBlockX();
                }
                else {
                    newx1 = playerData.claimResizing.getLesserBoundaryCorner().getBlockX();
                    newx2 = clickedBlock.getX();
                }

                if (playerData.lastShovelLocation.getBlockZ() ==
                    playerData.claimResizing.getLesserBoundaryCorner().getBlockZ())
                {
                    newz1 = clickedBlock.getZ();
                    newz2 = playerData.claimResizing.getGreaterBoundaryCorner().getBlockZ();
                }
                else {
                    newz1 = playerData.claimResizing.getLesserBoundaryCorner().getBlockZ();
                    newz2 = clickedBlock.getZ();
                }

                newy1 = playerData.claimResizing.getLesserBoundaryCorner().getBlockY();
                newy2 = clickedBlock.getY() -
                        plugin.getPluginConfig()
                                .getClaimConfiguration()
                                .getCreationConfiguration().extendIntoGroundDistance;

                dataStore.resizeClaimWithChecks(player, playerData, newx1, newx2, newy1, newy2, newz1, newz2);

                return;
            }

            //otherwise, since not currently resizing a claim, must be starting a resize, creating a new claim, or creating a subdivision
            Claim claim = dataStore.getClaimAt(clickedBlock.getLocation(),
                    true /*ignore height*/,
                    playerData.lastClaim
            );

            //if within an existing claim, he's not creating a new one
            if (claim != null) {
                //if the player has permission to edit the claim or subdivision
                Supplier<String> noEditReason = claim.checkPermission(
                        player,
                        ClaimPermission.Edit,
                        event,
                        () -> plugin.dataStore.getMessage(
                                Messages.CreateClaimFailOverlapOtherPlayer,
                                claim.getOwnerName()
                        )
                );
                if (noEditReason == null) {
                    //if he clicked on a corner, start resizing it
                    if ((clickedBlock.getX() == claim.getLesserBoundaryCorner().getBlockX() ||
                         clickedBlock.getX() == claim.getGreaterBoundaryCorner().getBlockX()) &&
                        (clickedBlock.getZ() == claim.getLesserBoundaryCorner().getBlockZ() ||
                         clickedBlock.getZ() == claim.getGreaterBoundaryCorner().getBlockZ()))
                    {
                        playerData.claimResizing = claim;
                        playerData.lastShovelLocation = clickedBlock.getLocation();
                        GriefPrevention.sendMessage(player, TextMode.INSTRUCTION, Messages.ResizeStart);
                    }

                    //if he didn't click on a corner and is in subdivision mode, he's creating a new subdivision
                    else if (playerData.toolMode == ToolMode.SUBDIVIDE) {
                        //if it's the first click, he's trying to start a new subdivision
                        if (playerData.lastShovelLocation == null) {
                            //if the clicked claim was a subdivision, tell him he can't start a new subdivision here
                            if (claim.parent != null) {
                                GriefPrevention.sendMessage(
                                        player,
                                        TextMode.ERROR,
                                        Messages.ResizeFailOverlapSubdivision
                                );
                            }

                            //otherwise start a new subdivision
                            else {
                                GriefPrevention.sendMessage(player, TextMode.INSTRUCTION, Messages.SubdivisionStart);
                                playerData.lastShovelLocation = clickedBlock.getLocation();
                                playerData.claimSubdividing = claim;
                            }
                        }

                        //otherwise, he's trying to finish creating a subdivision by setting the other boundary corner
                        else {
                            //if last shovel location was in a different world, assume the player is starting the create-claim workflow over
                            if (!playerData.lastShovelLocation.getWorld().equals(clickedBlock.getWorld())) {
                                playerData.lastShovelLocation = null;
                                onPlayerInteract(event);
                                return;
                            }

                            //try to create a new claim (will return null if this subdivision overlaps another)
                            CreateClaimResult result = dataStore.createClaim(
                                    player.getWorld(),
                                    playerData.lastShovelLocation.getBlockX(),
                                    clickedBlock.getX(),
                                    playerData.lastShovelLocation.getBlockY() -
                                    plugin.getPluginConfig()
                                            .getClaimConfiguration()
                                            .getCreationConfiguration().extendIntoGroundDistance,
                                    clickedBlock.getY() -
                                    plugin.getPluginConfig()
                                            .getClaimConfiguration()
                                            .getCreationConfiguration().extendIntoGroundDistance,
                                    playerData.lastShovelLocation.getBlockZ(),
                                    clickedBlock.getZ(),
                                    null,
                                    //owner is not used for subdivisions
                                    playerData.claimSubdividing,
                                    null,
                                    player
                            );

                            //if it didn't succeed, tell the player why
                            if (!result.succeeded || result.claim == null) {
                                if (result.claim != null) {
                                    GriefPrevention.sendMessage(
                                            player,
                                            TextMode.ERROR,
                                            Messages.CreateSubdivisionOverlap
                                    );
                                    BoundaryVisualization.visualizeClaim(
                                            player,
                                            result.claim,
                                            com.griefprevention.visualization.VisualizationType.CONFLICT_ZONE,
                                            clickedBlock
                                    );
                                }
                                else {
                                    GriefPrevention.sendMessage(
                                            player,
                                            TextMode.ERROR,
                                            Messages.CreateClaimFailOverlapRegion
                                    );
                                }

                                return;
                            }

                            //otherwise, advise him on the /trust command and show him his new subdivision
                            else {
                                GriefPrevention.sendMessage(player, TextMode.SUCCESS, Messages.SubdivisionSuccess);
                                BoundaryVisualization.visualizeClaim(
                                        player,
                                        result.claim,
                                        com.griefprevention.visualization.VisualizationType.CLAIM,
                                        clickedBlock
                                );
                                playerData.lastShovelLocation = null;
                                playerData.claimSubdividing = null;
                            }
                        }
                    }

                    //otherwise tell him he can't create a claim here, and show him the existing claim
                    //also advise him to consider /abandonclaim or resizing the existing claim
                    else {
                        GriefPrevention.sendMessage(player, TextMode.ERROR, Messages.CreateClaimFailOverlap);
                        BoundaryVisualization.visualizeClaim(
                                player,
                                claim,
                                com.griefprevention.visualization.VisualizationType.CLAIM,
                                clickedBlock
                        );
                    }
                }

                //otherwise tell the player he can't claim here because it's someone else's claim, and show him the claim
                else {
                    GriefPrevention.sendMessage(player, TextMode.ERROR, noEditReason.get());
                    BoundaryVisualization.visualizeClaim(
                            player,
                            claim,
                            com.griefprevention.visualization.VisualizationType.CONFLICT_ZONE,
                            clickedBlock
                    );
                }

                return;
            }

            //otherwise, the player isn't in an existing claim!

            //if he hasn't already start a claim with a previous shovel action
            Location lastShovelLocation = playerData.lastShovelLocation;
            if (lastShovelLocation == null) {
                //if claims are not enabled in this world and it's not an administrative claim, display an error message and stop
                if (!plugin.claimsEnabledForWorld(player.getWorld())) {
                    GriefPrevention.sendMessage(player, TextMode.ERROR, Messages.ClaimsDisabledWorld);
                    return;
                }

                //if he's at the claim count per player limit already and doesn't have permission to bypass, display an error message
                if (plugin.getPluginConfig().getClaimConfiguration().getCreationConfiguration().maximumClaims > 0 &&
                    !player.hasPermission(
                            "griefprevention.overrideclaimcountlimit") && playerData.getClaims().size() >=
                                                                          plugin.getPluginConfig()
                                                                                  .getClaimConfiguration()
                                                                                  .getCreationConfiguration().maximumClaims)
                {
                    GriefPrevention.sendMessage(
                            player,
                            TextMode.ERROR,
                            Messages.ClaimCreationFailedOverClaimCountLimit
                    );
                    return;
                }

                //remember it, and start him on the new claim
                playerData.lastShovelLocation = clickedBlock.getLocation();
                GriefPrevention.sendMessage(player, TextMode.INSTRUCTION, Messages.ClaimStart);

                //show him where he's working
                BoundaryVisualization.visualizeArea(
                        player,
                        new BoundingBox(clickedBlock),
                        com.griefprevention.visualization.VisualizationType.INITIALIZE_ZONE
                );
            }

            //otherwise, he's trying to finish creating a claim by setting the other boundary corner
            else {
                //if last shovel location was in a different world, assume the player is starting the create-claim workflow over
                if (!lastShovelLocation.getWorld().equals(clickedBlock.getWorld())) {
                    playerData.lastShovelLocation = null;
                    onPlayerInteract(event);
                    return;
                }

                //apply pvp rule
                if (playerData.inPvpCombat()) {
                    GriefPrevention.sendMessage(player, TextMode.ERROR, Messages.NoClaimDuringPvP);
                    return;
                }

                //apply minimum claim dimensions rule
                int newClaimWidth = Math.abs(playerData.lastShovelLocation.getBlockX() - clickedBlock.getX()) + 1;
                int newClaimHeight = Math.abs(playerData.lastShovelLocation.getBlockZ() - clickedBlock.getZ()) + 1;

                if (playerData.toolMode != ToolMode.ADMIN) {
                    if (newClaimWidth <
                        plugin.getPluginConfig().getClaimConfiguration().getCreationConfiguration().minimumWidth ||
                        newClaimHeight <
                        plugin.getPluginConfig().getClaimConfiguration().getCreationConfiguration().minimumWidth)
                    {
                        //this IF block is a workaround for craftbukkit bug which fires two events for one interaction
                        if (newClaimWidth != 1 && newClaimHeight != 1) {
                            GriefPrevention.sendMessage(
                                    player,
                                    TextMode.ERROR,
                                    Messages.NewClaimTooNarrow,
                                    String.valueOf(plugin.getPluginConfig()
                                            .getClaimConfiguration()
                                            .getCreationConfiguration().minimumWidth)
                            );
                        }
                        return;
                    }

                    int newArea = newClaimWidth * newClaimHeight;
                    if (newArea <
                        plugin.getPluginConfig().getClaimConfiguration().getCreationConfiguration().minimumArea)
                    {
                        if (newArea != 1) {
                            GriefPrevention.sendMessage(
                                    player,
                                    TextMode.ERROR,
                                    Messages.ResizeClaimInsufficientArea,
                                    String.valueOf(plugin.getPluginConfig()
                                            .getClaimConfiguration()
                                            .getCreationConfiguration().minimumArea)
                            );
                        }

                        return;
                    }
                }

                //if not an administrative claim, verify the player has enough claim blocks for this new claim
                if (playerData.toolMode != ToolMode.ADMIN) {
                    int newClaimArea = newClaimWidth * newClaimHeight;
                    int remainingBlocks = playerData.getRemainingClaimBlocks();
                    if (newClaimArea > remainingBlocks) {
                        GriefPrevention.sendMessage(
                                player,
                                TextMode.ERROR,
                                Messages.CreateClaimInsufficientBlocks,
                                String.valueOf(newClaimArea - remainingBlocks)
                        );
                        plugin.dataStore.tryAdvertiseAdminAlternatives(player);
                        return;
                    }
                }
                else {
                    playerID = null;
                }

                //try to create a new claim
                CreateClaimResult result = dataStore.createClaim(
                        player.getWorld(),
                        lastShovelLocation.getBlockX(),
                        clickedBlock.getX(),
                        lastShovelLocation.getBlockY() -
                        plugin.getPluginConfig()
                                .getClaimConfiguration()
                                .getCreationConfiguration().extendIntoGroundDistance,
                        clickedBlock.getY() -
                        plugin.getPluginConfig()
                                .getClaimConfiguration()
                                .getCreationConfiguration().extendIntoGroundDistance,
                        lastShovelLocation.getBlockZ(),
                        clickedBlock.getZ(),
                        playerID,
                        null,
                        null,
                        player
                );

                //if it didn't succeed, tell the player why
                if (!result.succeeded || result.claim == null) {
                    if (result.claim != null) {
                        GriefPrevention.sendMessage(player, TextMode.ERROR, Messages.CreateClaimFailOverlapShort);
                        BoundaryVisualization.visualizeClaim(
                                player,
                                result.claim,
                                com.griefprevention.visualization.VisualizationType.CONFLICT_ZONE,
                                clickedBlock
                        );
                    }
                    else {
                        GriefPrevention.sendMessage(player, TextMode.ERROR, Messages.CreateClaimFailOverlapRegion);
                    }
                }

                //otherwise, advise him on the /trust command and show him his new claim
                else {
                    GriefPrevention.sendMessage(player, TextMode.SUCCESS, Messages.CreateClaimSuccess);
                    BoundaryVisualization.visualizeClaim(player, result.claim, VisualizationType.CLAIM, clickedBlock);
                    playerData.lastShovelLocation = null;

                    //if it's a big claim, tell the player about subdivisions
                    if (!player.hasPermission("griefprevention.adminclaims") && result.claim.getArea() >= 1000) {
                        GriefPrevention.sendMessage(player, TextMode.INFO, Messages.BecomeMayor, 200L);
                        GriefPrevention.sendMessage(
                                player,
                                TextMode.INSTRUCTION,
                                Messages.SubdivisionVideo2,
                                201L,
                                DataStore.SUBDIVISION_VIDEO_URL
                        );
                    }

                    AutoExtendClaimTask.scheduleAsync(result.claim);
                }
            }
        }
    }

    private Block getTargetBlock(Player player, int maxDistance) throws IllegalStateException
    {
        Location eye = player.getEyeLocation();
        Material eyeMaterial = eye.getBlock().getType();
        boolean passThroughWater = (eyeMaterial == Material.WATER);
        BlockIterator iterator = new BlockIterator(player.getLocation(), player.getEyeHeight(), maxDistance);
        Block result = player.getLocation().getBlock().getRelative(BlockFace.UP);
        while (iterator.hasNext()) {
            result = iterator.next();
            Material type = result.getType();
            if (type != Material.AIR && (!passThroughWater || type != Material.WATER) && type != Material.GRASS &&
                type != Material.SNOW)
            {
                return result;
            }
        }

        return result;
    }
}
