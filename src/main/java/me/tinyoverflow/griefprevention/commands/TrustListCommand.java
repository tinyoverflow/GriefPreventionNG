package me.tinyoverflow.griefprevention.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.executors.CommandArguments;
import dev.jorel.commandapi.executors.PlayerCommandExecutor;
import me.tinyoverflow.griefprevention.Claim;
import me.tinyoverflow.griefprevention.ClaimPermission;
import me.tinyoverflow.griefprevention.GriefPrevention;
import me.tinyoverflow.griefprevention.Messages;
import me.tinyoverflow.griefprevention.TextMode;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Objects;
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
        return new CommandAPICommand(this.getCommandName())
                .withPermission("griefprevention.trustlist")
                .executesPlayer(this);
    }

    @Override
    public void run(Player player, CommandArguments commandArguments) throws WrapperCommandSyntaxException
    {
        Claim claim = this.getPlugin().getDataStore().getClaimAt(player.getLocation(), true, null);

        //if no claim here, error message
        if (claim == null)
        {
            GriefPrevention.sendMessage(player, TextMode.Err, Messages.TrustListNoClaim);
            return;
        }

        //if no permission to manage permissions, error message
        Supplier<String> errorMessage = claim.checkPermission(player, ClaimPermission.Manage, null);
        if (errorMessage != null)
        {
            GriefPrevention.sendMessage(player, TextMode.Err, errorMessage.get());
            return;
        }

        //otherwise build a list of explicit permissions by permission level
        //and send that to the player
        ArrayList<String> builders = new ArrayList<>();
        ArrayList<String> containers = new ArrayList<>();
        ArrayList<String> accessors = new ArrayList<>();
        ArrayList<String> managers = new ArrayList<>();
        claim.getPermissions(builders, containers, accessors, managers);

        GriefPrevention.sendMessage(player, TextMode.Info, Messages.TrustListHeader, claim.getOwnerName());

        StringBuilder permissions = new StringBuilder();
        permissions.append(ChatColor.GOLD).append('>');

        if (managers.size() > 0)
        {
            for (String manager : managers)
                permissions.append(this.getPlayerName(manager)).append(' ');
        }

        player.sendMessage(permissions.toString());
        permissions = new StringBuilder();
        permissions.append(ChatColor.YELLOW).append('>');

        if (builders.size() > 0)
        {
            for (String builder : builders)
                permissions.append(this.getPlayerName(builder)).append(' ');
        }

        player.sendMessage(permissions.toString());
        permissions = new StringBuilder();
        permissions.append(ChatColor.GREEN).append('>');

        if (containers.size() > 0)
        {
            for (String container : containers)
                permissions.append(this.getPlayerName(container)).append(' ');
        }

        player.sendMessage(permissions.toString());
        permissions = new StringBuilder();
        permissions.append(ChatColor.BLUE).append('>');

        if (accessors.size() > 0)
        {
            for (String accessor : accessors)
                permissions.append(this.getPlayerName(accessor)).append(' ');
        }

        player.sendMessage(permissions.toString());

        player.sendMessage(
                ChatColor.GOLD + this.getPlugin().getDataStore().getMessage(Messages.Manage) + " " +
                        ChatColor.YELLOW + this.getPlugin().getDataStore().getMessage(Messages.Build) + " " +
                        ChatColor.GREEN + this.getPlugin().getDataStore().getMessage(Messages.Containers) + " " +
                        ChatColor.BLUE + this.getPlugin().getDataStore().getMessage(Messages.Access));

        if (claim.getSubclaimRestrictions())
        {
            GriefPrevention.sendMessage(player, TextMode.Err, Messages.HasSubclaimRestriction);
        }
    }

    @NotNull
    private String getPlayerName(String manager)
    {
        return Optional
                .ofNullable(this.getPlugin().getServer().getOfflinePlayer(UUID.fromString(manager)).getName())
                .orElse("someone");
    }
}
