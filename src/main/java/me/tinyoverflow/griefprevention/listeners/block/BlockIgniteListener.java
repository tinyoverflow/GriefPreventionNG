package me.tinyoverflow.griefprevention.listeners.block;

import me.tinyoverflow.griefprevention.Claim;
import me.tinyoverflow.griefprevention.GriefPrevention;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Lightable;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.projectiles.BlockProjectileSource;
import org.bukkit.projectiles.ProjectileSource;

public class BlockIgniteListener implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockIgnite(BlockIgniteEvent igniteEvent)
    {
        //don't track in worlds where claims are not enabled
        if (!GriefPrevention.instance.claimsEnabledForWorld(igniteEvent.getBlock().getWorld())) return;

        if (igniteEvent.getCause() == BlockIgniteEvent.IgniteCause.LIGHTNING && GriefPrevention.instance.dataStore.getClaimAt(igniteEvent.getIgnitingEntity().getLocation(), false, null) != null)
        {
            igniteEvent.setCancelled(true); //BlockIgniteEvent is called before LightningStrikeEvent. See #532. However, see #1125 for further discussion on detecting trident-caused lightning.
        }

        // If a fire is started by a fireball from a dispenser, allow it if the dispenser is in the same claim.
        if (igniteEvent.getCause() == BlockIgniteEvent.IgniteCause.FIREBALL && igniteEvent.getIgnitingEntity() instanceof Fireball)
        {
            ProjectileSource shooter = ((Fireball) igniteEvent.getIgnitingEntity()).getShooter();
            if (shooter instanceof BlockProjectileSource)
            {
                Claim claim = GriefPrevention.instance.dataStore.getClaimAt(igniteEvent.getBlock().getLocation(), false, null);
                if (claim != null && GriefPrevention.instance.dataStore.getClaimAt(((BlockProjectileSource) shooter).getBlock().getLocation(), false, claim) == claim)
                {
                    return;
                }
            }
        }

        // Arrow ignition.
        if (igniteEvent.getCause() == BlockIgniteEvent.IgniteCause.ARROW && igniteEvent.getIgnitingEntity() != null)
        {
            // Arrows shot by players may return the shooter, not the arrow.
            if (igniteEvent.getIgnitingEntity() instanceof Player player)
            {
                boolean isBlockBreakCancelled = new BlockBreakEvent(igniteEvent.getBlock(), player).callEvent();
                if (isBlockBreakCancelled)
                    igniteEvent.setCancelled(true);

                return;
            }
            // Flammable lightable blocks do not fire EntityChangeBlockEvent when igniting.
            BlockData blockData = igniteEvent.getBlock().getBlockData();
            if (blockData instanceof Lightable lightable)
            {
                // Set lit for resulting data in event. Currently unused, but may be in the future.
                lightable.setLit(true);

                // Call event.
                EntityChangeBlockEvent changeBlockEvent = new EntityChangeBlockEvent(igniteEvent.getIgnitingEntity(), igniteEvent.getBlock(), blockData);
                GriefPrevention.instance.entityEventHandler.onEntityChangeBLock(changeBlockEvent);

                // Respect event result.
                if (changeBlockEvent.isCancelled())
                {
                    igniteEvent.setCancelled(true);
                }
            }
            return;
        }

        if (!GriefPrevention.instance.config_fireSpreads && igniteEvent.getCause() != BlockIgniteEvent.IgniteCause.FLINT_AND_STEEL && igniteEvent.getCause() != BlockIgniteEvent.IgniteCause.LIGHTNING)
        {
            igniteEvent.setCancelled(true);
        }
    }
}
