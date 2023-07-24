package me.tinyoverflow.griefprevention.listeners.entity;

import me.tinyoverflow.griefprevention.GriefPrevention;
import me.tinyoverflow.griefprevention.Messages;
import me.tinyoverflow.griefprevention.PlayerData;
import me.tinyoverflow.griefprevention.TextMode;
import me.tinyoverflow.griefprevention.datastore.DataStore;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Item;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.metadata.MetadataValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Calendar;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class EntityPickupItemListener implements Listener
{
    private final GriefPrevention plugin;
    private final DataStore dataStore;

    public EntityPickupItemListener(GriefPrevention plugin, DataStore dataStore)
    {
        this.plugin = plugin;
        this.dataStore = dataStore;
    }

    @EventHandler
    public void onEntityPickUpItem(@NotNull EntityPickupItemEvent event)
    {
        // Hostiles are allowed to equip death drops to preserve the challenge of item retrieval.
        if (event.getEntity() instanceof Monster) return;

        Player player = null;
        if (event.getEntity() instanceof Player) {
            player = (Player) event.getEntity();
        }

        //FEATURE: Lock dropped items to player who dropped them.
        protectLockedDrops(event, player);

        // FEATURE: Protect freshly-spawned players from PVP.
        preventPvpSpawnCamp(event, player);
    }

    private void protectLockedDrops(@NotNull EntityPickupItemEvent event, @Nullable Player player)
    {
        Item item = event.getItem();
        List<MetadataValue> data = item.getMetadata("GP_ITEMOWNER");

        // Ignore absent or invalid data.
        if (data.isEmpty() || !(data.get(0).value() instanceof UUID ownerID)) return;

        // Get owner from stored UUID.
        OfflinePlayer owner = plugin.getServer().getOfflinePlayer(ownerID);

        // Owner must be online and can pick up their own drops.
        if (!owner.isOnline() || Objects.equals(player, owner)) return;

        PlayerData playerData = dataStore.getPlayerData(ownerID);

        // If drops are unlocked, allow pick up.
        if (playerData.dropsAreUnlocked) return;

        // Block pick up.
        event.setCancelled(true);

        // Non-players (dolphins, allays) do not need to generate prompts.
        if (player == null) {
            return;
        }

        // If the owner hasn't been instructed how to unlock, send explanatory messages.
        if (!playerData.receivedDropUnlockAdvertisement) {
            GriefPrevention.sendMessage(owner.getPlayer(), TextMode.Instr, Messages.DropUnlockAdvertisement);
            GriefPrevention.sendMessage(
                    player,
                    TextMode.Err,
                    Messages.PickupBlockedExplanation,
                    GriefPrevention.lookupPlayerName(ownerID)
            );
            playerData.receivedDropUnlockAdvertisement = true;
        }
    }

    private void preventPvpSpawnCamp(@NotNull EntityPickupItemEvent event, @Nullable Player player)
    {
        // This is specific to players in pvp worlds.
        if (player == null || !plugin.pvpRulesApply(player.getWorld())) return;

        //if we're preventing spawn camping and the player was previously empty handed...
        if (plugin.config_pvp_protectFreshSpawns && (plugin.getItemInHand(
                player,
                EquipmentSlot.HAND
        ).getType() == Material.AIR))
        {
            //if that player is currently immune to pvp
            PlayerData playerData = dataStore.getPlayerData(player.getUniqueId());
            if (playerData.pvpImmune) {
                //if it's been less than 10 seconds since the last time he spawned, don't pick up the item
                long now = Calendar.getInstance().getTimeInMillis();
                long elapsedSinceLastSpawn = now - playerData.lastSpawn;
                if (elapsedSinceLastSpawn < 10000) {
                    event.setCancelled(true);
                    return;
                }

                //otherwise take away his immunity. he may be armed now.  at least, he's worth killing for some loot
                playerData.pvpImmune = false;
                GriefPrevention.sendMessage(player, TextMode.Warn, Messages.PvPImmunityEnd);
            }
        }
    }
}
