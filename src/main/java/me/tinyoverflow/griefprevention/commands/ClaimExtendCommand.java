package me.tinyoverflow.griefprevention.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.executors.CommandArguments;
import dev.jorel.commandapi.executors.PlayerCommandExecutor;
import me.tinyoverflow.griefprevention.*;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.function.Supplier;

public class ClaimExtendCommand extends BaseCommand implements PlayerCommandExecutor
{
    public ClaimExtendCommand(String commandName, GriefPrevention plugin)
    {
        super(commandName, plugin);
    }

    @Override
    public CommandAPICommand getCommand()
    {
        return new CommandAPICommand(getCommandName())
                .withPermission("griefprevention.extendclaim")
                .withArguments(new IntegerArgument("amount"))
                .executesPlayer(this);
    }

    @Override
    public void run(Player player, CommandArguments arguments) throws WrapperCommandSyntaxException
    {
        int amount = (Integer) arguments.get("amount");

        //requires claim modification tool in hand
        if (player.getGameMode() != GameMode.CREATIVE &&
            player.getInventory().getItemInMainHand().getType() !=
            GriefPrevention.instance.getPluginConfig().getClaimConfiguration().getToolsConfiguration().getModificationTool())
        {
            GriefPrevention.sendMessage(player, TextMode.ERROR, Messages.MustHoldModificationToolForThat);
            return;
        }

        //must be standing in a land claim
        PlayerData playerData = getPlugin().getDataStore().getPlayerData(player.getUniqueId());
        Claim claim = getPlugin().getDataStore().getClaimAt(player.getLocation(), true, playerData.lastClaim);
        if (claim == null)
        {
            GriefPrevention.sendMessage(player, TextMode.ERROR, Messages.StandInClaimToResize);
            return;
        }

        //must have permission to edit the land claim you're in
        Supplier<String> errorMessage = claim.checkPermission(player, ClaimPermission.Edit, null);
        if (errorMessage != null)
        {
            GriefPrevention.sendMessage(player, TextMode.ERROR, Messages.NotYourClaim);
            return;
        }

        //determine new corner coordinates
        org.bukkit.util.Vector direction = player.getLocation().getDirection();
        if (direction.getY() > .75)
        {
            GriefPrevention.sendMessage(player, TextMode.INFO, Messages.ClaimsExtendToSky);
            return;
        }

        if (direction.getY() < -.75)
        {
            GriefPrevention.sendMessage(player, TextMode.INFO, Messages.ClaimsAutoExtendDownward);
            return;
        }

        Location lc = claim.getLesserBoundaryCorner();
        Location gc = claim.getGreaterBoundaryCorner();
        int newx1 = lc.getBlockX();
        int newx2 = gc.getBlockX();
        int newy1 = lc.getBlockY();
        int newy2 = gc.getBlockY();
        int newz1 = lc.getBlockZ();
        int newz2 = gc.getBlockZ();

        //if changing Z only
        if (Math.abs(direction.getX()) < .3)
        {
            if (direction.getZ() > 0)
            {
                newz2 += amount;  //north
            }
            else
            {
                newz1 -= amount;  //south
            }
        }

        //if changing X only
        else if (Math.abs(direction.getZ()) < .3)
        {
            if (direction.getX() > 0)
            {
                newx2 += amount;  //east
            }
            else
            {
                newx1 -= amount;  //west
            }
        }

        //diagonals
        else
        {
            if (direction.getX() > 0)
            {
                newx2 += amount;
            }
            else
            {
                newx1 -= amount;
            }

            if (direction.getZ() > 0)
            {
                newz2 += amount;
            }
            else
            {
                newz1 -= amount;
            }
        }

        //attempt resize
        playerData.claimResizing = claim;
        getPlugin().getDataStore().resizeClaimWithChecks(
                player,
                playerData,
                newx1,
                newx2,
                newy1,
                newy2,
                newz1,
                newz2
        );
        playerData.claimResizing = null;
    }
}
