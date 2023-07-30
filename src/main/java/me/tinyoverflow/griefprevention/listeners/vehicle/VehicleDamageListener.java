package me.tinyoverflow.griefprevention.listeners.vehicle;

import me.tinyoverflow.griefprevention.*;
import me.tinyoverflow.griefprevention.datastore.DataStore;
import me.tinyoverflow.griefprevention.listeners.entity.EntityDamageListener;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class VehicleDamageListener implements Listener
{
    private final GriefPrevention plugin;
    private final DataStore dataStore;

    public VehicleDamageListener(GriefPrevention plugin, DataStore dataStore)
    {
        this.plugin = plugin;
        this.dataStore = dataStore;
    }

    //when a vehicle is damaged
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onVehicleDamage(@NotNull VehicleDamageEvent event)
    {
        //all of this is anti theft code
        if (!plugin.getPluginConfig().getClaimConfiguration().getProtectionConfiguration().isProtectVehiclesEnabled())
        {
            return;
        }

        //don't track in worlds where claims are not enabled
        if (!plugin.claimsEnabledForWorld(event.getVehicle().getWorld())) return;

        //determine which player is attacking, if any
        Player attacker = null;
        Projectile arrow = null;
        Entity damageSource = event.getAttacker();
        EntityType damageSourceType = null;

        //if damage source is null or a creeper, don't allow the damage when the vehicle is in a land claim
        if (damageSource != null)
        {
            damageSourceType = damageSource.getType();

            if (damageSource instanceof Player player)
            {
                attacker = player;
            }
            else if (damageSource instanceof Projectile projectile)
            {
                arrow = projectile;
                if (arrow.getShooter() instanceof Player shooter)
                {
                    attacker = shooter;
                }
            }
        }

        //if not a player and not an explosion, always allow
        if (attacker == null && damageSourceType != EntityType.CREEPER && damageSourceType != EntityType.WITHER &&
            damageSourceType != EntityType.PRIMED_TNT)
        {
            return;
        }

        //NOTE: vehicles can be pushed around.
        //so unless precautions are taken by the owner, a resourceful thief might find ways to steal anyway
        Claim cachedClaim = null;
        PlayerData playerData = null;

        if (attacker != null)
        {
            playerData = dataStore.getPlayerData(attacker.getUniqueId());
            cachedClaim = playerData.lastClaim;
        }

        Claim claim = dataStore.getClaimAt(event.getVehicle().getLocation(), false, cachedClaim);

        // Require a claim.
        if (claim == null) return;

        //if damaged by anything other than a player, cancel the event
        if (attacker == null)
        {
            event.setCancelled(true);
            if (arrow != null) arrow.remove();
            return;
        }

        //otherwise the player damaging the entity must have permission
        final Player finalAttacker = attacker;
        Supplier<String> override = () ->
        {
            String message = dataStore.getMessage(Messages.NoDamageClaimedEntity, claim.getOwnerName());
            if (finalAttacker.hasPermission("griefprevention.ignoreclaims"))
            {
                message += "  " + dataStore.getMessage(Messages.IgnoreClaimsAdvertisement);
            }
            return message;
        };
        Supplier<String> noContainersReason = claim.checkPermission(
                attacker,
                ClaimPermission.Inventory,
                event,
                override
        );
        if (noContainersReason != null)
        {
            event.setCancelled(true);
            EntityDamageListener.preventInfiniteBounce(arrow, event.getVehicle());
            GriefPrevention.sendMessage(attacker, TextMode.ERROR, noContainersReason.get());
        }

        //cache claim for later
        playerData.lastClaim = claim;
    }
}
