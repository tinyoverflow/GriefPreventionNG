package me.tinyoverflow.griefprevention.listeners.block;

import me.tinyoverflow.griefprevention.Claim;
import me.tinyoverflow.griefprevention.GriefPrevention;
import me.tinyoverflow.griefprevention.datastore.DataStore;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Dispenser;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class BlockDispenseListener implements Listener {
    private final DataStore dataStore;

    public BlockDispenseListener(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onDispense(org.bukkit.event.block.BlockDispenseEvent dispenseEvent) {
        //don't track in worlds where claims are not enabled
        if (!GriefPrevention.instance.claimsEnabledForWorld(dispenseEvent.getBlock().getWorld())) return;

        //from where?
        Block fromBlock = dispenseEvent.getBlock();
        BlockData fromData = fromBlock.getBlockData();
        if (!(fromData instanceof Dispenser dispenser)) return;

        //to where?
        Block toBlock = fromBlock.getRelative(dispenser.getFacing());
        Claim fromClaim = dataStore.getClaimAt(fromBlock.getLocation(), false, null);
        Claim toClaim = dataStore.getClaimAt(toBlock.getLocation(), false, fromClaim);

        //into wilderness is NOT OK in creative mode worlds
        Material materialDispensed = dispenseEvent.getItem().getType();
        if ((materialDispensed == Material.WATER_BUCKET || materialDispensed == Material.LAVA_BUCKET) && GriefPrevention.instance.creativeRulesApply(
                dispenseEvent.getBlock().getLocation()) && toClaim == null) {
            dispenseEvent.setCancelled(true);
            return;
        }

        //wilderness to wilderness is OK
        if (fromClaim == null && toClaim == null) return;

        //within claim is OK
        if (fromClaim == toClaim) return;

        //everything else is NOT OK
        dispenseEvent.setCancelled(true);
    }
}
