package me.tinyoverflow.griefprevention.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.OfflinePlayerArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.executors.CommandArguments;
import dev.jorel.commandapi.executors.PlayerCommandExecutor;
import me.tinyoverflow.griefprevention.Claim;
import me.tinyoverflow.griefprevention.GriefPrevention;
import me.tinyoverflow.griefprevention.Messages;
import me.tinyoverflow.griefprevention.TextMode;
import me.tinyoverflow.griefprevention.datastore.DataStore;
import me.tinyoverflow.griefprevention.logger.LogType;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class TransferClaimCommand extends BaseCommand implements PlayerCommandExecutor
{
    public TransferClaimCommand(String commandName, GriefPrevention plugin)
    {
        super(commandName, plugin);
    }

    @Override
    public CommandAPICommand getCommand()
    {
        return new CommandAPICommand(getCommandName())
                .withPermission("griefprevention.transferclaim")
                .withArguments(new OfflinePlayerArgument("target"))
                .executesPlayer(this);
    }

    @Override
    public void run(Player player, CommandArguments arguments) throws WrapperCommandSyntaxException
    {
        //which claim is the user in?
        Claim claim = getPlugin().getDataStore().getClaimAt(player.getLocation(), true, null);
        if (claim == null)
        {
            GriefPrevention.sendMessage(player, TextMode.Instr, Messages.TransferClaimMissing);
            return;
        }

        //check additional permission for admin claims
        if (claim.isAdminClaim() && !player.hasPermission("griefprevention.adminclaims"))
        {
            GriefPrevention.sendMessage(player, TextMode.Err, Messages.TransferClaimPermission);
            return;
        }

        OfflinePlayer target = (OfflinePlayer) arguments.get("target");
        try
        {
            getPlugin().getDataStore().changeClaimOwner(claim, target.getUniqueId());
        }
        catch (DataStore.NoTransferException e)
        {
            GriefPrevention.sendMessage(player, TextMode.Instr, Messages.TransferTopLevel);
            return;
        }

        //confirm
        GriefPrevention.sendMessage(player, TextMode.Success, Messages.TransferSuccess);
        GriefPrevention.AddLogEntry(player.getName() + " transferred a claim at " + GriefPrevention.getFriendlyLocationString(claim.getLesserBoundaryCorner()) + " to " + target.getName() + ".", LogType.ADMIN);
    }
}
