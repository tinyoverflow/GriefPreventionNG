package me.tinyoverflow.griefprevention.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.executors.CommandArguments;
import dev.jorel.commandapi.executors.PlayerCommandExecutor;
import me.tinyoverflow.griefprevention.*;
import me.tinyoverflow.griefprevention.logger.ActivityType;
import org.bukkit.entity.Player;

public class DeleteClaimCommand extends BaseCommand implements PlayerCommandExecutor
{
    public DeleteClaimCommand(String commandName, GriefPrevention plugin)
    {
        super(commandName, plugin);
    }

    @Override
    public CommandAPICommand getCommand()
    {
        return new CommandAPICommand(getCommandName())
                .withPermission("griefprevention.deleteclaim")
                .executesPlayer(this);
    }

    @Override
    public void run(Player player, CommandArguments commandArguments) throws WrapperCommandSyntaxException
    {
        //determine which claim the player is standing in
        Claim claim = getPlugin().getDataStore().getClaimAt(player.getLocation(), true /*ignore height*/, null);

        if (claim == null)
        {
            GriefPrevention.sendMessage(player, TextMode.ERROR, Messages.DeleteClaimMissing);
        }
        else
        {
            //deleting an admin claim additionally requires the adminclaims permission
            if (!claim.isAdminClaim() || player.hasPermission("griefprevention.adminclaims"))
            {
                PlayerData playerData = getPlugin().getDataStore().getPlayerData(player.getUniqueId());
                if (claim.children.size() > 0 && !playerData.warnedAboutMajorDeletion)
                {
                    GriefPrevention.sendMessage(player, TextMode.WARNING, Messages.DeletionSubdivisionWarning);
                    playerData.warnedAboutMajorDeletion = true;
                }
                else
                {
                    claim.removeSurfaceFluids(null);
                    getPlugin().getDataStore().deleteClaim(claim, true, true);

                    //if in a creative mode world, /restorenature the claim
                    if (getPlugin().creativeRulesApply(claim.getLesserBoundaryCorner()) ||
                        getPlugin().getPluginConfig().getClaimConfiguration().getRestorationConfiguration().isEnabled())
                    {
                        getPlugin().restoreClaim(claim, 0);
                    }

                    GriefPrevention.sendMessage(player, TextMode.SUCCESS, Messages.DeleteSuccess);
                    GriefPrevention.AddLogEntry(
                            player.getName() + " deleted " + claim.getOwnerName() + "'s claim at " +
                            GriefPrevention.getFriendlyLocationString(claim.getLesserBoundaryCorner()),
                            ActivityType.ADMIN
                    );

                    //revert any current visualization
                    playerData.setVisibleBoundaries(null);

                    playerData.warnedAboutMajorDeletion = false;
                }
            }
            else
            {
                GriefPrevention.sendMessage(player, TextMode.ERROR, Messages.CantDeleteAdminClaim);
            }
        }
    }
}
