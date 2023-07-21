package me.tinyoverflow.griefprevention.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.WorldArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.executors.CommandArguments;
import dev.jorel.commandapi.executors.ConsoleCommandExecutor;
import me.tinyoverflow.griefprevention.CustomLogEntryTypes;
import me.tinyoverflow.griefprevention.GriefPrevention;
import org.bukkit.World;
import org.bukkit.command.ConsoleCommandSender;

public class DeleteUserClaimsInWorldCommand extends BaseCommand implements ConsoleCommandExecutor
{
    public DeleteUserClaimsInWorldCommand(String commandName, GriefPrevention plugin)
    {
        super(commandName, plugin);
    }

    @Override
    public CommandAPICommand getCommand()
    {
        return new CommandAPICommand(this.getCommandName())
                .withPermission("griefprevention.deleteuserclaimsinworld")
                .withArguments(new WorldArgument("world"))
                .executesConsole(this);
    }

    @Override
    public void run(ConsoleCommandSender sender, CommandArguments commandArguments) throws WrapperCommandSyntaxException
    {
        World world = (World) commandArguments.get("world");

        //delete all USER claims in that world
        this.getPlugin().getDataStore().deleteClaimsInWorld(world, false);
        GriefPrevention.AddLogEntry("Deleted all user claims in world: " + world.getName() + ".", CustomLogEntryTypes.AdminActivity);
    }
}
