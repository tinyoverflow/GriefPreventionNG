package me.tinyoverflow.griefprevention.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.executors.CommandArguments;
import dev.jorel.commandapi.executors.PlayerCommandExecutor;
import me.tinyoverflow.griefprevention.Claim;
import me.tinyoverflow.griefprevention.GriefPrevention;
import me.tinyoverflow.griefprevention.Messages;
import me.tinyoverflow.griefprevention.PlayerData;
import me.tinyoverflow.griefprevention.TextMode;
import org.bukkit.entity.Player;

public class AbandonAllClaimsCommand extends BaseCommand implements PlayerCommandExecutor
{
    public AbandonAllClaimsCommand(String commandName, GriefPrevention plugin)
    {
        super(commandName, plugin);
    }

    @Override
    public CommandAPICommand getCommand()
    {
        return new CommandAPICommand(getCommandName())
                .withPermission("griefprevention.abandonallclaims")
                .withOptionalArguments(new StringArgument("confirm").replaceSuggestions(ArgumentSuggestions.strings("confirm")))
                .executesPlayer(this);
    }

    @Override
    public void run(Player player, CommandArguments commandArguments) throws WrapperCommandSyntaxException
    {
        if (commandArguments.getOptional("confirm").isEmpty())
        {
            GriefPrevention.sendMessage(player, TextMode.Err, Messages.ConfirmAbandonAllClaims);
            return;
        }

        //count claims
        PlayerData playerData = getPlugin().getDataStore().getPlayerData(player.getUniqueId());
        int originalClaimCount = playerData.getClaims().size();

        //check count
        if (originalClaimCount == 0)
        {
            GriefPrevention.sendMessage(player, TextMode.Err, Messages.YouHaveNoClaims);
            return;
        }

        double abandonReturnRatio = getPlugin().getPluginConfig().getClaimConfiguration().getClaimBlocks().abandonReturnRatio;
        if (abandonReturnRatio != 1.0D)
        {
            //adjust claim blocks
            for (Claim claim : playerData.getClaims())
            {
                playerData.setAccruedClaimBlocks(playerData.getAccruedClaimBlocks() - (int) Math.ceil((claim.getArea() * (1 - abandonReturnRatio))));
            }
        }


        //delete them
        getPlugin().getDataStore().deleteClaimsForPlayer(player.getUniqueId(), false);

        //inform the player
        int remainingBlocks = playerData.getRemainingClaimBlocks();
        GriefPrevention.sendMessage(player, TextMode.Success, Messages.SuccessfulAbandon, String.valueOf(remainingBlocks));

        //revert any current visualization
        playerData.setVisibleBoundaries(null);
    }
}
