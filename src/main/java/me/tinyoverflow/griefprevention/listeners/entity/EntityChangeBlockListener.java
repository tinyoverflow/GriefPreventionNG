package me.tinyoverflow.griefprevention.listeners.entity;

import me.tinyoverflow.griefprevention.*;
import me.tinyoverflow.griefprevention.datastore.DataStore;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.projectiles.BlockProjectileSource;
import org.bukkit.projectiles.ProjectileSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

public class EntityChangeBlockListener implements Listener
{
    private final GriefPrevention plugin;
    private final DataStore dataStore;

    public EntityChangeBlockListener(GriefPrevention plugin, DataStore dataStore)
    {
        this.plugin = plugin;
        this.dataStore = dataStore;
    }

    public static boolean isBlockSourceInClaim(@Nullable ProjectileSource projectileSource, @Nullable Claim claim)
    {
        return projectileSource instanceof BlockProjectileSource &&
               GriefPrevention.instance.getDataStore().getClaimAt(
                       ((BlockProjectileSource) projectileSource).getBlock().getLocation(),
                       false,
                       claim
               ) == claim;
    }

    //when an entity picks up an item
    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityPickup(EntityChangeBlockEvent event)
    {
        //FEATURE: endermen don't steal claimed blocks

        //if it's an enderman
        if (event.getEntity().getType() == EntityType.ENDERMAN)
        {
            //and the block is claimed
            if (dataStore.getClaimAt(event.getBlock().getLocation(), false, null) != null)
            {
                //he doesn't get to steal it
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onEntityChangeBlock(EntityChangeBlockEvent event)
    {
        if (plugin.getPluginConfig().getClaimConfiguration().getProtectionConfiguration().isPreventEndermenBlockMovingEnabled()
            && event.getEntityType() == EntityType.ENDERMAN)
        {
            event.setCancelled(true);
        }
        else if (
                plugin.getPluginConfig().getClaimConfiguration().getProtectionConfiguration().isPreventSilverfishBlockBreakingEnabled()
                && event.getEntityType() == EntityType.SILVERFISH)
        {
            event.setCancelled(true);
        }
        else if (
                plugin.getPluginConfig().getClaimConfiguration().getProtectionConfiguration().isPreventRabbitsEatingCropsEnabled()
                && event.getEntityType() == EntityType.RABBIT)
        {
            event.setCancelled(true);
        }
        else if (
                plugin.getPluginConfig().getClaimConfiguration().getProtectionConfiguration().isPreventRavagerDamageEnabled()
                && event.getEntityType() == EntityType.RAVAGER)
        {
            event.setCancelled(true);
        }
        // All other handling depends on claims being enabled.
        else if (plugin.getPluginConfig().getClaimConfiguration().isWorldEnabled(event.getBlock().getWorld()))
        {
            return;
        }

        // Handle projectiles changing blocks: TNT ignition, tridents knocking down pointed dripstone, etc.
        if (event.getEntity() instanceof Projectile)
        {
            handleProjectileChangeBlock(event, (Projectile) event.getEntity());
        }
        else if (event.getEntityType() == EntityType.WITHER)
        {
            Claim claim = dataStore.getClaimAt(event.getBlock().getLocation(), false, null);
            if (claim == null || !claim.areExplosivesAllowed || !plugin.config_blockClaimExplosions)
            {
                event.setCancelled(true);
            }
        }

        //don't allow crops to be trampled, except by a player with build permission
        else if (event.getTo() == Material.DIRT && event.getBlock().getType() == Material.FARMLAND)
        {
            if (event.getEntityType() != EntityType.PLAYER)
            {
                event.setCancelled(true);
            }
            else
            {
                Player player = (Player) event.getEntity();
                Block block = event.getBlock();
                if (plugin.allowBreak(player, block, block.getLocation()) != null)
                {
                    event.setCancelled(true);
                }
            }
        }

        // Prevent melting powdered snow.
        else if (event.getBlock().getType() == Material.POWDER_SNOW && event.getTo() == Material.AIR)
        {
            handleEntityMeltPowderedSnow(event);
        }

        // Prevent breaking lily pads via collision with a boat.
        else if (event.getEntity() instanceof Vehicle && !event.getEntity().getPassengers().isEmpty())
        {
            Entity driver = event.getEntity().getPassengers().get(0);
            if (driver instanceof Player)
            {
                Block block = event.getBlock();
                if (plugin.allowBreak((Player) driver, block, block.getLocation()) != null)
                {
                    event.setCancelled(true);
                }
            }
        }

        //sand cannon fix - when the falling block doesn't fall straight down, take additional anti-grief steps
        else if (event.getEntityType() == EntityType.FALLING_BLOCK)
        {
            FallingBlock entity = (FallingBlock) event.getEntity();
            Block block = event.getBlock();

            //if changing a block TO air, this is when the falling block formed.  note its original location
            if (event.getTo() == Material.AIR)
            {
                entity.setMetadata("GP_FALLINGBLOCK", new FixedMetadataValue(plugin, block.getLocation()));
            }
            //otherwise, the falling block is forming a block.  compare new location to original source
            else
            {
                List<MetadataValue> values = entity.getMetadata("GP_FALLINGBLOCK");
                //if we're not sure where this entity came from (maybe another plugin didn't follow the standard?), allow the block to form
                //Or if entity fell through an end portal, allow it to form, as the event is erroneously fired twice in this scenario.
                if (values.size() < 1) return;

                Location originalLocation = (Location) (values.get(0).value());
                Location newLocation = block.getLocation();

                //if did not fall straight down
                if (originalLocation.getBlockX() != newLocation.getBlockX()
                    || originalLocation.getBlockZ() != newLocation.getBlockZ())
                {
                    //in creative mode worlds, never form the block
                    if (plugin.getPluginConfig().getClaimConfiguration().getWorldMode(newLocation.getWorld()) ==
                        ClaimsMode.Creative)
                    {
                        event.setCancelled(true);
                        entity.remove();
                        return;
                    }

                    //in other worlds, if landing in land claim, only allow if source was also in the land claim
                    Claim claim = dataStore.getClaimAt(newLocation, false, null);
                    if (claim != null && !claim.contains(originalLocation, false, false))
                    {
                        //when not allowed, drop as item instead of forming a block
                        event.setCancelled(true);

                        // Just in case, skip already dead entities.
                        if (entity.isDead())
                        {
                            return;
                        }

                        // Remove entity so it doesn't continuously spawn drops.
                        entity.remove();

                        ItemStack itemStack = new ItemStack(entity.getBlockData().getMaterial(), 1);
                        block.getWorld().dropItemNaturally(entity.getLocation(), itemStack);
                    }
                }
            }
        }
    }

    private void handleProjectileChangeBlock(EntityChangeBlockEvent event, Projectile projectile)
    {
        Block block = event.getBlock();
        Claim claim = dataStore.getClaimAt(block.getLocation(), false, null);

        // Wilderness rules
        if (claim == null)
        {
            // No modification in the wilderness in creative mode.
            if (plugin.creativeRulesApply(block.getLocation())
                || plugin.getPluginConfig().getClaimConfiguration().getWorldMode(block.getWorld()) ==
                   ClaimsMode.SurvivalRequiringClaims)
            {
                event.setCancelled(true);
                return;
            }

            // Unclaimed area is fair game.
            return;
        }

        ProjectileSource shooter = projectile.getShooter();

        if (shooter instanceof Player)
        {
            Supplier<String> denial = claim.checkPermission((Player) shooter, ClaimPermission.Build, event);

            // If the player cannot place the material being broken, disallow.
            if (denial != null)
            {
                // Unlike entities where arrows rebound and may cause multiple alerts,
                // projectiles lodged in blocks do not continuously re-trigger events.
                GriefPrevention.sendMessage((Player) shooter, TextMode.ERROR, denial.get());
                event.setCancelled(true);
            }

            return;
        }

        // Allow change if projectile was shot by a dispenser in the same claim.
        if (isBlockSourceInClaim(shooter, claim)) return;

        // Prevent change in all other cases.
        event.setCancelled(true);
    }

    private void handleEntityMeltPowderedSnow(@NotNull EntityChangeBlockEvent event)
    {
        // Note: this does not handle flaming arrows; they are handled earlier by #handleProjectileChangeBlock
        Player player = null;
        if (event.getEntity() instanceof Player localPlayer)
        {
            player = localPlayer;
        }
        else if (event.getEntity() instanceof Mob mob)
        {
            // Handle players leading packs of zombies.
            if (mob.getTarget() instanceof Player localPlayer)
            {
                player = localPlayer;
            }
            // Handle players leading burning leashed entities.
            else if (mob.isLeashed() && mob.getLeashHolder() instanceof Player localPlayer) player = localPlayer;
        }

        if (player != null)
        {
            Block block = event.getBlock();
            if (plugin.allowBreak(player, block, block.getLocation()) != null)
            {
                event.setCancelled(true);
            }
        }
        else
        {
            // Unhandled case, i.e. skeletons on fire due to sunlight lose target to search for cover.
            // Possible to handle by tagging entities during combustion, but likely not worth it.
            event.setCancelled(true);
        }
    }
}
