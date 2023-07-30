package me.tinyoverflow.griefprevention.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.OfflinePlayerArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.executors.CommandArguments;
import dev.jorel.commandapi.executors.PlayerCommandExecutor;
import me.tinyoverflow.griefprevention.GriefPrevention;
import me.tinyoverflow.griefprevention.Messages;
import me.tinyoverflow.griefprevention.PlayerData;
import me.tinyoverflow.griefprevention.TextMode;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class GivePetCommand extends BaseCommand implements PlayerCommandExecutor
{
    public GivePetCommand(String commandName, GriefPrevention plugin)
    {
        super(commandName, plugin);
    }

    @Override
    public CommandAPICommand getCommand()
    {
        return new CommandAPICommand(getCommandName())
                .withArguments(new OfflinePlayerArgument("target"))
                .withSubcommand(new CommandAPICommand("cancel").executesPlayer(this::cancel))
                .executesPlayer(this);
    }

    @Override
    public void run(Player player, CommandArguments arguments) throws WrapperCommandSyntaxException
    {
        PlayerData playerData = getPlugin().getDataStore().getPlayerData(player.getUniqueId());

        //find the specified player
        OfflinePlayer targetPlayer = (OfflinePlayer) arguments.get("target");
        if (targetPlayer == null
            || !targetPlayer.isOnline() && !targetPlayer.hasPlayedBefore()
            || targetPlayer.getName() == null)
        {
            GriefPrevention.sendMessage(player, TextMode.ERROR, Messages.PlayerNotFound2);
            return;
        }

        //remember the player's ID for later pet transfer
        playerData.petGiveawayRecipient = targetPlayer;

        //send instructions
        GriefPrevention.sendMessage(player, TextMode.INSTRUCTION, Messages.ReadyToTransferPet);
    }

    public void cancel(Player player, CommandArguments arguments)
    {
        PlayerData playerData = getPlugin().getDataStore().getPlayerData(player.getUniqueId());
        playerData.petGiveawayRecipient = null;
        GriefPrevention.sendMessage(player, TextMode.SUCCESS, Messages.PetTransferCancellation);
    }
}
