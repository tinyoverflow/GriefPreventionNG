package me.tinyoverflow.griefprevention.listeners.entity;

import me.tinyoverflow.griefprevention.GriefPrevention;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

public class EntityTargetListener implements Listener
{
    private final GriefPrevention plugin;
    private final @NotNull NamespacedKey luredByPlayerKey;

    public EntityTargetListener(GriefPrevention plugin)
    {
        this.plugin = plugin;

        luredByPlayerKey = new NamespacedKey(plugin, "lured_by_player");
    }

    // Tag passive animals that can become aggressive so that we can tell whether they are hostile later
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityTarget(@NotNull EntityTargetEvent event)
    {
        if (!plugin.claimsEnabledForWorld(event.getEntity().getWorld())) return;

        EntityType entityType = event.getEntityType();
        if (entityType != EntityType.HOGLIN && entityType != EntityType.POLAR_BEAR) {
            return;
        }

        if (event.getReason() == EntityTargetEvent.TargetReason.TEMPT) {
            event.getEntity().getPersistentDataContainer().set(luredByPlayerKey, PersistentDataType.BYTE, (byte) 1);
        }
        else {
            event.getEntity().getPersistentDataContainer().remove(luredByPlayerKey);
        }
    }
}
