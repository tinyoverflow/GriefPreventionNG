package me.tinyoverflow.griefprevention.commands;

import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.executors.CommandArguments;
import dev.jorel.commandapi.executors.PlayerCommandExecutor;
import me.tinyoverflow.griefprevention.GriefPrevention;
import me.tinyoverflow.griefprevention.Messages;
import me.tinyoverflow.griefprevention.TextMode;
import me.tinyoverflow.griefprevention.logger.ActivityType;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class DeleteAllClaimsCommand implements PlayerCommandExecutor
{
    private final GriefPrevention plugin;

    public DeleteAllClaimsCommand(GriefPrevention plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public void run(Player player, CommandArguments commandArguments) throws WrapperCommandSyntaxException
    {
        //try to find that player
        OfflinePlayer otherPlayer = (OfflinePlayer) commandArguments.get("target");

        //delete all that player's claims
        plugin.getDataStore().deleteClaimsForPlayer(otherPlayer.getUniqueId(), true);

        GriefPrevention.sendMessage(player, TextMode.SUCCESS, Messages.DeleteAllSuccess, otherPlayer.getName());
        if (player != null) {
            GriefPrevention.AddLogEntry(
                    player.getName() + " deleted all claims belonging to " + otherPlayer.getName() + ".",
                    ActivityType.ADMIN
            );

            //revert any current visualization
            if (player.isOnline()) {
                plugin.getDataStore().getPlayerData(player.getUniqueId()).setVisibleBoundaries(null);
            }
        }
    }
}
