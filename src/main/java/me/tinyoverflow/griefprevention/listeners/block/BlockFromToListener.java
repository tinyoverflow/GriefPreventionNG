package me.tinyoverflow.griefprevention.listeners.block;

import me.tinyoverflow.griefprevention.Claim;
import me.tinyoverflow.griefprevention.GriefPrevention;
import me.tinyoverflow.griefprevention.datastore.DataStore;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;

import java.util.Objects;

public class BlockFromToListener implements Listener {
    private final DataStore dataStore;
    private Claim lastSpreadFromClaim = null;
    private Claim lastSpreadToClaim = null;

    public BlockFromToListener(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onBlockFromTo(BlockFromToEvent spreadEvent) {
        //always allow fluids to flow straight down
        if (spreadEvent.getFace() == BlockFace.DOWN) return;

        //don't track in worlds where claims are not enabled
        if (!GriefPrevention.instance.claimsEnabledForWorld(spreadEvent.getBlock().getWorld())) return;

        //where from and where to?
        Location fromLocation = spreadEvent.getBlock().getLocation();
        Location toLocation = spreadEvent.getToBlock().getLocation();
        boolean isInCreativeRulesWorld = GriefPrevention.instance.creativeRulesApply(toLocation);
        Claim fromClaim = dataStore.getClaimAt(fromLocation, false, lastSpreadFromClaim);
        Claim toClaim = dataStore.getClaimAt(toLocation, false, lastSpreadToClaim);

        //due to the nature of what causes this event (fluid flow/spread),
        //we'll probably run similar checks for the same pair of claims again,
        //so we cache them to use in claim lookup later
        lastSpreadFromClaim = fromClaim;
        lastSpreadToClaim = toClaim;

        if (!isFluidFlowAllowed(fromClaim, toClaim, isInCreativeRulesWorld)) {
            spreadEvent.setCancelled(true);
        }
    }

    /**
     * Determines whether fluid flow is allowed between two claims.
     *
     * @param from The claim at the source location of the fluid flow, or null if it's wilderness.
     * @param to The claim at the destination location of the fluid flow, or null if it's wilderness.
     * @param creativeRulesApply Whether creative rules apply to the world where claims are located.
     * @return `true` if fluid flow is allowed, `false` otherwise.
     */
    private boolean isFluidFlowAllowed(Claim from, Claim to, boolean creativeRulesApply)
    {
        // Special case: if in a world with creative rules,
        // don't allow fluids to flow into wilderness.
        if (creativeRulesApply && to == null) return false;

        // The fluid flow should be allowed or denied based on the specific combination
        // of source and destination claim types. The following matrix outlines these
        // combinations and indicates whether fluid flow should be permitted:
        //
        //   +--------------+------+----------+----------+----------+--------------+----------+---------+
        //   | From \ To    | Wild | Claim A1 | Sub A1_1 | Sub A1_2 | Sub A1_3 (R) | Claim A2 | Claim B |
        //   +--------------+------+----------+----------+----------+--------------+----------+---------+
        //   | Wild         | Yes  | -        | -        | -        | -            | -        | -       |
        //   +--------------+------+----------+----------+----------+--------------+----------+---------+
        //   | Claim A1     | Yes  | Yes      | Yes      | Yes      | -            | Yes      | -       |
        //   +--------------+------+----------+----------+----------+--------------+----------+---------+
        //   | Sub A1_1     | Yes  | -        | Yes      | -        | -            | -        | -       |
        //   +--------------+------+----------+----------+----------+--------------+----------+---------+
        //   | Sub A1_2     | Yes  | -        | -        | Yes      | -            | -        | -       |
        //   +--------------+------+----------+----------+----------+--------------+----------+---------+
        //   | Sub A1_3 (R) | Yes  | -        | -        | -        | Yes          | -        | -       |
        //   +--------------+------+----------+----------+----------+--------------+----------+---------+
        //   | Claim A2     | Yes  | Yes      | -        | -        | -            | Yes      | -       |
        //   +--------------+------+----------+----------+----------+--------------+----------+---------+
        //   | Claim B      | Yes  | -        | -        | -        | -            | -        | Yes     |
        //   +--------------+------+----------+----------+----------+--------------+----------+---------+
        //
        //   Legend:
        //     Wild = wilderness
        //     Claim A* = claim owned by player A
        //     Sub A*_* = subdivision of Claim A*
        //     (R) = Restricted subdivision
        //     Claim B = claim owned by player B
        //     Yes = fluid flow allowed
        //     - = fluid flow not allowed

        boolean fromWilderness = from == null;
        boolean toWilderness = to == null;
        boolean sameClaim = from != null && to != null && Objects.equals(from.getID(), to.getID());
        boolean sameOwner = from != null && to != null && Objects.equals(from.getOwnerID(), to.getOwnerID());
        boolean isToSubdivision = to != null && to.parent != null;
        boolean isToRestrictedSubdivision = isToSubdivision && to.getSubclaimRestrictions();
        boolean isFromSubdivision = from != null && from.parent != null;

        if (toWilderness) return true;
        if (fromWilderness) return false;
        if (sameClaim) return true;
        if (isFromSubdivision) return false;
        if (isToSubdivision) return !isToRestrictedSubdivision;
        return sameOwner;
    }
}
