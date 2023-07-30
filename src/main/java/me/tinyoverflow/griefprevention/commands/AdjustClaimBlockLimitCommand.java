package me.tinyoverflow.griefprevention.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.OfflinePlayerArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.executors.CommandArguments;
import dev.jorel.commandapi.executors.PlayerCommandExecutor;
import me.tinyoverflow.griefprevention.GriefPrevention;
import me.tinyoverflow.griefprevention.Messages;
import me.tinyoverflow.griefprevention.PlayerData;
import me.tinyoverflow.griefprevention.TextMode;
import me.tinyoverflow.griefprevention.logger.LogType;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class AdjustClaimBlockLimitCommand extends BaseCommand implements PlayerCommandExecutor
{
    public AdjustClaimBlockLimitCommand(String commandName, GriefPrevention plugin)
    {
        super(commandName, plugin);
    }

    @Override
    public CommandAPICommand getCommand()
    {
        return new CommandAPICommand(getCommandName())
                .withPermission("griefprevention.adjustclaimblocklimit")
                .withArguments(new OfflinePlayerArgument("target"))
                .withArguments(new IntegerArgument("limit", 0))
                .executesPlayer(this);
    }

    @Override
    public void run(Player player, CommandArguments commandArguments) throws WrapperCommandSyntaxException
    {
        int limit = (int) commandArguments.get("limit");
        OfflinePlayer targetPlayer = (OfflinePlayer) commandArguments.get("target");
        if (targetPlayer == null)
        {
            GriefPrevention.sendMessage(player, TextMode.Err, Messages.PlayerNotFound2);
            return;
        }

        PlayerData playerData = getPlugin().getDataStore().getPlayerData(targetPlayer.getUniqueId());
        playerData.setAccruedClaimBlocksLimit(limit);
        getPlugin().getDataStore().savePlayerData(targetPlayer.getUniqueId(), playerData);

        GriefPrevention.sendMessage(player, TextMode.Success, Messages.AdjustLimitSuccess, targetPlayer.getName(), String.valueOf(limit));
        if (player != null)
            GriefPrevention.AddLogEntry(player.getName() + " adjusted " + targetPlayer.getName() + "'s max accrued claim block limit to " + limit + ".", LogType.ADMIN);

    }
}
