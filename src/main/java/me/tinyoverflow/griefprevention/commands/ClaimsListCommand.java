package me.tinyoverflow.griefprevention.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.OfflinePlayerArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.executors.CommandArguments;
import dev.jorel.commandapi.executors.PlayerCommandExecutor;
import me.tinyoverflow.griefprevention.*;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Vector;

public class ClaimsListCommand extends BaseCommand implements PlayerCommandExecutor
{
    public ClaimsListCommand(String commandName, GriefPrevention plugin)
    {
        super(commandName, plugin);
    }

    @Override
    public CommandAPICommand getCommand()
    {
        return new CommandAPICommand(getCommandName())
                .withPermission("griefprevention.claimslist")
                .withOptionalArguments(new OfflinePlayerArgument("target"))
                .executesPlayer(this);
    }

    @Override
    public void run(Player player, CommandArguments commandArguments) throws WrapperCommandSyntaxException
    {
        OfflinePlayer otherPlayer = (OfflinePlayer) commandArguments.getOptional("target").orElse(player);

        //otherwise if no permission to delve into another player's claims data
        if (!otherPlayer.getUniqueId().equals(player.getUniqueId()) &&
            !player.hasPermission("griefprevention.claimslistother"))
        {
            GriefPrevention.sendMessage(player, TextMode.ERROR, Messages.ClaimsListNoPermission);
            return;
        }

        //load the target player's data
        PlayerData playerData = getPlugin().getDataStore().getPlayerData(otherPlayer.getUniqueId());
        Vector<Claim> claims = playerData.getClaims();
        GriefPrevention.sendMessage(player, TextMode.INSTRUCTION, Messages.StartBlockMath,
                String.valueOf(playerData.getAccruedClaimBlocks()),
                String.valueOf((playerData.getBonusClaimBlocks() +
                                getPlugin().getDataStore().getGroupBonusBlocks(otherPlayer.getUniqueId()))),
                String.valueOf((playerData.getAccruedClaimBlocks() + playerData.getBonusClaimBlocks() +
                                getPlugin().getDataStore().getGroupBonusBlocks(otherPlayer.getUniqueId())))
        );

        if (claims.size() > 0)
        {
            GriefPrevention.sendMessage(player, TextMode.INSTRUCTION, Messages.ClaimsListHeader);
            for (int i = 0; i < playerData.getClaims().size(); i++)
            {
                Claim claim = playerData.getClaims().get(i);
                GriefPrevention.sendMessage(
                        player,
                        TextMode.INSTRUCTION,
                        GriefPrevention.getFriendlyLocationString(claim.getLesserBoundaryCorner()) +
                        getPlugin().getDataStore().getMessage(
                                Messages.ContinueBlockMath,
                                String.valueOf(claim.getArea())
                        )
                );
            }

            GriefPrevention.sendMessage(
                    player,
                    TextMode.INSTRUCTION,
                    Messages.EndBlockMath,
                    String.valueOf(playerData.getRemainingClaimBlocks())
            );
        }

        //drop the data we just loaded, if the player isn't online
        if (!otherPlayer.isOnline())
        {
            getPlugin().getDataStore().clearCachedPlayerData(otherPlayer.getUniqueId());
        }
    }
}
