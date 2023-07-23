package me.tinyoverflow.griefprevention.commands;

import com.griefprevention.visualization.BoundaryVisualization;
import com.griefprevention.visualization.VisualizationType;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.executors.CommandArguments;
import dev.jorel.commandapi.executors.PlayerCommandExecutor;
import me.tinyoverflow.griefprevention.CreateClaimResult;
import me.tinyoverflow.griefprevention.GriefPrevention;
import me.tinyoverflow.griefprevention.Messages;
import me.tinyoverflow.griefprevention.PlayerData;
import me.tinyoverflow.griefprevention.TextMode;
import me.tinyoverflow.griefprevention.datastore.DataStore;
import me.tinyoverflow.griefprevention.tasks.AutoExtendClaimTask;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class ClaimCommand extends BaseCommand implements PlayerCommandExecutor
{
    public ClaimCommand(String commandName, GriefPrevention plugin)
    {
        super(commandName, plugin);
    }

    @Override
    public CommandAPICommand getCommand()
    {
        return new CommandAPICommand(getCommandName())
                .withPermission("griefprevention.claim")
                .withOptionalArguments(new IntegerArgument("radius", getPlugin().getPluginConfig().getClaimConfiguration().getCreation().automaticMinimumRadius))
                .executesPlayer(this);
    }

    @Override
    public void run(Player player, CommandArguments args) throws WrapperCommandSyntaxException
    {
        if (!getPlugin().claimsEnabledForWorld(player.getWorld()))
        {
            GriefPrevention.sendMessage(player, TextMode.Err, Messages.ClaimsDisabledWorld);
            return;
        }

        PlayerData playerData = getPlugin().getDataStore().getPlayerData(player.getUniqueId());

        //if he's at the claim count per player limit already and doesn't have permission to bypass, display an error message
        if (getPlugin().getPluginConfig().getClaimConfiguration().getCreation().maximumClaims > 0 &&
                !player.hasPermission("griefprevention.overrideclaimcountlimit") &&
                playerData.getClaims().size() >= getPlugin().getPluginConfig().getClaimConfiguration().getCreation().maximumClaims)
        {
            GriefPrevention.sendMessage(player, TextMode.Err, Messages.ClaimCreationFailedOverClaimCountLimit);
            return;
        }

        //default is chest claim radius, unless -1
        int radius = getPlugin().getPluginConfig().getClaimConfiguration().getCreation().automaticPreferredRadius;
        if (radius < 0) radius = (int) Math.ceil(Math.sqrt(getPlugin().getPluginConfig().getClaimConfiguration().getCreation().minimumArea) / 2);

        //if player has any claims, respect claim minimum size setting
        if (playerData.getClaims().size() > 0)
        {
            //if player has exactly one land claim, this requires the claim modification tool to be in hand (or creative mode player)
            if (playerData.getClaims().size() == 1 &&
                    player.getGameMode() != GameMode.CREATIVE &&
                    !player.getInventory().getItemInMainHand().getType().equals(getPlugin().getPluginConfig().getClaimConfiguration().getTools().getModificationTool()))
            {
                GriefPrevention.sendMessage(player, TextMode.Err, Messages.MustHoldModificationToolForThat);
                return;
            }

            radius = (int) Math.ceil(Math.sqrt(getPlugin().getPluginConfig().getClaimConfiguration().getCreation().minimumArea) / 2);
        }

        //allow for specifying the radius
        if (args.getOptional("radius").isPresent())
        {
            if (playerData.getClaims().size() < 2 && player.getGameMode() != GameMode.CREATIVE && player.getInventory().getItemInMainHand().getType() != getPlugin().getPluginConfig().getClaimConfiguration().getTools().getModificationTool())
            {
                GriefPrevention.sendMessage(player, TextMode.Err, Messages.RadiusRequiresGoldenShovel);
                return;
            }

            int specifiedRadius;
            try
            {
                specifiedRadius = (int) args.getOptional("radius").get();
            }
            catch (NumberFormatException e)
            {
                return;
            }

            if (specifiedRadius < radius)
            {
                GriefPrevention.sendMessage(player, TextMode.Err, Messages.MinimumRadius, String.valueOf(radius));
                return;
            }
            else
            {
                radius = specifiedRadius;
            }
        }

        if (radius < 0) radius = 0;

        Location lc = player.getLocation().add(-radius, 0, -radius);
        Location gc = player.getLocation().add(radius, 0, radius);

        //player must have sufficient unused claim blocks
        int area = Math.abs((gc.getBlockX() - lc.getBlockX() + 1) * (gc.getBlockZ() - lc.getBlockZ() + 1));
        int remaining = playerData.getRemainingClaimBlocks();
        if (remaining < area)
        {
            GriefPrevention.sendMessage(player, TextMode.Err, Messages.CreateClaimInsufficientBlocks, String.valueOf(area - remaining));
            getPlugin().dataStore.tryAdvertiseAdminAlternatives(player);
            return;
        }

        CreateClaimResult result = getPlugin().getDataStore().createClaim(lc.getWorld(),
                lc.getBlockX(), gc.getBlockX(),
                lc.getBlockY() - getPlugin().getPluginConfig().getClaimConfiguration().getCreation().extendIntoGroundDistance - 1,
                gc.getWorld().getHighestBlockYAt(gc) - getPlugin().getPluginConfig().getClaimConfiguration().getCreation().extendIntoGroundDistance - 1,
                lc.getBlockZ(), gc.getBlockZ(),
                player.getUniqueId(), null, null, player);
        if (!result.succeeded || result.claim == null)
        {
            if (result.claim != null)
            {
                GriefPrevention.sendMessage(player, TextMode.Err, Messages.CreateClaimFailOverlapShort);

                BoundaryVisualization.visualizeClaim(player, result.claim, VisualizationType.CONFLICT_ZONE);
            }
            else
            {
                GriefPrevention.sendMessage(player, TextMode.Err, Messages.CreateClaimFailOverlapRegion);
            }
        }
        else
        {
            GriefPrevention.sendMessage(player, TextMode.Success, Messages.CreateClaimSuccess);

            //link to a video demo of land claiming, based on world type
            if (getPlugin().creativeRulesApply(player.getLocation()))
            {
                GriefPrevention.sendMessage(player, TextMode.Instr, Messages.CreativeBasicsVideo2, DataStore.CREATIVE_VIDEO_URL);
            }
            else if (getPlugin().claimsEnabledForWorld(player.getWorld()))
            {
                GriefPrevention.sendMessage(player, TextMode.Instr, Messages.SurvivalBasicsVideo2, DataStore.SURVIVAL_VIDEO_URL);
            }
            BoundaryVisualization.visualizeClaim(player, result.claim, VisualizationType.CLAIM);
            playerData.claimResizing = null;
            playerData.lastShovelLocation = null;

            AutoExtendClaimTask.scheduleAsync(result.claim);
        }
    }
}
