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
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;

public class BuyClaimBlocksCommand extends BaseCommand implements PlayerCommandExecutor
{
    public BuyClaimBlocksCommand(String commandName, GriefPrevention plugin)
    {
        super(commandName, plugin);
    }

    @Override
    public CommandAPICommand getCommand()
    {
        return new CommandAPICommand(this.getCommandName())
                .withPermission("griefprevention.buyclaimblocks")
                .withOptionalArguments(new IntegerArgument("amount", 1))
                .executesPlayer(this);
    }

    @Override
    public void run(Player player, CommandArguments commandArguments) throws WrapperCommandSyntaxException
    {
        // if economy is disabled, don't do anything
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

        //if purchase disabled, send error message
        if (GriefPrevention.instance.config_economy_claimBlocksPurchaseCost == 0)
        {
            GriefPrevention.sendMessage(player, TextMode.Err, Messages.OnlySellBlocks);
            return;
        }

        Economy economy = economyWrapper.getEconomy();

        //if no parameter, just tell player cost per block and balance
        if (commandArguments.getOptional("amount").isEmpty())
        {
            GriefPrevention.sendMessage(player, TextMode.Info, Messages.BlockPurchaseCost, String.valueOf(GriefPrevention.instance.config_economy_claimBlocksPurchaseCost), String.valueOf(economy.getBalance(player)));
        }
        else
        {
            PlayerData playerData = this.getPlugin().getDataStore().getPlayerData(player.getUniqueId());
            int blockCount = (int) commandArguments.getOptional("amount").orElseThrow();

            //if the player can't afford his purchase, send error message
            double balance = economy.getBalance(player);
            double totalCost = blockCount * GriefPrevention.instance.config_economy_claimBlocksPurchaseCost;
            if (totalCost > balance)
            {
                GriefPrevention.sendMessage(player, TextMode.Err, Messages.InsufficientFunds, economy.format(totalCost), economy.format(balance));
            }

            //otherwise carry out transaction
            else
            {
                int newBonusClaimBlocks = playerData.getBonusClaimBlocks() + blockCount;

                //if the player is going to reach max bonus limit, send error message
                int bonusBlocksLimit = GriefPrevention.instance.config_economy_claimBlocksMaxBonus;
                if (bonusBlocksLimit != 0 && newBonusClaimBlocks > bonusBlocksLimit)
                {
                    GriefPrevention.sendMessage(player, TextMode.Err, Messages.MaxBonusReached, String.valueOf(blockCount), String.valueOf(bonusBlocksLimit));
                    return;
                }

                //withdraw cost
                economy.withdrawPlayer(player, totalCost);

                //add blocks
                playerData.setBonusClaimBlocks(playerData.getBonusClaimBlocks() + blockCount);
                this.getPlugin().getDataStore().savePlayerData(player.getUniqueId(), playerData);

                //inform player
                GriefPrevention.sendMessage(player, TextMode.Success, Messages.PurchaseConfirmation, economy.format(totalCost), String.valueOf(playerData.getRemainingClaimBlocks()));
            }
        }
    }
}
