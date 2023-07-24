package me.tinyoverflow.griefprevention.listeners.player;

import me.tinyoverflow.griefprevention.GriefPrevention;
import me.tinyoverflow.griefprevention.tasks.EquipShovelProcessingTask;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;

public class PlayerItemHeldListener implements Listener {
    private final GriefPrevention plugin;

    public PlayerItemHeldListener(GriefPrevention plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemHeldChange(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();

        //if he's switching to the golden shovel
        ItemStack newItemStack = player.getInventory().getItem(event.getNewSlot());
        if (newItemStack != null && newItemStack.getType() == plugin.getPluginConfig().getClaimConfiguration().getToolsConfiguration().getModificationTool()) {
            //give the player his available claim blocks count and claiming instructions, but only if he keeps the shovel equipped for a minimum time, to avoid mouse wheel spam
            if (plugin.claimsEnabledForWorld(player.getWorld())) {
                EquipShovelProcessingTask task = new EquipShovelProcessingTask(player);
                plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, task, 15L);
            }
        }
    }
}
