package me.tinyoverflow.griefprevention.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.executors.CommandArguments;
import dev.jorel.commandapi.executors.PlayerCommandExecutor;
import me.tinyoverflow.griefprevention.*;
import org.bukkit.entity.Player;

public class RestrictSubClaimCommand extends BaseCommand implements PlayerCommandExecutor
{
    public RestrictSubClaimCommand(String commandName, GriefPrevention plugin)
    {
        super(commandName, plugin);
    }

    @Override
    public CommandAPICommand getCommand()
    {
        return new CommandAPICommand(getCommandName())
                .withPermission("griefprevention.restrictsubclaims")
                .executesPlayer(this);
    }

    @Override
    public void run(Player player, CommandArguments commandArguments) throws WrapperCommandSyntaxException
    {
        PlayerData playerData = getPlugin().getDataStore().getPlayerData(player.getUniqueId());
        Claim claim = getPlugin().getDataStore().getClaimAt(player.getLocation(), true, playerData.lastClaim);
        if (claim == null || claim.parent == null)
        {
            GriefPrevention.sendMessage(player, TextMode.ERROR, Messages.StandInSubclaim);
            return;
        }

        // If player has /ignoreclaims on, continue
        // If admin claim, fail if this user is not an admin
        // If not an admin claim, fail if this user is not the owner
        if (!playerData.ignoreClaims &&
            (claim.isAdminClaim() ? !player.hasPermission("griefprevention.adminclaims") : !player.getUniqueId().equals(
                    claim.parent.ownerID)))
        {
            GriefPrevention.sendMessage(player, TextMode.ERROR, Messages.OnlyOwnersModifyClaims, claim.getOwnerName());
            return;
        }

        if (claim.getSubclaimRestrictions())
        {
            claim.setSubclaimRestrictions(false);
            GriefPrevention.sendMessage(player, TextMode.SUCCESS, Messages.SubclaimUnrestricted);
        }
        else
        {
            claim.setSubclaimRestrictions(true);
            GriefPrevention.sendMessage(player, TextMode.SUCCESS, Messages.SubclaimRestricted);
        }
        getPlugin().getDataStore().saveClaim(claim);
    }
}
