package me.tinyoverflow.griefprevention.listeners.hanging;

import me.tinyoverflow.griefprevention.datastore.DataStore;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.hanging.HangingBreakEvent;

public class HangingBreakListener implements Listener {
    private final DataStore dataStore;

    public HangingBreakListener(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemFrameBrokenByBoat(final HangingBreakEvent event) {
        // Checks if the event is caused by physics - 90% of cases caused by a boat (other 10% would be a block,
        // however since it's in a claim, unless you use a TNT block we don't need to worry about it).
        if (event.getCause() != HangingBreakEvent.RemoveCause.PHYSICS) {
            return;
        }

        // Cancels the event if in a claim, as we can not efficiently retrieve the person/entity who broke the Item Frame/Hangable Item.
        if (dataStore.getClaimAt(event.getEntity().getLocation(), false, null) != null) {
            event.setCancelled(true);
        }
    }
}
