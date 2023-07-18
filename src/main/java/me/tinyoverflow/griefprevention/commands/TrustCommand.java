package me.tinyoverflow.griefprevention.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.CustomArgument;
import dev.jorel.commandapi.arguments.OfflinePlayerArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.executors.CommandArguments;
import dev.jorel.commandapi.executors.PlayerCommandExecutor;
import me.tinyoverflow.griefprevention.Claim;
import me.tinyoverflow.griefprevention.ClaimPermission;
import me.tinyoverflow.griefprevention.GriefPrevention;
import me.tinyoverflow.griefprevention.Messages;
import me.tinyoverflow.griefprevention.PlayerData;
import me.tinyoverflow.griefprevention.TextMode;
import me.tinyoverflow.griefprevention.events.TrustChangedEvent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Supplier;

import static dev.jorel.commandapi.arguments.CustomArgument.CustomArgumentException;
import static dev.jorel.commandapi.arguments.CustomArgument.MessageBuilder;

public class TrustCommand implements BaseCommand, PlayerCommandExecutor
{
    private final GriefPrevention plugin;

    public TrustCommand(GriefPrevention plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public CommandAPICommand getCommand()
    {
        return new CommandAPICommand("trust")
                .withPermission("griefprevention.trust")
                .withArguments(new OfflinePlayerArgument("target"))
                .withOptionalArguments(this.trustLevelArgument("level"))
                .executesPlayer(this);
    }

    @Override
    public void run(Player player, CommandArguments arguments) throws WrapperCommandSyntaxException
    {
        OfflinePlayer target = (Player) arguments.get("target");
        ClaimPermission permissionLevel = (ClaimPermission) arguments
                .getOptional("level")
                .orElse(ClaimPermission.Build);

        //determine which claim the player is standing in
        Claim claim = this.plugin.getDataStore().getClaimAt(player.getLocation(), true /*ignore height*/, null);

        //determine which claims should be modified
        ArrayList<Claim> targetClaims = new ArrayList<>();
        if (claim == null)
        {
            PlayerData playerData = this.plugin.getDataStore().getPlayerData(player.getUniqueId());
            targetClaims.addAll(playerData.getClaims());
        }
        else
        {
            //check permission here
            if (claim.checkPermission(player, ClaimPermission.Manage, null) != null)
            {
                GriefPrevention.sendMessage(player, TextMode.Err, Messages.NoPermissionTrust, claim.getOwnerName());
                return;
            }

            //see if the player has the level of permission he's trying to grant
            Supplier<String> errorMessage;

            //permission level null indicates granting permission trust
            errorMessage = claim.checkPermission(player, permissionLevel, null);

            //error message for trying to grant a permission the player doesn't have
            if (errorMessage != null)
            {
                GriefPrevention.sendMessage(player, TextMode.Err, Messages.CantGrantThatPermission);
                return;
            }

            targetClaims.add(claim);
        }

        //if we didn't determine which claims to modify, tell the player to be specific
        if (targetClaims.size() == 0)
        {
            GriefPrevention.sendMessage(player, TextMode.Err, Messages.GrantPermissionNoClaim);
            return;
        }

        String identifierToAdd = target.getUniqueId().toString();

        //calling the event
        TrustChangedEvent event = new TrustChangedEvent(player, targetClaims, permissionLevel, true, identifierToAdd);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled())
        {
            return;
        }

        //apply changes
        for (Claim currentClaim : event.getClaims())
        {
            currentClaim.setPermission(identifierToAdd, permissionLevel);
            this.plugin.getDataStore().saveClaim(currentClaim);
        }

        //notify player
        String permissionDescription = switch (permissionLevel) {
            case Build -> this.plugin.getDataStore().getMessage(Messages.BuildPermission);
            case Inventory -> this.plugin.getDataStore().getMessage(Messages.ContainersPermission);
            case Access -> this.plugin.getDataStore().getMessage(Messages.AccessPermission);
            case Manage -> this.plugin.getDataStore().getMessage(Messages.PermissionsPermission);
            default -> "";
        };

        String location = claim == null
                ? this.plugin.getDataStore().getMessage(Messages.LocationAllClaims)
                : this.plugin.getDataStore().getMessage(Messages.LocationCurrentClaim);

        GriefPrevention.sendMessage(
                player,
                TextMode.Success,
                Messages.GrantPermissionConfirmation,
                target.getName(),
                permissionDescription,
                location
        );
    }

    private Argument<ClaimPermission> trustLevelArgument(String nodeName)
    {
        return new CustomArgument<>(new StringArgument(nodeName), info -> {
            try
            {
                return ClaimPermission.valueOf(info.input());
            }
            catch (IllegalArgumentException exception)
            {
                throw CustomArgumentException.fromMessageBuilder(
                        new MessageBuilder("Unknown trust level: ").appendArgInput()
                );
            }
        }).replaceSuggestions(ArgumentSuggestions.strings(info -> Arrays
                .stream(ClaimPermission.values())
                .map(ClaimPermission::name)
                .map(String::toLowerCase)
                .filter(level -> !level.equals("edit"))
                .toArray(String[]::new))
        );
    }
}
