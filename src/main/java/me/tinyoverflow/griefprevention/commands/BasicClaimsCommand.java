package me.tinyoverflow.griefprevention.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.executors.CommandArguments;
import dev.jorel.commandapi.executors.PlayerCommandExecutor;
import me.tinyoverflow.griefprevention.GriefPrevention;
import me.tinyoverflow.griefprevention.Messages;
import me.tinyoverflow.griefprevention.PlayerData;
import me.tinyoverflow.griefprevention.ShovelMode;
import me.tinyoverflow.griefprevention.TextMode;
import org.bukkit.entity.Player;

public class BasicClaimsCommand extends BaseCommand implements PlayerCommandExecutor
{
    public BasicClaimsCommand(String commandName, GriefPrevention plugin)
    {
        super(commandName, plugin);
    }

    @Override
    public CommandAPICommand getCommand()
    {
        return new CommandAPICommand(this.getCommandName())
                .withPermission("griefprevention.basicclaims")
                .executesPlayer(this);
    }

    @Override
    public void run(Player player, CommandArguments commandArguments) throws WrapperCommandSyntaxException
    {
        PlayerData playerData = this.getPlugin().getDataStore().getPlayerData(player.getUniqueId());
        playerData.shovelMode = ShovelMode.Basic;
        playerData.claimSubdividing = null;
        GriefPrevention.sendMessage(player, TextMode.Success, Messages.BasicClaimsMode);
    }
}
