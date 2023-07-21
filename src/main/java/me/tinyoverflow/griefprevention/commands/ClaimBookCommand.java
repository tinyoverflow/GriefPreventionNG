package me.tinyoverflow.griefprevention.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.PlayerArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.executors.CommandArguments;
import dev.jorel.commandapi.executors.PlayerCommandExecutor;
import me.tinyoverflow.griefprevention.GriefPrevention;
import me.tinyoverflow.griefprevention.tasks.WelcomeTask;
import org.bukkit.entity.Player;

public class ClaimBookCommand extends BaseCommand implements PlayerCommandExecutor
{
    public ClaimBookCommand(String commandName, GriefPrevention plugin)
    {
        super(commandName, plugin);
    }

    @Override
    public CommandAPICommand getCommand()
    {
        return new CommandAPICommand("claimbook")
                .withPermission("griefprevention.claimbook")
                .withArguments(new PlayerArgument("target"))
                .executesPlayer(this);
    }

    @Override
    public void run(Player player, CommandArguments commandArguments) throws WrapperCommandSyntaxException
    {
        Player target = (Player) commandArguments.getOptional("target").orElse(player);
        new WelcomeTask(target).run();
    }
}
