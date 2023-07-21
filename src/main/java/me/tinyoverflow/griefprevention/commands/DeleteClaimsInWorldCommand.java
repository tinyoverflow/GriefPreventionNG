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

public class DeleteClaimsInWorldCommand extends BaseCommand implements ConsoleCommandExecutor
{
    public DeleteClaimsInWorldCommand(String commandName, GriefPrevention plugin)
    {
        super(commandName, plugin);
    }

    @Override
    public CommandAPICommand getCommand()
    {
        return new CommandAPICommand(this.getCommandName())
                .withPermission("griefprevention.deleteclaimsinworld")
                .withArguments(new WorldArgument("world"))
                .executesConsole(this);
    }

    @Override
    public void run(ConsoleCommandSender sender, CommandArguments commandArguments) throws WrapperCommandSyntaxException
    {
        //try to find the specified world
        World world = (World) commandArguments.get("world");

        //delete all claims in that world
        this.getPlugin().getDataStore().deleteClaimsInWorld(world, true);
        GriefPrevention.AddLogEntry("Deleted all claims in world: " + world.getName() + ".", CustomLogEntryTypes.AdminActivity);
    }
}
