package me.tinyoverflow.griefprevention.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.executors.CommandArguments;
import dev.jorel.commandapi.executors.PlayerCommandExecutor;
import me.tinyoverflow.griefprevention.*;
import me.tinyoverflow.griefprevention.datastore.DataStore;
import org.bukkit.entity.Player;

public class SubdivideClaimsCommand extends BaseCommand implements PlayerCommandExecutor
{
    public SubdivideClaimsCommand(String commandName, GriefPrevention plugin)
    {
        super(commandName, plugin);
    }

    @Override
    public CommandAPICommand getCommand()
    {
        return new CommandAPICommand(getCommandName())
                .withPermission("griefprevention.subdivideclaims")
                .executesPlayer(this);
    }

    @Override
    public void run(Player player, CommandArguments commandArguments) throws WrapperCommandSyntaxException
    {
        PlayerData playerData = getPlugin().getDataStore().getPlayerData(player.getUniqueId());
        playerData.toolMode = ToolMode.SUBDIVIDE;
        playerData.claimSubdividing = null;
        GriefPrevention.sendMessage(player, TextMode.INSTRUCTION, Messages.SubdivisionMode);
        GriefPrevention.sendMessage(
                player,
                TextMode.INSTRUCTION,
                Messages.SubdivisionVideo2,
                DataStore.SUBDIVISION_VIDEO_URL
        );
    }
}
