package me.tinyoverflow.griefprevention.commands;

import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.executors.CommandArguments;
import dev.jorel.commandapi.executors.PlayerCommandExecutor;
import me.tinyoverflow.griefprevention.Claim;
import me.tinyoverflow.griefprevention.GriefPrevention;
import me.tinyoverflow.griefprevention.Messages;
import me.tinyoverflow.griefprevention.TextMode;
import org.bukkit.entity.Player;

import java.util.List;

public class AdminClaimListCommand implements PlayerCommandExecutor
{
    private final GriefPrevention plugin;

    public AdminClaimListCommand(GriefPrevention plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public void run(Player player, CommandArguments commandArguments) throws WrapperCommandSyntaxException
    {
        List<Claim> claims = plugin
                .getDataStore()
                .claims
                .stream()
                .filter(Claim::isAdminClaim)
                .toList();

        if (!claims.isEmpty()) {
            GriefPrevention.sendMessage(player, TextMode.INSTRUCTION, Messages.ClaimsListHeader);
            for (Claim claim : claims) {
                GriefPrevention.sendMessage(
                        player,
                        TextMode.INSTRUCTION,
                        GriefPrevention.getFriendlyLocationString(claim.getLesserBoundaryCorner())
                );
            }
        }
    }
}
