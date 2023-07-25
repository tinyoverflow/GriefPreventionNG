package me.tinyoverflow.griefprevention.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.executors.CommandArguments;
import dev.jorel.commandapi.executors.PlayerCommandExecutor;
import me.tinyoverflow.griefprevention.*;
import me.tinyoverflow.griefprevention.events.SaveTrappedPlayerEvent;
import me.tinyoverflow.griefprevention.tasks.PlayerRescueTask;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class TrappedCommand extends BaseCommand implements PlayerCommandExecutor
{
    public TrappedCommand(String commandName, GriefPrevention plugin)
    {
        super(commandName, plugin);
    }

    @Override
    public CommandAPICommand getCommand()
    {
        return new CommandAPICommand(this.getCommandName())
                .withPermission("griefprevention.trapped")
                .executesPlayer(this);
    }

    @Override
    public void run(Player player, CommandArguments commandArguments) throws WrapperCommandSyntaxException
    {
        PlayerData playerData = this.getPlugin().getDataStore().getPlayerData(player.getUniqueId());
        Claim claim = this.getPlugin().getDataStore().getClaimAt(player.getLocation(), false, playerData.lastClaim);

        //if another /trapped is pending, ignore this slash command
        if (playerData.pendingTrapped)
        {
            return;
        }

        //if the player isn't in a claim or has permission to build, tell him to man up
        if (claim == null || claim.checkPermission(player, ClaimPermission.Build, null) == null)
        {
            GriefPrevention.sendMessage(player, TextMode.Err, Messages.NotTrappedHere);
            return;
        }

        //rescue destination may be set by GPFlags or other plugin, ask to find out
        SaveTrappedPlayerEvent event = new SaveTrappedPlayerEvent(claim);
        Bukkit.getPluginManager().callEvent(event);

        //if the player is in the nether or end, he's screwed (there's no way to programmatically find a safe place for him)
        if (player.getWorld().getEnvironment() != World.Environment.NORMAL && event.getDestination() == null)
        {
            GriefPrevention.sendMessage(player, TextMode.Err, Messages.TrappedWontWorkHere);
            return;
        }

        //if the player is in an administrative claim and AllowTrappedInAdminClaims is false, he should contact an admin
        if (!GriefPrevention.instance.getPluginConfig().getClaimConfiguration().isAllowTrappedInAdminClaims() && claim.isAdminClaim() && event.getDestination() == null)
        {
            GriefPrevention.sendMessage(player, TextMode.Err, Messages.TrappedWontWorkHere);
            return;
        }
        //send instructions
        GriefPrevention.sendMessage(player, TextMode.Instr, Messages.RescuePending);

        //create a task to rescue this player in a little while
        PlayerRescueTask task = new PlayerRescueTask(player, player.getLocation(), event.getDestination());
        this.getPlugin().getServer().getScheduler().scheduleSyncDelayedTask(this.getPlugin(), task, 200L);  //20L ~ 1 second
    }
}
