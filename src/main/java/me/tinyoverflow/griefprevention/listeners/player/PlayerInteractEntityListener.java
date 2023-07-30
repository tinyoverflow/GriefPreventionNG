package me.tinyoverflow.griefprevention.listeners.player;

import me.tinyoverflow.griefprevention.*;
import me.tinyoverflow.griefprevention.datastore.DataStore;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.*;
import org.bukkit.entity.minecart.PoweredMinecart;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;
import java.util.function.Supplier;

public class PlayerInteractEntityListener implements Listener
{
    private final GriefPrevention plugin;
    private final DataStore dataStore;

    public PlayerInteractEntityListener(GriefPrevention plugin, DataStore dataStore)
    {
        this.plugin = plugin;
        this.dataStore = dataStore;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event)
    {
        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();

        if (!plugin.claimsEnabledForWorld(entity.getWorld())) return;

        //allow horse protection to be overridden to allow management from other plugins
        if (!plugin.getPluginConfig().getClaimConfiguration().getMobsConfiguration().protectHorses &&
            entity instanceof AbstractHorse)
        {
            return;
        }
        if (!plugin.getPluginConfig().getClaimConfiguration().getMobsConfiguration().protectDonkeys &&
            entity instanceof Donkey)
        {
            return;
        }
        if (!plugin.getPluginConfig().getClaimConfiguration().getMobsConfiguration().protectDonkeys &&
            entity instanceof Mule)
        {
            return;
        }
        if (!plugin.getPluginConfig().getClaimConfiguration().getMobsConfiguration().protectLlamas &&
            entity instanceof Llama)
        {
            return;
        }

        PlayerData playerData = dataStore.getPlayerData(player.getUniqueId());

        //if entity is tameable and has an owner, apply special rules
        if (entity instanceof Tameable tameable)
        {
            if (tameable.isTamed())
            {
                if (tameable.getOwner() != null)
                {
                    UUID ownerID = tameable.getOwner().getUniqueId();

                    //if the player interacting is the owner or an admin in ignore claims mode, always allow
                    if (player.getUniqueId().equals(ownerID) || playerData.ignoreClaims)
                    {
                        //if giving away pet, do that instead
                        if (playerData.petGiveawayRecipient != null)
                        {
                            tameable.setOwner(playerData.petGiveawayRecipient);
                            playerData.petGiveawayRecipient = null;
                            GriefPrevention.sendMessage(player, TextMode.SUCCESS, Messages.PetGiveawayConfirmation);
                            event.setCancelled(true);
                        }

                        return;
                    }
                    if (!plugin.pvpRulesApply(entity.getLocation().getWorld()) ||
                        plugin.getPluginConfig().getPvpConfiguration().isProtectPets())
                    {
                        //otherwise disallow
                        OfflinePlayer owner = plugin.getServer().getOfflinePlayer(ownerID);
                        String ownerName = owner.getName();
                        if (ownerName == null) ownerName = "someone";
                        String message = plugin.dataStore.getMessage(Messages.NotYourPet, ownerName);
                        if (player.hasPermission("griefprevention.ignoreclaims"))
                        {
                            message += "  " + plugin.dataStore.getMessage(Messages.IgnoreClaimsAdvertisement);
                        }
                        GriefPrevention.sendMessage(player, TextMode.ERROR, message);
                        event.setCancelled(true);
                        return;
                    }
                }
            }
            else  //world repair code for a now-fixed GP bug //TODO: necessary anymore?
            {
                //ensure this entity can be tamed by players
                tameable.setOwner(null);
                if (tameable instanceof InventoryHolder holder)
                {
                    holder.getInventory().clear();
                }
            }
        }

        //don't allow interaction with item frames or armor stands in claimed areas without build permission
        if (entity.getType() == EntityType.ARMOR_STAND || entity instanceof Hanging)
        {
            String noBuildReason = plugin.allowBuild(player, entity.getLocation(), Material.ITEM_FRAME);
            if (noBuildReason != null)
            {
                GriefPrevention.sendMessage(player, TextMode.ERROR, noBuildReason);
                event.setCancelled(true);
                return;
            }
        }

        //limit armor placements when entity count is too high
        if (entity.getType() == EntityType.ARMOR_STAND && plugin.creativeRulesApply(player.getLocation()))
        {
            if (playerData == null) playerData = dataStore.getPlayerData(player.getUniqueId());
            Claim claim = dataStore.getClaimAt(entity.getLocation(), false, playerData.lastClaim);
            if (claim == null) return;

            String noEntitiesReason = claim.allowMoreEntities(false);
            if (noEntitiesReason != null)
            {
                GriefPrevention.sendMessage(player, TextMode.ERROR, noEntitiesReason);
                event.setCancelled(true);
                return;
            }
        }

        //always allow interactions when player is in ignore claims mode
        if (playerData.ignoreClaims) return;

        //don't allow container access during pvp combat
        if ((entity instanceof StorageMinecart || entity instanceof PoweredMinecart))
        {
            if (playerData.siegeData != null)
            {
                GriefPrevention.sendMessage(player, TextMode.ERROR, Messages.SiegeNoContainers);
                event.setCancelled(true);
                return;
            }

            if (playerData.inPvpCombat())
            {
                GriefPrevention.sendMessage(player, TextMode.ERROR, Messages.PvPNoContainers);
                event.setCancelled(true);
                return;
            }
        }

        //if the entity is a vehicle and we're preventing theft in claims
        if (plugin.getPluginConfig().getClaimConfiguration().getProtectionConfiguration().isLockContainersEnabled() &&
            entity instanceof Vehicle)
        {
            //if the entity is in a claim
            Claim claim = dataStore.getClaimAt(entity.getLocation(), false, null);
            if (claim != null)
            {
                //for storage entities, apply container rules (this is a potential theft)
                if (entity instanceof InventoryHolder)
                {
                    Supplier<String> noContainersReason = claim.checkPermission(
                            player,
                            ClaimPermission.Inventory,
                            event
                    );
                    if (noContainersReason != null)
                    {
                        GriefPrevention.sendMessage(player, TextMode.ERROR, noContainersReason.get());
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }

        //if the entity is an animal, apply container rules
        if ((plugin.getPluginConfig().getClaimConfiguration().getProtectionConfiguration().isLockContainersEnabled() &&
             (entity instanceof Animals || entity instanceof Fish)) || (entity.getType() == EntityType.VILLAGER &&
                                                                        plugin.getPluginConfig().getClaimConfiguration().getProtectionConfiguration().isPreventVillagerTradesEnabled()))
        {
            //if the entity is in a claim
            Claim claim = dataStore.getClaimAt(entity.getLocation(), false, null);
            if (claim != null)
            {
                Supplier<String> override = () ->
                {
                    String message = plugin.dataStore.getMessage(Messages.NoDamageClaimedEntity, claim.getOwnerName());
                    if (player.hasPermission("griefprevention.ignoreclaims"))
                    {
                        message += "  " + plugin.dataStore.getMessage(Messages.IgnoreClaimsAdvertisement);
                    }

                    return message;
                };
                final Supplier<String> noContainersReason = claim.checkPermission(
                        player,
                        ClaimPermission.Inventory,
                        event,
                        override
                );
                if (noContainersReason != null)
                {
                    GriefPrevention.sendMessage(player, TextMode.ERROR, noContainersReason.get());
                    event.setCancelled(true);
                    return;
                }
            }
        }

        ItemStack itemInHand = plugin.getItemInHand(player, event.getHand());

        //if preventing theft, prevent leashing claimed creatures
        if (plugin.getPluginConfig().getClaimConfiguration().getProtectionConfiguration().isLockContainersEnabled() &&
            entity instanceof Creature && itemInHand.getType() == Material.LEAD)
        {
            Claim claim = dataStore.getClaimAt(entity.getLocation(), false, playerData.lastClaim);
            if (claim != null)
            {
                Supplier<String> failureReason = claim.checkPermission(player, ClaimPermission.Inventory, event);
                if (failureReason != null)
                {
                    event.setCancelled(true);
                    GriefPrevention.sendMessage(player, TextMode.ERROR, failureReason.get());
                    return;
                }
            }
        }

        // Name tags may only be used on entities that the player is allowed to kill.
        if (itemInHand.getType() == Material.NAME_TAG)
        {
            boolean isCancelled = new EntityDamageByEntityEvent(
                    player,
                    entity,
                    EntityDamageEvent.DamageCause.CUSTOM,
                    0
            ).callEvent();

            // Don't print message - damage event handler should have handled it.
            if (isCancelled) event.setCancelled(true);
        }
    }
}
