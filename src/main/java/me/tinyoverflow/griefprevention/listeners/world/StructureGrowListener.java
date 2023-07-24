package me.tinyoverflow.griefprevention.listeners.world;

import me.tinyoverflow.griefprevention.Claim;
import me.tinyoverflow.griefprevention.GriefPrevention;
import me.tinyoverflow.griefprevention.datastore.DataStore;
import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.StructureGrowEvent;

public class StructureGrowListener implements Listener {
    private final DataStore dataStore;

    public StructureGrowListener(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    @EventHandler(ignoreCancelled = true)
    public void onTreeGrow(StructureGrowEvent growEvent) {
        //only take these potentially expensive steps if configured to do so
        if (!GriefPrevention.instance.config_limitTreeGrowth) return;

        //don't track in worlds where claims are not enabled
        if (!GriefPrevention.instance.claimsEnabledForWorld(growEvent.getWorld())) return;

        Location rootLocation = growEvent.getLocation();
        Claim rootClaim = dataStore.getClaimAt(rootLocation, false, null);
        String rootOwnerName = null;

        //who owns the spreading block, if anyone?
        if (rootClaim != null) {
            //tree growth in subdivisions is dependent on who owns the top level claim
            if (rootClaim.parent != null) rootClaim = rootClaim.parent;

            //if an administrative claim, just let the tree grow where it wants
            if (rootClaim.isAdminClaim()) return;

            //otherwise, note the owner of the claim
            rootOwnerName = rootClaim.getOwnerName();
        }

        //for each block growing
        for (int i = 0; i < growEvent.getBlocks().size(); i++) {
            BlockState block = growEvent.getBlocks().get(i);
            Claim blockClaim = dataStore.getClaimAt(block.getLocation(), false, rootClaim);

            //if it's growing into a claim
            if (blockClaim != null) {
                //if there's no owner for the new tree, or the owner for the new tree is different from the owner of the claim
                if (rootOwnerName == null || !rootOwnerName.equals(blockClaim.getOwnerName())) {
                    growEvent.getBlocks().remove(i--);
                }
            }
        }
    }
}
