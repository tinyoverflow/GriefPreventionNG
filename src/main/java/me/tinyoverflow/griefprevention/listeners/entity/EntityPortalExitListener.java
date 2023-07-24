package me.tinyoverflow.griefprevention.listeners.entity;

import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class EntityPortalExitListener implements Listener
{
    //Don't let people drop in TNT through end portals
    //Necessarily this shouldn't be an issue anyway since the platform is obsidian...
    @EventHandler(ignoreCancelled = true)
    void onTNTExitPortal(org.bukkit.event.entity.EntityPortalExitEvent event)
    {
        if (event.getEntityType() != EntityType.PRIMED_TNT)
            return;

        if (event.getTo().getWorld().getEnvironment() != World.Environment.THE_END)
            return;

        event.getEntity().remove();
    }
}
