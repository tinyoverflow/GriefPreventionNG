package me.tinyoverflow.griefprevention.commands;

import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.executors.CommandArguments;
import dev.jorel.commandapi.executors.PlayerCommandExecutor;
import me.tinyoverflow.griefprevention.*;
import org.bukkit.entity.Player;

public class ToolModeCommand implements PlayerCommandExecutor
{
    private final GriefPrevention plugin;
    private final ShovelMode shovelMode;

    public ToolModeCommand(GriefPrevention plugin, ShovelMode shovelMode)
    {
        this.plugin = plugin;
        this.shovelMode = shovelMode;
    }

    @Override
    public void run(Player sender, CommandArguments args) throws WrapperCommandSyntaxException
    {
        PlayerData playerData = plugin.getDataStore().getPlayerData(sender.getUniqueId());
        playerData.shovelMode = shovelMode;

        GriefPrevention.sendMessage(sender, TextMode.SUCCESS, switch (shovelMode)
        {
            case BASIC -> Messages.BasicClaimsMode;
            case SUBDIVIDE -> Messages.SubdivisionMode;
            case ADMIN -> Messages.AdminClaimsMode;
            default -> throw new IllegalStateException("Unexpected value: " + shovelMode);
        });
    }

    public void help(Player sender, CommandArguments commandArguments)
    {
        PlayerData playerData = plugin.getDataStore().getPlayerData(sender.getUniqueId());

        sender.sendMessage("Your current mode: " + playerData.shovelMode.name());
    }
}
