package me.tinyoverflow.griefprevention.listeners.player;

import me.tinyoverflow.griefprevention.*;
import me.tinyoverflow.griefprevention.datastore.DataStore;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEggThrowEvent;

import java.util.function.Supplier;

public class PlayerEggThrowListener implements Listener
{
    private final GriefPrevention instance;
    private final DataStore dataStore;

    public PlayerEggThrowListener(GriefPrevention instance, DataStore dataStore)
    {
        this.instance = instance;
        this.dataStore = dataStore;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerThrowEgg(PlayerEggThrowEvent event)
    {
        Player player = event.getPlayer();
        PlayerData playerData = dataStore.getPlayerData(player.getUniqueId());
        Claim claim = dataStore.getClaimAt(event.getEgg().getLocation(), false, playerData.lastClaim);

        //allow throw egg if player is in ignore claims mode
        if (playerData.ignoreClaims || claim == null) return;

        Supplier<String> failureReason = claim.checkPermission(player, ClaimPermission.Inventory, event);
        if (failureReason != null)
        {
            String reason = failureReason.get();
            if (player.hasPermission("griefprevention.ignoreclaims"))
            {
                reason += "  " + instance.dataStore.getMessage(Messages.IgnoreClaimsAdvertisement);
            }

            GriefPrevention.sendMessage(player, TextMode.ERROR, reason);

            //cancel the event by preventing hatching
            event.setHatching(false);

            //only give the egg back if player is in survival or adventure
            if (player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE)
            {
                player.getInventory().addItem(event.getEgg().getItem());
            }
        }
    }
}
