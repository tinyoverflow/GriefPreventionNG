package me.tinyoverflow.griefprevention.commands;

import java.util.ArrayList;
import java.util.List;

public class CommandManager
{
    private final List<BaseCommand> commands = new ArrayList<>();

    public void add(BaseCommand command) {
        this.commands.add(command);
    }

    public void register() {
        commands.forEach(command -> command.getCommand().register());
    }
}
