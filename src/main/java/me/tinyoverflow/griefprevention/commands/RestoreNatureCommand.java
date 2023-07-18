package me.tinyoverflow.griefprevention.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.executors.CommandArguments;
import dev.jorel.commandapi.executors.PlayerCommandExecutor;
import me.tinyoverflow.griefprevention.GriefPrevention;
import me.tinyoverflow.griefprevention.Messages;
import me.tinyoverflow.griefprevention.PlayerData;
import me.tinyoverflow.griefprevention.ShovelMode;
import me.tinyoverflow.griefprevention.TextMode;
import org.bukkit.entity.Player;

public class RestoreNatureCommand extends BaseCommand implements PlayerCommandExecutor
{
    public RestoreNatureCommand(String commandName, GriefPrevention plugin)
    {
        super(commandName, plugin);
    }

    @Override
    public CommandAPICommand getCommand()
    {
        return new CommandAPICommand(this.getCommandName())
                .withPermission("griefprevention.restorenature")
                .withOptionalArguments(new BooleanArgument("aggressive"))
                .executesPlayer(this);
    }

    @Override
    public void run(Player player, CommandArguments arguments) throws WrapperCommandSyntaxException
    {
        boolean aggressive = (boolean) arguments.getOptional("aggressive").orElse(false);
        PlayerData playerData = this.plugin.getDataStore().getPlayerData(player.getUniqueId());

        playerData.shovelMode = aggressive
                ? ShovelMode.RestoreNatureAggressive
                : ShovelMode.RestoreNature;

        GriefPrevention.sendMessage(player, TextMode.Instr, aggressive
                ? Messages.RestoreNatureAggressiveActivate
                : Messages.RestoreNatureActivate);
    }
}
