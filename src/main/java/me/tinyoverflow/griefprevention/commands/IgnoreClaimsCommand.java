package me.tinyoverflow.griefprevention.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.executors.CommandArguments;
import dev.jorel.commandapi.executors.PlayerCommandExecutor;
import me.tinyoverflow.griefprevention.GriefPrevention;
import me.tinyoverflow.griefprevention.Messages;
import me.tinyoverflow.griefprevention.PlayerData;
import me.tinyoverflow.griefprevention.TextMode;
import org.bukkit.entity.Player;

public class IgnoreClaimsCommand implements BaseCommand, PlayerCommandExecutor
{
    private final GriefPrevention plugin;

    public IgnoreClaimsCommand(GriefPrevention plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public CommandAPICommand getCommand()
    {
        return new CommandAPICommand("ignoreclaims")
                .withPermission("griefprevention.ignoreclaims")
                .executesPlayer(this);
    }

    @Override
    public void run(Player player, CommandArguments arguments) throws WrapperCommandSyntaxException
    {
        PlayerData playerData = this.plugin.getDataStore().getPlayerData(player.getUniqueId());
        playerData.ignoreClaims = !playerData.ignoreClaims;

        GriefPrevention.sendMessage(
                player,
                TextMode.Success,
                playerData.ignoreClaims ? Messages.IgnoringClaims : Messages.RespectingClaims
        );
    }
}
