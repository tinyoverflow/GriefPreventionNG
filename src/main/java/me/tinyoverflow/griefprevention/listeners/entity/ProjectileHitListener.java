package me.tinyoverflow.griefprevention.listeners.entity;

import me.tinyoverflow.griefprevention.Claim;
import me.tinyoverflow.griefprevention.ClaimPermission;
import me.tinyoverflow.griefprevention.GriefPrevention;
import me.tinyoverflow.griefprevention.TextMode;
import me.tinyoverflow.griefprevention.datastore.DataStore;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;

import java.util.function.Supplier;

public class ProjectileHitListener implements Listener {
    private final DataStore dataStore;

    public ProjectileHitListener(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    @EventHandler(ignoreCancelled = true)
    private void chorusFlower(ProjectileHitEvent event) {
        //don't track in worlds where claims are not enabled
        if (!GriefPrevention.instance.claimsEnabledForWorld(event.getEntity().getWorld())) return;

        Block block = event.getHitBlock();

        // Ensure projectile affects block.
        if (block == null || block.getType() != Material.CHORUS_FLOWER) return;

        Claim claim = dataStore.getClaimAt(block.getLocation(), false, null);
        if (claim == null) return;

        Player shooter = null;
        Projectile projectile = event.getEntity();

        if (projectile.getShooter() instanceof Player) shooter = (Player) projectile.getShooter();

        if (shooter == null) {
            event.setCancelled(true);
            return;
        }

        Supplier<String> allowContainer = claim.checkPermission(shooter, ClaimPermission.Inventory, event);

        if (allowContainer != null) {
            event.setCancelled(true);
            GriefPrevention.sendMessage(shooter, TextMode.Err, allowContainer.get());
        }
    }
}
