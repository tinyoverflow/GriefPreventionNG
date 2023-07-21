package me.tinyoverflow.griefprevention.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.OfflinePlayerArgument;
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

public class UntrustCommand extends BaseCommand implements PlayerCommandExecutor
{
    public UntrustCommand(String commandName, GriefPrevention plugin)
    {
        super(commandName, plugin);
    }

    @Override
    public CommandAPICommand getCommand()
    {
        return new CommandAPICommand(this.getCommandName())
                .withPermission("griefprevention.untrust")
                .withArguments(new OfflinePlayerArgument("target"))
                .executesPlayer(this);
    }

    @Override
    public void run(Player player, CommandArguments arguments) throws WrapperCommandSyntaxException
    {
        //determine which claim the player is standing in
        Claim claim = this.getPlugin().getDataStore().getClaimAt(player.getLocation(), true /*ignore height*/, null);

        //determine whether a single player or clearing permissions entirely
        boolean clearPermissions = false;
        OfflinePlayer otherPlayer = (OfflinePlayer) arguments.get("target");
        /*if (args[0].equals("all"))
        {
            if (claim == null || claim.checkPermission(player, ClaimPermission.Edit, null) == null)
            {
                clearPermissions = true;
            }
            else
            {
                GriefPrevention.sendMessage(player, TextMode.Err, Messages.ClearPermsOwnerOnly);
                return;
            }
        }*/

        //if no claim here, apply changes to all his claims
        if (claim == null)
        {
            PlayerData playerData = this.getPlugin().getDataStore().getPlayerData(player.getUniqueId());

            String idToDrop = otherPlayer.getUniqueId().toString();

            //calling event
            TrustChangedEvent event = new TrustChangedEvent(player, playerData.getClaims(), null, false, idToDrop);
            Bukkit.getPluginManager().callEvent(event);

            if (event.isCancelled())
            {
                return;
            }

            //dropping permissions
            for (Claim targetClaim : event.getClaims()) {
                claim = targetClaim;

                //if untrusting "all" drop all permissions
                if (clearPermissions)
                {
                    claim.clearPermissions();
                }

                //otherwise drop individual permissions
                else
                {
                    claim.dropPermission(idToDrop);
                    claim.managers.remove(idToDrop);
                }

                //save changes
                this.getPlugin().getDataStore().saveClaim(claim);
            }

            //beautify for output
            /*if (args[0].equals("public"))
            {
                args[0] = "the public";
            }*/

            //confirmation message
            if (!clearPermissions)
            {
                GriefPrevention.sendMessage(player, TextMode.Success, Messages.UntrustIndividualAllClaims, otherPlayer.getName());
            }
            else
            {
                GriefPrevention.sendMessage(player, TextMode.Success, Messages.UntrustEveryoneAllClaims);
            }
        }

        //otherwise, apply changes to only this claim
        else if (claim.checkPermission(player, ClaimPermission.Manage, null) != null)
        {
            GriefPrevention.sendMessage(player, TextMode.Err, Messages.NoPermissionTrust, claim.getOwnerName());
            return;
        }
        else
        {
            //if clearing all
            if (clearPermissions)
            {
                //requires owner
                if (claim.checkPermission(player, ClaimPermission.Edit, null) != null)
                {
                    GriefPrevention.sendMessage(player, TextMode.Err, Messages.UntrustAllOwnerOnly);
                    return;
                }

                //calling the event
                TrustChangedEvent event = new TrustChangedEvent(player, claim, null, false, otherPlayer.getName());
                Bukkit.getPluginManager().callEvent(event);

                if (event.isCancelled())
                {
                    return;
                }

                event.getClaims().forEach(Claim::clearPermissions);
                GriefPrevention.sendMessage(player, TextMode.Success, Messages.ClearPermissionsOneClaim);
            }

            //otherwise individual permission drop
            else
            {
                String idToDrop = otherPlayer.getUniqueId().toString();

                boolean targetIsManager = claim.managers.contains(idToDrop);
                if (targetIsManager && claim.checkPermission(player, ClaimPermission.Edit, null) != null)  //only claim owners can untrust managers
                {
                    GriefPrevention.sendMessage(player, TextMode.Err, Messages.ManagersDontUntrustManagers, claim.getOwnerName());
                    return;
                }
                else
                {
                    //calling the event
                    TrustChangedEvent event = new TrustChangedEvent(player, claim, null, false, idToDrop);
                    Bukkit.getPluginManager().callEvent(event);

                    if (event.isCancelled())
                    {
                        return;
                    }

                    event.getClaims().forEach(targetClaim -> targetClaim.dropPermission(event.getIdentifier()));

                    //beautify for output
                    /*if (args[0].equals("public"))
                    {
                        args[0] = "the public";
                    }*/

                    GriefPrevention.sendMessage(player, TextMode.Success, Messages.UntrustIndividualSingleClaim, otherPlayer.getName());
                }
            }

            //save changes
            this.getPlugin().getDataStore().saveClaim(claim);
        }
    }
}
