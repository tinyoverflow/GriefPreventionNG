package me.tinyoverflow.griefprevention.listeners.entity;

import me.tinyoverflow.griefprevention.Claim;
import me.tinyoverflow.griefprevention.GriefPrevention;
import me.tinyoverflow.griefprevention.PendingItemProtection;
import me.tinyoverflow.griefprevention.PlayerData;
import me.tinyoverflow.griefprevention.datastore.DataStore;
import me.tinyoverflow.griefprevention.events.ProtectDeathDropsEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

public class EntityDeathListener implements Listener
{
    private final GriefPrevention plugin;
    private final DataStore dataStore;

    public EntityDeathListener(GriefPrevention plugin, DataStore dataStore)
    {
        this.plugin = plugin;
        this.dataStore = dataStore;
    }

    //when an entity dies...
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event)
    {
        LivingEntity entity = event.getEntity();

        //don't do the rest in worlds where claims are not enabled
        if (!plugin.claimsEnabledForWorld(entity.getWorld())) return;

        //special rule for creative worlds: killed entities don't drop items or experience orbs
        if (plugin.creativeRulesApply(entity.getLocation())) {
            event.setDroppedExp(0);
            event.getDrops().clear();
        }

        //FEATURE: when a player is involved in a siege (attacker or defender role)
        //his death will end the siege

        if (entity.getType() != EntityType.PLAYER) return;  //only tracking players

        Player player = (Player) entity;
        PlayerData playerData = dataStore.getPlayerData(player.getUniqueId());

        //if involved in a siege
        if (playerData.siegeData != null) {
            //end it, with the dying player being the loser
            dataStore.endSiege(playerData.siegeData, null, player.getName(), event.getDrops());
        }

        //FEATURE: lock dropped items to player who dropped them
        World world = entity.getWorld();

        //decide whether to apply this feature to this situation (depends on the world where it happens)
        boolean isPvPWorld = plugin.pvpRulesApply(world);
        if ((isPvPWorld && plugin.config_lockDeathDropsInPvpWorlds) ||
            (!isPvPWorld && plugin.config_lockDeathDropsInNonPvpWorlds))
        {
            Claim claim = dataStore.getClaimAt(player.getLocation(), false, playerData.lastClaim);
            ProtectDeathDropsEvent protectionEvent = new ProtectDeathDropsEvent(claim);
            Bukkit.getPluginManager().callEvent(protectionEvent);
            if (!protectionEvent.isCancelled()) {
                //remember information about these drops so that they can be marked when they spawn as items
                long expirationTime = System.currentTimeMillis() + 3000;  //now + 3 seconds
                Location deathLocation = player.getLocation();
                UUID playerID = player.getUniqueId();
                List<ItemStack> drops = event.getDrops();
                for (ItemStack stack : drops) {
                    plugin.pendingItemWatchList.add(
                            new PendingItemProtection(deathLocation, playerID, expirationTime, stack));
                }

                //allow the player to receive a message about how to unlock any drops
                playerData.dropsAreUnlocked = false;
                playerData.receivedDropUnlockAdvertisement = false;
            }
        }
    }
}
