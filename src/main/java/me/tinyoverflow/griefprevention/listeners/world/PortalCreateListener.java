package me.tinyoverflow.griefprevention.listeners.world;

import me.tinyoverflow.griefprevention.Claim;
import me.tinyoverflow.griefprevention.ClaimPermission;
import me.tinyoverflow.griefprevention.GriefPrevention;
import me.tinyoverflow.griefprevention.TextMode;
import me.tinyoverflow.griefprevention.datastore.DataStore;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.PortalCreateEvent;

import java.util.function.Supplier;

public class PortalCreateListener implements Listener {
    private final DataStore dataStore;

    public PortalCreateListener(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    @EventHandler(ignoreCancelled = true)
    public void onNetherPortalCreate(final PortalCreateEvent event) {
        if (event.getReason() != PortalCreateEvent.CreateReason.NETHER_PAIR) {
            return;
        }

        // Ignore this event if preventNonPlayerCreatedPortals config option is disabled, and we don't know the entity.
        if (!(event.getEntity() instanceof Player) && !GriefPrevention.instance.getPluginConfig().getClaimConfiguration().getProtectionConfiguration().isPreventNonPlayerPortalsEnabled()) {
            return;
        }

        for (BlockState blockState : event.getBlocks()) {
            Claim claim = dataStore.getClaimAt(blockState.getLocation(), false, null);
            if (claim != null) {
                if (event.getEntity() instanceof Player player) {
                    Supplier<String> noPortalReason = claim.checkPermission(player, ClaimPermission.Build, event);

                    if (noPortalReason != null) {
                        event.setCancelled(true);
                        GriefPrevention.sendMessage(player, TextMode.Err, noPortalReason.get());
                        return;
                    }
                }
                else {
                    // Cancels the event if in a claim, as we can not efficiently retrieve the person/entity who created the portal.
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }
}
