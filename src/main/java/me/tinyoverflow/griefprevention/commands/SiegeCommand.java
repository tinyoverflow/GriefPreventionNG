package me.tinyoverflow.griefprevention.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.PlayerArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.executors.CommandArguments;
import dev.jorel.commandapi.executors.PlayerCommandExecutor;
import me.tinyoverflow.griefprevention.*;
import org.bukkit.entity.Player;

import java.util.Optional;

public class SiegeCommand extends BaseCommand implements PlayerCommandExecutor
{
    public SiegeCommand(String commandName, GriefPrevention plugin)
    {
        super(commandName, plugin);
    }

    @Override
    public CommandAPICommand getCommand()
    {
        return new CommandAPICommand("siege")
                .withOptionalArguments(new PlayerArgument("target"))
                .executesPlayer(this);
    }

    @Override
    public void run(Player player, CommandArguments commandArguments) throws WrapperCommandSyntaxException
    {
//error message for when siege mode is disabled
        if (!getPlugin().getPluginConfig().getSiegeConfiguration().isEnabledForWorld(player.getWorld()))
        {
            GriefPrevention.sendMessage(player, TextMode.ERROR, Messages.NonSiegeWorld);
            return;
        }

        //can't start a siege when you're already involved in one
        PlayerData attackerData = getPlugin().getDataStore().getPlayerData(player.getUniqueId());
        if (attackerData.siegeData != null)
        {
            GriefPrevention.sendMessage(player, TextMode.ERROR, Messages.AlreadySieging);
            return;
        }

        //can't start a siege when you're protected from pvp combat
        if (attackerData.pvpImmune)
        {
            GriefPrevention.sendMessage(player, TextMode.ERROR, Messages.CantFightWhileImmune);
            return;
        }

        //if a player name was specified, use that
        Optional<Object> defenderOptional = commandArguments.getOptional("player").or(
                () -> Optional.ofNullable(getPlugin().getServer().getPlayer(attackerData.lastPvpPlayer))
        );

        if (defenderOptional.isEmpty())
        {
            return;
        }

        Player defender = (Player) defenderOptional.orElseThrow();

        // First off, you cannot siege yourself, that's just
        // silly:
        if (player.getUniqueId().equals(defender.getUniqueId()))
        {
            GriefPrevention.sendMessage(player, TextMode.ERROR, Messages.NoSiegeYourself);
            return;
        }

        //victim must not have the permission which makes him immune to siege
        if (defender.hasPermission("griefprevention.siegeimmune"))
        {
            GriefPrevention.sendMessage(player, TextMode.ERROR, Messages.SiegeImmune);
            return;
        }

        //victim must not be under siege already
        PlayerData defenderData = getPlugin().getDataStore().getPlayerData(defender.getUniqueId());
        if (defenderData.siegeData != null)
        {
            GriefPrevention.sendMessage(player, TextMode.ERROR, Messages.AlreadyUnderSiegePlayer);
            return;
        }

        //victim must not be pvp immune
        if (defenderData.pvpImmune)
        {
            GriefPrevention.sendMessage(player, TextMode.ERROR, Messages.NoSiegeDefenseless);
            return;
        }

        Claim defenderClaim = getPlugin().getDataStore().getClaimAt(defender.getLocation(), false, null);

        //defender must have some level of permission there to be protected
        if (defenderClaim == null || defenderClaim.checkPermission(defender, ClaimPermission.Access, null) != null)
        {
            GriefPrevention.sendMessage(player, TextMode.ERROR, Messages.NotSiegableThere);
            return;
        }

        //attacker must be close to the claim he wants to siege
        if (!defenderClaim.isNear(player.getLocation(), 25))
        {
            GriefPrevention.sendMessage(player, TextMode.ERROR, Messages.SiegeTooFarAway);
            return;
        }

        //claim can't be under siege already
        if (defenderClaim.siegeData != null)
        {
            GriefPrevention.sendMessage(player, TextMode.ERROR, Messages.AlreadyUnderSiegeArea);
            return;
        }

        //can't siege admin claims
        if (defenderClaim.isAdminClaim())
        {
            GriefPrevention.sendMessage(player, TextMode.ERROR, Messages.NoSiegeAdminClaim);
            return;
        }

        //can't be on cooldown
        if (getPlugin().getDataStore().onCooldown(player, defender, defenderClaim))
        {
            GriefPrevention.sendMessage(player, TextMode.ERROR, Messages.SiegeOnCooldown);
            return;
        }

        //start the siege
        getPlugin().getDataStore().startSiege(player, defender, defenderClaim);

        //confirmation message for attacker, warning message for defender
        GriefPrevention.sendMessage(defender, TextMode.WARNING, Messages.SiegeAlert, player.getName());
        GriefPrevention.sendMessage(player, TextMode.SUCCESS, Messages.SiegeConfirmed, defender.getName());
    }
}
