package me.tinyoverflow.griefprevention.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.executors.CommandArguments;
import dev.jorel.commandapi.executors.PlayerCommandExecutor;
import me.tinyoverflow.griefprevention.*;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public class TrustListCommand extends BaseCommand implements PlayerCommandExecutor
{
    public TrustListCommand(String commandName, GriefPrevention plugin)
    {
        super(commandName, plugin);
    }

    @Override
    public CommandAPICommand getCommand()
    {
        return new CommandAPICommand(getCommandName())
                .withPermission("griefprevention.trustlist")
                .executesPlayer(this);
    }

    @Override
    public void run(Player player, CommandArguments commandArguments) throws WrapperCommandSyntaxException
    {
        Claim claim = getPlugin().getDataStore().getClaimAt(player.getLocation(), true, null);

        //if no claim here, error message
        if (claim == null)
        {
            GriefPrevention.sendMessage(player, TextMode.ERROR, Messages.TrustListNoClaim);
            return;
        }

        //if no permission to manage permissions, error message
        Supplier<String> errorMessage = claim.checkPermission(player, ClaimPermission.Manage, null);
        if (errorMessage != null)
        {
            GriefPrevention.sendMessage(player, TextMode.ERROR, errorMessage.get());
            return;
        }

        //otherwise build a list of explicit permissions by permission level
        //and send that to the player
        ArrayList<String> builders = new ArrayList<>();
        ArrayList<String> containers = new ArrayList<>();
        ArrayList<String> accessors = new ArrayList<>();
        ArrayList<String> managers = new ArrayList<>();
        claim.getPermissions(builders, containers, accessors, managers);

        GriefPrevention.sendMessage(player, TextMode.INFO, Messages.TrustListHeader, claim.getOwnerName());

        StringBuilder permissions = new StringBuilder();
        permissions.append(ChatColor.GOLD).append('>');

        if (managers.size() > 0)
        {
            for (String manager : managers)
                permissions.append(getPlayerName(manager)).append(' ');
        }

        player.sendMessage(permissions.toString());
        permissions = new StringBuilder();
        permissions.append(ChatColor.YELLOW).append('>');

        if (builders.size() > 0)
        {
            for (String builder : builders)
                permissions.append(getPlayerName(builder)).append(' ');
        }

        player.sendMessage(permissions.toString());
        permissions = new StringBuilder();
        permissions.append(ChatColor.GREEN).append('>');

        if (containers.size() > 0)
        {
            for (String container : containers)
                permissions.append(getPlayerName(container)).append(' ');
        }

        player.sendMessage(permissions.toString());
        permissions = new StringBuilder();
        permissions.append(ChatColor.BLUE).append('>');

        if (accessors.size() > 0)
        {
            for (String accessor : accessors)
                permissions.append(getPlayerName(accessor)).append(' ');
        }

        player.sendMessage(permissions.toString());

        player.sendMessage(
                ChatColor.GOLD + getPlugin().getDataStore().getMessage(Messages.Manage) + " " +
                ChatColor.YELLOW + getPlugin().getDataStore().getMessage(Messages.Build) + " " +
                ChatColor.GREEN + getPlugin().getDataStore().getMessage(Messages.Containers) + " " +
                ChatColor.BLUE + getPlugin().getDataStore().getMessage(Messages.Access));

        if (claim.getSubclaimRestrictions())
        {
            GriefPrevention.sendMessage(player, TextMode.ERROR, Messages.HasSubclaimRestriction);
        }
    }

    @NotNull
    private String getPlayerName(String manager)
    {
        return Optional
                .ofNullable(getPlugin().getServer().getOfflinePlayer(UUID.fromString(manager)).getName())
                .orElse("someone");
    }
}
