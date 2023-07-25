package me.tinyoverflow.griefprevention.listeners.entity;

import me.tinyoverflow.griefprevention.GriefPrevention;
import me.tinyoverflow.griefprevention.PlayerData;
import me.tinyoverflow.griefprevention.datastore.DataStore;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Calendar;

public class EntityDamageByEntityListener implements Listener
{
    private final GriefPrevention plugin;
    private final DataStore dataStore;

    public EntityDamageByEntityListener(GriefPrevention plugin, DataStore dataStore)
    {
        this.plugin = plugin;
        this.dataStore = dataStore;
    }

    // Flag players engaging in PVP.
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityDamageByEntityMonitor(@NotNull EntityDamageByEntityEvent event)
    {
        //FEATURE: prevent players who very recently participated in pvp combat from hiding inventory to protect it from looting
        //FEATURE: prevent players who are in pvp combat from logging out to avoid being defeated

        // If there is no damage (snowballs, eggs, etc.) or the defender is not a player in a PVP world, do nothing.
        if (event.getDamage() == 0
            || !(event.getEntity() instanceof Player defender)
            || !plugin.pvpRulesApply(defender.getWorld()))
        {
            return;
        }

        //determine which player is attacking, if any
        Player attacker = null;
        Entity damageSource = event.getDamager();

        if (damageSource instanceof Player damager) {
            attacker = damager;
        }
        else if (damageSource instanceof Projectile arrow && arrow.getShooter() instanceof Player shooter) {
            attacker = shooter;
        }

        // If not PVP or attacking self, do nothing.
        if (attacker == null || attacker == defender) return;

        PlayerData defenderData = dataStore.getPlayerData(defender.getUniqueId());
        PlayerData attackerData = dataStore.getPlayerData(attacker.getUniqueId());

        long now = Calendar.getInstance().getTimeInMillis();
        defenderData.lastPvpTimestamp = now;
        defenderData.lastPvpPlayer = attacker.getName();
        attackerData.lastPvpTimestamp = now;
        attackerData.lastPvpPlayer = defender.getName();
    }
}
