package me.tinyoverflow.griefprevention.listeners.entity;

import me.tinyoverflow.griefprevention.ClaimsMode;
import me.tinyoverflow.griefprevention.GriefPrevention;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ExpBottleEvent;

public class ExpBottleListener implements Listener
{
    private final GriefPrevention plugin;

    public ExpBottleListener(GriefPrevention plugin)
    {
        this.plugin = plugin;
    }

    //when an experience bottle explodes...
    @EventHandler(priority = EventPriority.LOWEST)
    public void onExpBottle(ExpBottleEvent event)
    {
        ClaimsMode worldMode = plugin.getPluginConfig()
                .getClaimConfiguration()
                .getWorldMode(event.getEntity().getLocation().getWorld());

        if (worldMode == ClaimsMode.Creative)
            event.setExperience(0);
    }
}
