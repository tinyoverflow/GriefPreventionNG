package me.tinyoverflow.griefprevention.listeners.player;

import me.tinyoverflow.griefprevention.*;
import me.tinyoverflow.griefprevention.datastore.DataStore;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTakeLecternBookEvent;

import java.util.function.Supplier;

public class PlayerTakeLecternBookListener implements Listener {
    private final DataStore dataStore;

    public PlayerTakeLecternBookListener(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    void onTakeBook(PlayerTakeLecternBookEvent event) {
        Player player = event.getPlayer();
        PlayerData playerData = dataStore.getPlayerData(player.getUniqueId());
        Claim claim = dataStore.getClaimAt(event.getLectern().getLocation(), false, playerData.lastClaim);

        if (claim != null) {
            playerData.lastClaim = claim;
            Supplier<String> noContainerReason = claim.checkPermission(player, ClaimPermission.Inventory, event);

            if (noContainerReason != null) {
                event.setCancelled(true);
                player.closeInventory();
                GriefPrevention.sendMessage(player, TextMode.Err, noContainerReason.get());
            }
        }
    }
}
