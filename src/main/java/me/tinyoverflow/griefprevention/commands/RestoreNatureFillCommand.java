package me.tinyoverflow.griefprevention.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.executors.CommandArguments;
import dev.jorel.commandapi.executors.PlayerCommandExecutor;
import me.tinyoverflow.griefprevention.*;
import org.bukkit.entity.Player;

public class RestoreNatureFillCommand extends BaseCommand implements PlayerCommandExecutor
{
    public RestoreNatureFillCommand(String commandName, GriefPrevention plugin)
    {
        super(commandName, plugin);
    }

    @Override
    public CommandAPICommand getCommand()
    {
        return new CommandAPICommand(getCommandName())
                .withPermission("griefprevention.restorenaturefill")
                .withOptionalArguments(new IntegerArgument("radius", 1))
                .executesPlayer(this);
    }

    @Override
    public void run(Player player, CommandArguments arguments) throws WrapperCommandSyntaxException
    {
        PlayerData playerData = getPlugin().getDataStore().getPlayerData(player.getUniqueId());

        playerData.shovelMode = ShovelMode.RestoreNatureFill;
        playerData.fillRadius = (int) arguments.getOptional("radius").orElse(2);

        GriefPrevention.sendMessage(
                player,
                TextMode.SUCCESS,
                Messages.FillModeActive,
                String.valueOf(playerData.fillRadius)
        );
    }
}
