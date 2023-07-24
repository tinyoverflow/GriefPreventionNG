package me.tinyoverflow.griefprevention.listeners.inventory;

import me.tinyoverflow.griefprevention.PlayerData;
import me.tinyoverflow.griefprevention.datastore.DataStore;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.metadata.MetadataValue;

import java.util.List;
import java.util.UUID;

public class InventoryPickupItemListener implements Listener {
    private final DataStore dataStore;

    public InventoryPickupItemListener(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryPickupItem(InventoryPickupItemEvent event) {
        // Prevent hoppers from taking items dropped by players upon death.
        if (event.getInventory().getType() != InventoryType.HOPPER) {
            return;
        }

        List<MetadataValue> meta = event.getItem().getMetadata("GP_ITEMOWNER");
        // We only care about an item if it has been flagged as belonging to a player.
        if (meta.isEmpty()) {
            return;
        }

        UUID itemOwnerId = (UUID) meta.get(0).value();
        // Determine if the owner has unlocked their dropped items.
        // This first requires that the player is logged in.
        if (itemOwnerId != null && Bukkit.getServer().getPlayer(itemOwnerId) != null) {
            PlayerData itemOwner = dataStore.getPlayerData(itemOwnerId);
            // If locked, don't allow pickup
            if (!itemOwner.dropsAreUnlocked) {
                event.setCancelled(true);
            }
        }
    }
}
