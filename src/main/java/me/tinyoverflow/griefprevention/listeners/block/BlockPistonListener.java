package me.tinyoverflow.griefprevention.listeners.block;

import me.tinyoverflow.griefprevention.Claim;
import me.tinyoverflow.griefprevention.GriefPrevention;
import me.tinyoverflow.griefprevention.PistonMode;
import me.tinyoverflow.griefprevention.datastore.DataStore;
import me.tinyoverflow.griefprevention.utils.BoundingBox;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.PistonMoveReaction;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;

public class BlockPistonListener implements Listener {
    private final DataStore dataStore;

    public BlockPistonListener(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    // Prevent pistons pushing blocks into or out of claims.
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onBlockPistonExtend(BlockPistonExtendEvent event) {
        onPistonEvent(event, event.getBlocks(), false);
    }

    // Prevent pistons pulling blocks into or out of claims.
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onBlockPistonRetract(BlockPistonRetractEvent event) {
        onPistonEvent(event, event.getBlocks(), true);
    }

    // Handle piston push and pulls.
    private void onPistonEvent(BlockPistonEvent event, List<Block> blocks, boolean isRetract) {
        PistonMode pistonMode = GriefPrevention.instance.config_pistonMovement;
        // Return if piston movements are ignored.
        if (pistonMode == PistonMode.IGNORED) return;

        // Don't check in worlds where claims are not enabled.
        if (!GriefPrevention.instance.claimsEnabledForWorld(event.getBlock().getWorld())) return;

        BlockFace direction = event.getDirection();
        Block pistonBlock = event.getBlock();
        Claim pistonClaim = dataStore.getClaimAt(pistonBlock.getLocation(), false,
                pistonMode != PistonMode.CLAIMS_ONLY, null
        );

        // A claim is required, but the piston is not inside a claim.
        if (pistonClaim == null && pistonMode == PistonMode.CLAIMS_ONLY) {
            event.setCancelled(true);
            return;
        }

        // If no blocks are moving, quickly check if another claim's boundaries are violated.
        if (blocks.isEmpty()) {
            // No block and retraction is always safe.
            if (isRetract) return;

            Block invadedBlock = pistonBlock.getRelative(direction);
            Claim invadedClaim = dataStore.getClaimAt(invadedBlock.getLocation(), false,
                    pistonMode != PistonMode.CLAIMS_ONLY, pistonClaim
            );
            if (invadedClaim != null && (pistonClaim == null || !Objects.equals(
                    pistonClaim.getOwnerID(),
                    invadedClaim.getOwnerID()
            ))) {
                event.setCancelled(true);
            }

            return;
        }

        // Create bounding box for moved blocks.
        BoundingBox movedBlocks = BoundingBox.ofBlocks(blocks);
        // Expand to include invaded zone.
        movedBlocks.resize(direction, 1);

        if (pistonClaim != null) {
            // If blocks are all inside the same claim as the piston, allow.
            if (new BoundingBox(pistonClaim).contains(movedBlocks)) return;

            /*
             * In claims-only mode, all moved blocks must be inside the owning claim.
             * From BigScary:
             *  - Could push into another land claim, don't want to spend CPU checking for that
             *  - Push ice out, place torch, get water outside the claim
             */
            if (pistonMode == PistonMode.CLAIMS_ONLY) {
                event.setCancelled(true);
                return;
            }
        }

        // Check if blocks are in line vertically.
        if (movedBlocks.getLength() == 1 && movedBlocks.getWidth() == 1) {
            // Pulling up is always safe. The claim may not contain the area pulled from, but claims cannot stack.
            if (isRetract && direction == BlockFace.UP) return;

            // Pushing down is always safe. The claim may not contain the area pushed into, but claims cannot stack.
            if (!isRetract && direction == BlockFace.DOWN) return;
        }

        // Assemble list of potentially intersecting claims from chunks interacted with.
        ArrayList<Claim> intersectable = new ArrayList<>();
        int chunkXMax = movedBlocks.getMaxX() >> 4;
        int chunkZMax = movedBlocks.getMaxZ() >> 4;

        for (int chunkX = movedBlocks.getMinX() >> 4; chunkX <= chunkXMax; ++chunkX) {
            for (int chunkZ = movedBlocks.getMinZ() >> 4; chunkZ <= chunkZMax; ++chunkZ) {
                ArrayList<Claim> chunkClaims = dataStore.chunksToClaimsMap.get(DataStore.getChunkHash(chunkX, chunkZ));
                if (chunkClaims == null) continue;

                for (Claim claim : chunkClaims) {
                    // Ensure claim is not piston claim and is in same world.
                    if (pistonClaim != claim && pistonBlock.getWorld().equals(claim.getLesserBoundaryCorner().getWorld()))
                        intersectable.add(claim);
                }
            }
        }

        BiPredicate<Claim, BoundingBox> intersectionHandler;
        final Claim finalPistonClaim = pistonClaim;

        // Fast mode: Bounding box intersection always causes a conflict, even if blocks do not conflict.
        if (pistonMode == PistonMode.EVERYWHERE_SIMPLE) {
            intersectionHandler = (claim, claimBoundingBox) ->
            {
                // If owners are different, cancel.
                if (finalPistonClaim == null || !Objects.equals(finalPistonClaim.getOwnerID(), claim.getOwnerID())) {
                    event.setCancelled(true);
                    return true;
                }

                // Otherwise, proceed to next claim.
                return false;
            };
        }
        // Precise mode: Bounding box intersection may not yield a conflict. Individual blocks must be considered.
        else {
            // Set up list of affected blocks.
            HashSet<Block> checkBlocks = new HashSet<>(blocks);

            // Add all blocks that will be occupied after the shift.
            for (Block block : blocks)
                if (block.getPistonMoveReaction() != PistonMoveReaction.BREAK)
                    checkBlocks.add(block.getRelative(direction));

            intersectionHandler = (claim, claimBoundingBox) ->
            {
                // Ensure that the claim contains an affected block.
                if (checkBlocks.stream().noneMatch(claimBoundingBox::contains)) return false;

                // If pushing this block will change ownership, cancel the event and take away the piston (for performance reasons).
                if (finalPistonClaim == null || !Objects.equals(finalPistonClaim.getOwnerID(), claim.getOwnerID())) {
                    event.setCancelled(true);
                    if (GriefPrevention.instance.config_pistonExplosionSound) {
                        pistonBlock.getWorld().createExplosion(pistonBlock.getLocation(), 0);
                    }
                    pistonBlock.getWorld().dropItem(
                            pistonBlock.getLocation(),
                            new ItemStack(event.isSticky() ? Material.STICKY_PISTON : Material.PISTON)
                    );
                    pistonBlock.setType(Material.AIR);
                    return true;
                }

                // Otherwise, proceed to next claim.
                return false;
            };
        }

        for (Claim claim : intersectable) {
            BoundingBox claimBoundingBox = new BoundingBox(claim);

            // Ensure claim intersects with block bounding box.
            if (!claimBoundingBox.intersects(movedBlocks)) continue;

            // Do additional mode-based handling.
            if (intersectionHandler.test(claim, claimBoundingBox)) return;
        }
    }
}
