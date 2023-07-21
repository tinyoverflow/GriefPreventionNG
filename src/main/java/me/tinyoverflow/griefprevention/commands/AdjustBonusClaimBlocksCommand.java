package me.tinyoverflow.griefprevention.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.OfflinePlayerArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.executors.CommandArguments;
import dev.jorel.commandapi.executors.PlayerCommandExecutor;
import me.tinyoverflow.griefprevention.*;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class AdjustBonusClaimBlocksCommand extends BaseCommand implements PlayerCommandExecutor
{
    public AdjustBonusClaimBlocksCommand(String commandName, GriefPrevention plugin)
    {
        super(commandName, plugin);
    }

    @Override
    public CommandAPICommand getCommand()
    {
        return new CommandAPICommand(this.getCommandName())
                .withPermission("griefprevention.adjustbonusclaimblocks")
                .withArguments(new OfflinePlayerArgument("target"))
                .withArguments(new IntegerArgument("limit", 0))
                .executesPlayer(this);
    }

    @Override
    public void run(Player player, CommandArguments commandArguments) throws WrapperCommandSyntaxException
    {
        Integer adjustment = (Integer) commandArguments.get("limit");
        OfflinePlayer targetPlayer = (OfflinePlayer) commandArguments.get("target");

        if (targetPlayer == null)
        {
            GriefPrevention.sendMessage(player, TextMode.Err, Messages.PlayerNotFound2);
            return;
        }

        //give blocks to player
        PlayerData playerData = this.getPlugin().getDataStore().getPlayerData(targetPlayer.getUniqueId());
        playerData.setBonusClaimBlocks(playerData.getBonusClaimBlocks() + adjustment);
        this.getPlugin().getDataStore().savePlayerData(targetPlayer.getUniqueId(), playerData);

        GriefPrevention.sendMessage(player, TextMode.Success, Messages.AdjustBlocksSuccess, targetPlayer.getName(), String.valueOf(adjustment), String.valueOf(playerData.getBonusClaimBlocks()));
        if (player != null)
            GriefPrevention.AddLogEntry(player.getName() + " adjusted " + targetPlayer.getName() + "'s bonus claim blocks by " + adjustment + ".", CustomLogEntryTypes.AdminActivity);
    }
}
