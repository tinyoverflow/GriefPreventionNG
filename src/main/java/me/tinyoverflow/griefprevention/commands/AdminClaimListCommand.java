package me.tinyoverflow.griefprevention.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.executors.CommandArguments;
import dev.jorel.commandapi.executors.PlayerCommandExecutor;
import me.tinyoverflow.griefprevention.Claim;
import me.tinyoverflow.griefprevention.GriefPrevention;
import me.tinyoverflow.griefprevention.Messages;
import me.tinyoverflow.griefprevention.TextMode;
import org.bukkit.entity.Player;

import java.util.List;

public class AdminClaimListCommand extends BaseCommand implements PlayerCommandExecutor
{
    public AdminClaimListCommand(String commandName, GriefPrevention plugin)
    {
        super(commandName, plugin);
    }

    @Override
    public CommandAPICommand getCommand()
    {
        return new CommandAPICommand(this.getCommandName())
                .withPermission("griefprevention.adminclaimlist")
                .executesPlayer(this);
    }

    @Override
    public void run(Player player, CommandArguments commandArguments) throws WrapperCommandSyntaxException
    {
        List<Claim> claims = this.getPlugin()
                .getDataStore()
                .claims
                .stream()
                .filter(Claim::isAdminClaim)
                .toList();

        if (claims.size() > 0)
        {
            GriefPrevention.sendMessage(player, TextMode.Instr, Messages.ClaimsListHeader);
            for (Claim claim : claims)
            {
                GriefPrevention.sendMessage(player, TextMode.Instr, GriefPrevention.getFriendlyLocationString(claim.getLesserBoundaryCorner()));
            }
        }
    }
}
