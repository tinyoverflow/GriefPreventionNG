package me.tinyoverflow.griefprevention.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.PlayerArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.executors.CommandArguments;
import dev.jorel.commandapi.executors.PlayerCommandExecutor;
import me.tinyoverflow.griefprevention.GriefPrevention;
import me.tinyoverflow.griefprevention.Messages;
import me.tinyoverflow.griefprevention.PlayerData;
import me.tinyoverflow.griefprevention.TextMode;
import org.bukkit.entity.Player;

import java.util.Optional;

public class UnlockItemsCommand extends BaseCommand implements PlayerCommandExecutor
{
    public UnlockItemsCommand(String commandName, GriefPrevention plugin)
    {
        super(commandName, plugin);
    }

    @Override
    public CommandAPICommand getCommand()
    {
        return new CommandAPICommand(getCommandName())
                .withPermission("griefprevention.unlockitems")
                .withOptionalArguments(new PlayerArgument("target"))
                .executesPlayer(this);
    }

    @Override
    public void run(Player player, CommandArguments commandArguments) throws WrapperCommandSyntaxException
    {
        Optional<Object> targetPlayer = commandArguments.getOptional("target");
        PlayerData playerData;

        if (player.hasPermission("griefprevention.unlockothersdrops") && targetPlayer.isPresent())
        {
            Player otherPlayer = (Player) targetPlayer.orElseThrow();
            playerData = getPlugin().getDataStore().getPlayerData(otherPlayer.getUniqueId());
            GriefPrevention.sendMessage(
                    player,
                    TextMode.SUCCESS,
                    Messages.DropUnlockOthersConfirmation,
                    otherPlayer.getName()
            );
        }
        else
        {
            playerData = getPlugin().getDataStore().getPlayerData(player.getUniqueId());
            GriefPrevention.sendMessage(player, TextMode.SUCCESS, Messages.DropUnlockConfirmation);
        }

        playerData.dropsAreUnlocked = true;
    }
}
