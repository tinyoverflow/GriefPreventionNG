package me.tinyoverflow.griefprevention.commands;

import dev.jorel.commandapi.CommandAPICommand;
import me.tinyoverflow.griefprevention.GriefPrevention;

public abstract class BaseCommand
{
    private final String commandName;
    private final GriefPrevention plugin;

    public BaseCommand(String commandName, GriefPrevention plugin)
    {
        this.commandName = commandName;
        this.plugin = plugin;
    }

    public GriefPrevention getPlugin()
    {
        return plugin;
    }

    public String getCommandName()
    {
        return commandName;
    }

    public abstract CommandAPICommand getCommand();
}
