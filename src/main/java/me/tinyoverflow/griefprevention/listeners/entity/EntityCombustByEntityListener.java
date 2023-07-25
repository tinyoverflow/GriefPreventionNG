package me.tinyoverflow.griefprevention.listeners.entity;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.NotNull;

public class EntityCombustByEntityListener implements Listener
{
    //when an entity is set on fire
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onEntityCombustByEntity(@NotNull EntityCombustByEntityEvent event)
    {
        //handle it just like we would an entity damage by entity event, except don't send player messages to avoid double messages
        //in cases like attacking with a flame sword or flame arrow, which would ALSO trigger the direct damage event handler

        boolean isCancelled = new EntityDamageByEntityEvent(
                event.getCombuster(),
                event.getEntity(),
                EntityDamageEvent.DamageCause.FIRE_TICK,
                event.getDuration()
        ).callEvent();

        if (isCancelled) {
            event.setCancelled(true);
        }
    }
}
