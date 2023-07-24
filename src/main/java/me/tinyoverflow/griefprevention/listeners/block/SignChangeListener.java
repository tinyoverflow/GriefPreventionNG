package me.tinyoverflow.griefprevention.listeners.block;

import me.tinyoverflow.griefprevention.GriefPrevention;
import me.tinyoverflow.griefprevention.PlayerData;
import me.tinyoverflow.griefprevention.TextMode;
import me.tinyoverflow.griefprevention.datastore.DataStore;
import me.tinyoverflow.griefprevention.handlers.PlayerEventHandler;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;

import java.util.Collection;

public class SignChangeListener implements Listener {

    private final DataStore dataStore;

    public SignChangeListener(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    //when a player changes the text of a sign...
    @EventHandler(ignoreCancelled = true)
    public void onSignChanged(SignChangeEvent event) {
        Player player = event.getPlayer();
        Block sign = event.getBlock();

        String noBuildReason = GriefPrevention.instance.allowBuild(player, sign.getLocation(), sign.getType());
        if (noBuildReason != null) {
            GriefPrevention.sendMessage(player, TextMode.Err, noBuildReason);
            event.setCancelled(true);
            return;
        }

        //send sign content to online administrators
        if (!GriefPrevention.instance.config_signNotifications) return;

        StringBuilder lines = new StringBuilder(" placed a sign @ " + GriefPrevention.getFriendlyLocationString(event.getBlock().getLocation()));
        boolean notEmpty = false;
        for (int i = 0; i < event.getLines().length; i++) {
            String withoutSpaces = event.getLine(i).replace(" ", "");
            if (!withoutSpaces.isEmpty()) {
                notEmpty = true;
                lines.append("\n  ").append(event.getLine(i));
            }
        }

        String signMessage = lines.toString();

        PlayerData playerData = dataStore.getPlayerData(player.getUniqueId());
        //if not empty and wasn't the same as the last sign, log it and remember it for later
        //This has been temporarily removed since `signMessage` includes location, not just the message. Waste of memory IMO
        //if(notEmpty && (playerData.lastSignMessage == null || !playerData.lastSignMessage.equals(signMessage)))
        if (notEmpty) {
            GriefPrevention.AddLogEntry(player.getName() + lines.toString().replace("\n  ", ";"), null);
            PlayerEventHandler.makeSocialLogEntry(player.getName(), signMessage);
            //playerData.lastSignMessage = signMessage;

            if (!player.hasPermission("griefprevention.eavesdropsigns")) {
                @SuppressWarnings("unchecked") Collection<Player> players = (Collection<Player>) GriefPrevention.instance.getServer().getOnlinePlayers();
                for (Player otherPlayer : players) {
                    if (otherPlayer.hasPermission("griefprevention.eavesdropsigns")) {
                        otherPlayer.sendMessage(ChatColor.GRAY + player.getName() + signMessage);
                    }
                }
            }
        }
    }
}
