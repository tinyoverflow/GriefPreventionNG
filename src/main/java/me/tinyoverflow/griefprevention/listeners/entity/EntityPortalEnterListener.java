package me.tinyoverflow.griefprevention.listeners.entity;

import me.tinyoverflow.griefprevention.GriefPrevention;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEnterEvent;

public class EntityPortalEnterListener implements Listener
{
    private final GriefPrevention plugin;

    public EntityPortalEnterListener(GriefPrevention plugin)
    {
        this.plugin = plugin;
    }

    //Used by "sand cannon" fix to ignore fallingblocks that fell through End Portals
    //This is largely due to a CB issue with the above event
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onFallingBlockEnterPortal(EntityPortalEnterEvent event)
    {
        if (event.getEntityType() != EntityType.FALLING_BLOCK)
            return;
        event.getEntity().removeMetadata("GP_FALLINGBLOCK", plugin);
    }
}
