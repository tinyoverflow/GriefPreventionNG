package me.tinyoverflow.griefprevention.listeners.player;

import me.tinyoverflow.griefprevention.*;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;

import java.util.function.Supplier;

public class PlayerFishListener implements Listener
{
    private final GriefPrevention plugin;

    public PlayerFishListener(GriefPrevention plugin)
    {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerFish(PlayerFishEvent event)
    {
        Entity entity = event.getCaught();
        if (entity == null) return;  //if nothing pulled, uninteresting event

        //if should be protected from pulling in land claims without permission
        if (entity.getType() == EntityType.ARMOR_STAND || entity instanceof Animals)
        {
            Player player = event.getPlayer();
            PlayerData playerData = plugin.dataStore.getPlayerData(player.getUniqueId());
            Claim claim = plugin.dataStore.getClaimAt(entity.getLocation(), false, playerData.lastClaim);
            if (claim != null)
            {
                //if no permission, cancel
                Supplier<String> errorMessage = claim.checkPermission(player, ClaimPermission.Inventory, event);
                if (errorMessage != null)
                {
                    event.setCancelled(true);
                    GriefPrevention.sendMessage(
                            player,
                            TextMode.ERROR,
                            Messages.NoDamageClaimedEntity,
                            claim.getOwnerName()
                    );
                }
            }
        }
    }
}
