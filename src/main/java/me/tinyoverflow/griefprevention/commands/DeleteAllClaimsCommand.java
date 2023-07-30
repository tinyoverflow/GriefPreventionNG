package me.tinyoverflow.griefprevention.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.OfflinePlayerArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.executors.CommandArguments;
import dev.jorel.commandapi.executors.PlayerCommandExecutor;
import me.tinyoverflow.griefprevention.GriefPrevention;
import me.tinyoverflow.griefprevention.Messages;
import me.tinyoverflow.griefprevention.TextMode;
import me.tinyoverflow.griefprevention.logger.LogType;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class DeleteAllClaimsCommand extends BaseCommand implements PlayerCommandExecutor
{
    public DeleteAllClaimsCommand(String commandName, GriefPrevention plugin)
    {
        super(commandName, plugin);
    }

    @Override
    public CommandAPICommand getCommand()
    {
        return new CommandAPICommand(getCommandName())
                .withPermission("griefprevention.deleteallclaims")
                .withArguments(new OfflinePlayerArgument("target"))
                .executesPlayer(this);
    }

    @Override
    public void run(Player player, CommandArguments commandArguments) throws WrapperCommandSyntaxException
    {
        //try to find that player
        OfflinePlayer otherPlayer = (OfflinePlayer) commandArguments.get("target");

        //delete all that player's claims
        getPlugin().getDataStore().deleteClaimsForPlayer(otherPlayer.getUniqueId(), true);

        GriefPrevention.sendMessage(player, TextMode.Success, Messages.DeleteAllSuccess, otherPlayer.getName());
        if (player != null)
        {
            GriefPrevention.AddLogEntry(player.getName() + " deleted all claims belonging to " + otherPlayer.getName() + ".", LogType.ADMIN);

            //revert any current visualization
            if (player.isOnline())
            {
                getPlugin().getDataStore().getPlayerData(player.getUniqueId()).setVisibleBoundaries(null);
            }
        }
    }
}
