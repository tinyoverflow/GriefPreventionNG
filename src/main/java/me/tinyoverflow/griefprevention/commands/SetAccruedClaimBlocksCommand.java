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
import me.tinyoverflow.griefprevention.logger.ActivityType;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class SetAccruedClaimBlocksCommand extends BaseCommand implements PlayerCommandExecutor
{
    public SetAccruedClaimBlocksCommand(String commandName, GriefPrevention plugin)
    {
        super(commandName, plugin);
    }

    @Override
    public CommandAPICommand getCommand()
    {
        return new CommandAPICommand(getCommandName())
                .withPermission("griefprevention.setaccruedclaimblocks")
                .withArguments(new OfflinePlayerArgument("target"))
                .withArguments(new IntegerArgument("amount", 0))
                .executesPlayer(this);
    }

    @Override
    public void run(Player player, CommandArguments commandArguments) throws WrapperCommandSyntaxException
    {
        //parse the adjustment amount
        Integer newAmount = (Integer) commandArguments.get("amount");
        OfflinePlayer targetPlayer = (OfflinePlayer) commandArguments.get("target");

        //set player's blocks
        PlayerData playerData = getPlugin().getDataStore().getPlayerData(targetPlayer.getUniqueId());
        playerData.setAccruedClaimBlocks(newAmount);
        getPlugin().getDataStore().savePlayerData(targetPlayer.getUniqueId(), playerData);

        GriefPrevention.sendMessage(player, TextMode.SUCCESS, Messages.SetClaimBlocksSuccess);
        if (player != null)
        {
            GriefPrevention.AddLogEntry(
                    player.getName() + " set " + targetPlayer.getName() + "'s accrued claim blocks to " + newAmount +
                    ".", ActivityType.ADMIN);
        }
    }
}
