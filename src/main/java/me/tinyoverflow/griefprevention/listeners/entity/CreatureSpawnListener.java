package me.tinyoverflow.griefprevention.listeners.entity;

import me.tinyoverflow.griefprevention.Claim;
import me.tinyoverflow.griefprevention.GriefPrevention;
import me.tinyoverflow.griefprevention.datastore.DataStore;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

public class CreatureSpawnListener implements Listener
{
    private final GriefPrevention plugin;
    private final DataStore dataStore;

    public CreatureSpawnListener(GriefPrevention plugin, DataStore dataStore)
    {
        this.plugin = plugin;
        this.dataStore = dataStore;
    }

    //when a creature spawns...
    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntitySpawn(CreatureSpawnEvent event)
    {
        //these rules apply only to creative worlds
        if (!plugin.creativeRulesApply(event.getLocation())) return;

        //chicken eggs and breeding could potentially make a mess in the wilderness, once griefers get involved
        CreatureSpawnEvent.SpawnReason reason = event.getSpawnReason();
        if (reason != CreatureSpawnEvent.SpawnReason.SPAWNER_EGG &&
            reason != CreatureSpawnEvent.SpawnReason.BUILD_IRONGOLEM &&
            reason != CreatureSpawnEvent.SpawnReason.BUILD_SNOWMAN &&
            event.getEntityType() != EntityType.ARMOR_STAND)
        {
            event.setCancelled(true);
            return;
        }

        //otherwise, just apply the limit on total entities per claim (and no spawning in the wilderness!)
        Claim claim = dataStore.getClaimAt(event.getLocation(), false, null);
        if (claim == null || claim.allowMoreEntities(true) != null) {
            event.setCancelled(true);
        }
    }
}
