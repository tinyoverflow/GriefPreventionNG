package me.tinyoverflow.griefprevention.listeners.entity;

import me.tinyoverflow.griefprevention.*;
import me.tinyoverflow.griefprevention.datastore.DataStore;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static me.tinyoverflow.griefprevention.GriefPrevention.instance;

public class PotionSplashListener implements Listener
{
    private final Set<PotionEffectType> GRIEF_EFFECTS = Set.of(
            // Damaging effects
            PotionEffectType.HARM,
            PotionEffectType.POISON,
            PotionEffectType.WITHER,
            // Effects that could remove entities from normally-secure pens
            PotionEffectType.JUMP,
            PotionEffectType.LEVITATION
    );
    private final Set<PotionEffectType> POSITIVE_EFFECTS = Set.of(
            PotionEffectType.ABSORPTION,
            PotionEffectType.CONDUIT_POWER,
            PotionEffectType.DAMAGE_RESISTANCE,
            PotionEffectType.DOLPHINS_GRACE,
            PotionEffectType.FAST_DIGGING,
            PotionEffectType.FIRE_RESISTANCE,
            PotionEffectType.HEAL,
            PotionEffectType.HEALTH_BOOST,
            PotionEffectType.HERO_OF_THE_VILLAGE,
            PotionEffectType.INCREASE_DAMAGE,
            PotionEffectType.INVISIBILITY,
            PotionEffectType.JUMP,
            PotionEffectType.LUCK,
            PotionEffectType.NIGHT_VISION,
            PotionEffectType.REGENERATION,
            PotionEffectType.SATURATION,
            PotionEffectType.SLOW_FALLING,
            PotionEffectType.SPEED,
            PotionEffectType.WATER_BREATHING
    );
    private final DataStore dataStore;

    public PotionSplashListener(DataStore dataStore)
    {
        this.dataStore = dataStore;
    }

    //when a splash potion affects one or more entities...
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPotionSplash(@NotNull PotionSplashEvent event)
    {
        ThrownPotion potion = event.getPotion();

        ProjectileSource projectileSource = potion.getShooter();
        // Ignore potions with no source.
        if (projectileSource == null) return;
        final Player thrower;
        if ((projectileSource instanceof Player)) {
            thrower = (Player) projectileSource;
        }
        else {
            thrower = null;
        }
        AtomicBoolean messagedPlayer = new AtomicBoolean(false);

        Collection<PotionEffect> effects = potion.getEffects();
        for (PotionEffect effect : effects) {
            PotionEffectType effectType = effect.getType();

            // Restrict some potions on claimed villagers and animals.
            // Griefers could use potions to kill entities or steal them over fences.
            if (GRIEF_EFFECTS.contains(effectType)) {
                Claim cachedClaim = null;
                for (LivingEntity affected : event.getAffectedEntities()) {
                    // Always impact the thrower.
                    if (affected == thrower) continue;

                    if (affected.getType() == EntityType.VILLAGER || affected instanceof Animals) {
                        Claim claim = dataStore.getClaimAt(affected.getLocation(), false, cachedClaim);
                        if (claim != null) {
                            cachedClaim = claim;

                            if (thrower == null) {
                                // Non-player source: Witches, dispensers, etc.
                                if (!EntityChangeBlockListener.isBlockSourceInClaim(projectileSource, claim)) {
                                    // If the source is not a block in the same claim as the affected entity, disallow.
                                    event.setIntensity(affected, 0);
                                }
                            }
                            else {
                                // Source is a player. Determine if they have permission to access entities in the claim.
                                Supplier<String> override = () -> instance.dataStore.getMessage(
                                        Messages.NoDamageClaimedEntity,
                                        claim.getOwnerName()
                                );
                                final Supplier<String> noContainersReason = claim.checkPermission(
                                        thrower,
                                        ClaimPermission.Inventory,
                                        event,
                                        override
                                );
                                if (noContainersReason != null) {
                                    event.setIntensity(affected, 0);
                                    if (messagedPlayer.compareAndSet(false, true)) {
                                        GriefPrevention.sendMessage(thrower, TextMode.Err, noContainersReason.get());
                                    }
                                }
                            }
                        }
                    }
                }
            }

            //Otherwise, ignore potions not thrown by players
            if (thrower == null) return;

            //otherwise, no restrictions for positive effects
            if (POSITIVE_EFFECTS.contains(effectType)) continue;

            for (LivingEntity affected : event.getAffectedEntities()) {
                //always impact the thrower
                if (affected == thrower) continue;

                //always impact non players
                if (!(affected instanceof Player affectedPlayer)) continue;

                //otherwise if in no-pvp zone, stop effect
                //FEATURE: prevent players from engaging in PvP combat inside land claims (when it's disabled)
                if (instance.config_pvp_noCombatInPlayerLandClaims || instance.config_pvp_noCombatInAdminLandClaims) {
                    PlayerData playerData = dataStore.getPlayerData(thrower.getUniqueId());
                    Consumer<Messages> cancelHandler = message ->
                    {
                        event.setIntensity(affected, 0);
                        if (messagedPlayer.compareAndSet(false, true)) {
                            GriefPrevention.sendMessage(thrower, TextMode.Err, message);
                        }
                    };
                    if (EntityDamageListener.handlePvpInClaim(
                            thrower,
                            affectedPlayer,
                            thrower.getLocation(),
                            playerData,
                            () -> cancelHandler.accept(Messages.CantFightWhileImmune)
                    ))
                    {
                        continue;
                    }
                    playerData = dataStore.getPlayerData(affectedPlayer.getUniqueId());
                    EntityDamageListener.handlePvpInClaim(
                            thrower,
                            affectedPlayer,
                            affectedPlayer.getLocation(),
                            playerData,
                            () -> cancelHandler.accept(Messages.PlayerInPvPSafeZone)
                    );
                }
            }
        }
    }
}
