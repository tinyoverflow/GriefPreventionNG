package me.tinyoverflow.griefprevention.commands;

import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.executors.CommandArguments;
import dev.jorel.commandapi.executors.PlayerCommandExecutor;
import me.tinyoverflow.griefprevention.GriefPrevention;
import me.tinyoverflow.griefprevention.Messages;
import me.tinyoverflow.griefprevention.PlayerData;
import me.tinyoverflow.griefprevention.TextMode;
import org.bukkit.entity.Player;

public class IgnoreClaimsCommand implements PlayerCommandExecutor
{
    private final GriefPrevention plugin;

    public IgnoreClaimsCommand(GriefPrevention plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public void run(Player player, CommandArguments arguments) throws WrapperCommandSyntaxException
    {
        PlayerData playerData = plugin.getDataStore().getPlayerData(player.getUniqueId());
        playerData.ignoreClaims = !playerData.ignoreClaims;

        GriefPrevention.sendMessage(
                player,
                TextMode.SUCCESS,
                playerData.ignoreClaims ? Messages.IgnoringClaims : Messages.RespectingClaims
        );
    }
}
