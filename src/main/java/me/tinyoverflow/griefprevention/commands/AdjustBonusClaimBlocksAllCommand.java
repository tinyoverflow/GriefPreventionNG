package me.tinyoverflow.griefprevention.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.executors.CommandArguments;
import dev.jorel.commandapi.executors.PlayerCommandExecutor;
import me.tinyoverflow.griefprevention.GriefPrevention;
import me.tinyoverflow.griefprevention.Messages;
import me.tinyoverflow.griefprevention.PlayerData;
import me.tinyoverflow.griefprevention.TextMode;
import me.tinyoverflow.griefprevention.logger.LogType;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.UUID;

public class AdjustBonusClaimBlocksAllCommand extends BaseCommand implements PlayerCommandExecutor
{
    public AdjustBonusClaimBlocksAllCommand(String commandName, GriefPrevention plugin)
    {
        super(commandName, plugin);
    }

    @Override
    public CommandAPICommand getCommand()
    {
        return new CommandAPICommand(getCommandName())
                .withPermission("griefprevention.adjustbonusclaimblocksall")
                .withArguments(new IntegerArgument("limit", 0))
                .executesPlayer(this);
    }

    @Override
    public void run(Player player, CommandArguments commandArguments) throws WrapperCommandSyntaxException
    {
        int adjustment = (int) commandArguments.get("limit");

        Collection<? extends Player> players = getPlugin().getServer().getOnlinePlayers();
        StringBuilder builder = new StringBuilder();
        for (Player onlinePlayer : players)
        {
            UUID playerID = onlinePlayer.getUniqueId();
            PlayerData playerData = getPlugin().getDataStore().getPlayerData(playerID);
            playerData.setBonusClaimBlocks(playerData.getBonusClaimBlocks() + adjustment);
            getPlugin().getDataStore().savePlayerData(playerID, playerData);
            builder.append(onlinePlayer.getName()).append(' ');
        }

        GriefPrevention.sendMessage(player, TextMode.Success, Messages.AdjustBlocksAllSuccess, String.valueOf(adjustment));
        GriefPrevention.AddLogEntry("Adjusted all " + players.size() + "players' bonus claim blocks by " + adjustment + ".  " + builder, LogType.ADMIN);
    }
}
