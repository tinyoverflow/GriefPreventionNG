package me.tinyoverflow.griefprevention.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.executors.CommandArguments;
import dev.jorel.commandapi.executors.PlayerCommandExecutor;
import me.tinyoverflow.griefprevention.GriefPrevention;
import me.tinyoverflow.griefprevention.Messages;
import me.tinyoverflow.griefprevention.PlayerData;
import me.tinyoverflow.griefprevention.TextMode;
import me.tinyoverflow.griefprevention.handlers.EconomyHandler;
import org.bukkit.entity.Player;

public class SellClaimBlocksCommand extends BaseCommand implements PlayerCommandExecutor
{
    public SellClaimBlocksCommand(String commandName, GriefPrevention plugin)
    {
        super(commandName, plugin);
    }

    @Override
    public CommandAPICommand getCommand()
    {
        return new CommandAPICommand(this.getCommandName())
                .withPermission("griefprevention.sellclaimblocks")
                .withOptionalArguments(new IntegerArgument("amount", 1))
                .executesPlayer(this);
    }

    @Override
    public void run(Player player, CommandArguments commandArguments) throws WrapperCommandSyntaxException
    {
//if economy is disabled, don't do anything
        EconomyHandler.EconomyWrapper economyWrapper = this.getPlugin().getEconomyHandler().getWrapper();
        if (economyWrapper == null)
        {
            GriefPrevention.sendMessage(player, TextMode.Err, Messages.BuySellNotConfigured);
            return;
        }

        if (!player.hasPermission("griefprevention.buysellclaimblocks"))
        {
            GriefPrevention.sendMessage(player, TextMode.Err, Messages.NoPermissionForCommand);
            return;
        }

        //if disabled, error message
        if (GriefPrevention.instance.config_economy_claimBlocksSellValue == 0)
        {
            GriefPrevention.sendMessage(player, TextMode.Err, Messages.OnlyPurchaseBlocks);
            return;
        }

        //load player data
        PlayerData playerData = this.getPlugin().getDataStore().getPlayerData(player.getUniqueId());
        int availableBlocks = playerData.getRemainingClaimBlocks();

        //if no amount provided, just tell player value per block sold, and how many he can sell
        if (commandArguments.getOptional("amount").isEmpty())
        {
            GriefPrevention.sendMessage(player, TextMode.Info, Messages.BlockSaleValue, String.valueOf(GriefPrevention.instance.config_economy_claimBlocksSellValue), String.valueOf(availableBlocks));
            return;
        }

        //parse number of blocks
        int blockCount = (int) commandArguments.getOptional("amount").orElseThrow();

        //if he doesn't have enough blocks, tell him so
        if (blockCount > availableBlocks)
        {
            GriefPrevention.sendMessage(player, TextMode.Err, Messages.NotEnoughBlocksForSale);
        }

        //otherwise carry out the transaction
        else
        {
            //compute value and deposit it
            double totalValue = blockCount * GriefPrevention.instance.config_economy_claimBlocksSellValue;
            economyWrapper.getEconomy().depositPlayer(player, totalValue);

            //subtract blocks
            playerData.setBonusClaimBlocks(playerData.getBonusClaimBlocks() - blockCount);
            this.getPlugin().getDataStore().savePlayerData(player.getUniqueId(), playerData);

            //inform player
            GriefPrevention.sendMessage(player, TextMode.Success, Messages.BlockSaleConfirmation, economyWrapper.getEconomy().format(totalValue), String.valueOf(playerData.getRemainingClaimBlocks()));
        }
    }
}
