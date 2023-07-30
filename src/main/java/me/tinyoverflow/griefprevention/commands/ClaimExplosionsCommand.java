package me.tinyoverflow.griefprevention.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.executors.CommandArguments;
import dev.jorel.commandapi.executors.PlayerCommandExecutor;
import me.tinyoverflow.griefprevention.*;
import org.bukkit.entity.Player;

import java.util.function.Supplier;

public class ClaimExplosionsCommand extends BaseCommand implements PlayerCommandExecutor
{
    public ClaimExplosionsCommand(String commandName, GriefPrevention plugin)
    {
        super(commandName, plugin);
    }

    @Override
    public CommandAPICommand getCommand()
    {
        return new CommandAPICommand(getCommandName())
                .withPermission("griefprevention.claimexplosions")
                .executesPlayer(this);
    }

    @Override
    public void run(Player player, CommandArguments commandArguments) throws WrapperCommandSyntaxException
    {
        Claim claim = getPlugin().getDataStore().getClaimAt(player.getLocation(), true /*ignore height*/, null);

        if (claim == null)
        {
            GriefPrevention.sendMessage(player, TextMode.ERROR, Messages.DeleteClaimMissing);
        }
        else
        {
            Supplier<String> noBuildReason = claim.checkPermission(player, ClaimPermission.Build, null);
            if (noBuildReason != null)
            {
                GriefPrevention.sendMessage(player, TextMode.ERROR, noBuildReason.get());
                return;
            }

            if (claim.areExplosivesAllowed)
            {
                claim.areExplosivesAllowed = false;
                GriefPrevention.sendMessage(player, TextMode.SUCCESS, Messages.ExplosivesDisabled);
            }
            else
            {
                claim.areExplosivesAllowed = true;
                GriefPrevention.sendMessage(player, TextMode.SUCCESS, Messages.ExplosivesEnabled);
            }
        }
    }
}
