package me.tinyoverflow.griefprevention.commands.claim;

import com.griefprevention.visualization.BoundaryVisualization;
import com.griefprevention.visualization.VisualizationType;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.executors.CommandArguments;
import dev.jorel.commandapi.executors.PlayerCommandExecutor;
import me.tinyoverflow.griefprevention.*;
import me.tinyoverflow.griefprevention.configurations.ClaimConfiguration;
import me.tinyoverflow.griefprevention.configurations.GriefPreventionConfiguration;
import me.tinyoverflow.griefprevention.data.repositories.ClaimRepository;
import me.tinyoverflow.griefprevention.data.repositories.UserRepository;
import me.tinyoverflow.griefprevention.datastore.DataStore;
import me.tinyoverflow.griefprevention.tasks.AutoExtendClaimTask;
import me.tinyoverflow.tolker.Tolker;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class ClaimCreateCommand implements PlayerCommandExecutor
{
    private final GriefPrevention plugin;
    private final Tolker tolker;
    private final UserRepository userRepository;
    private final ClaimRepository claimRepository;
    private final ClaimConfiguration claimConfiguration;

    public ClaimCreateCommand(GriefPrevention plugin, Tolker tolker, UserRepository userRepository,
                              ClaimRepository claimRepository)
    {
        this.plugin = plugin;
        this.tolker = tolker;
        this.userRepository = userRepository;
        this.claimRepository = claimRepository;

        claimConfiguration = GriefPreventionConfiguration.getInstance().getClaimConfiguration();
    }

    @Override
    public void run(Player player, CommandArguments args) throws WrapperCommandSyntaxException
    {
        // Return early if the world is disabled.
        if (!claimConfiguration.isWorldEnabled(player.getWorld())) {
            tolker.from("errors.claim.world-disabled").send(player);
            return;
        }

        PlayerData playerData = plugin.getDataStore().getPlayerData(player);

        // Verify the player hasn't reached the maximum amount of claims.
        boolean hasClaimLimit = claimConfiguration.getCreationConfiguration().hasClaimLimit();
        int claimAmountLimit = claimConfiguration.getCreationConfiguration().getMaximumClaims();
        boolean playerReachedClaimLimit = playerData.getClaims().size() >= claimAmountLimit;
        boolean playerCanOverrideLimit = player.hasPermission(Permissions.Claim.OVERRIDE_LIMIT);

        if (hasClaimLimit && playerReachedClaimLimit && !playerCanOverrideLimit) {
            tolker.from("errors.claim.limit-reached")
                    .with("limit", claimAmountLimit)
                    .send(player);

            return;
        }

        //default is chest claim radius, unless -1
        int radius = plugin.getPluginConfig()
                           .getClaimConfiguration()
                           .getCreationConfiguration().automaticPreferredRadius;
        if (radius < 0) {
            radius = (int) Math.ceil(
                    Math.sqrt(plugin.getPluginConfig().getClaimConfiguration().getCreationConfiguration().minimumArea) /
                    2);
        }

        //if player has any claims, respect claim minimum size setting
        if (playerData.getClaims().size() > 0) {
            //if player has exactly one land claim, this requires the claim modification tool to be in hand (or creative mode player)
            if (playerData.getClaims().size() == 1 &&
                player.getGameMode() != GameMode.CREATIVE &&
                !player.getInventory()
                       .getItemInMainHand()
                       .getType()
                       .equals(plugin.getPluginConfig()
                                     .getClaimConfiguration()
                                     .getToolsConfiguration()
                                     .getModificationTool()))
            {
                GriefPrevention.sendMessage(player, TextMode.ERROR, Messages.MustHoldModificationToolForThat);
                return;
            }

            radius = (int) Math.ceil(
                    Math.sqrt(plugin.getPluginConfig().getClaimConfiguration().getCreationConfiguration().minimumArea) /
                    2);
        }

        //allow for specifying the radius
        if (args.getOptional("radius").isPresent()) {
            if (playerData.getClaims().size() < 2 && player.getGameMode() != GameMode.CREATIVE &&
                player.getInventory().getItemInMainHand().getType() !=
                plugin.getPluginConfig().getClaimConfiguration().getToolsConfiguration().getModificationTool())
            {
                GriefPrevention.sendMessage(player, TextMode.ERROR, Messages.RadiusRequiresGoldenShovel);
                return;
            }

            int specifiedRadius;
            try {
                specifiedRadius = (int) args.getOptional("radius").get();
            }
            catch (NumberFormatException e) {
                return;
            }

            if (specifiedRadius < radius) {
                GriefPrevention.sendMessage(player, TextMode.ERROR, Messages.MinimumRadius, String.valueOf(radius));
                return;
            }
            else {
                radius = specifiedRadius;
            }
        }

        if (radius < 0) radius = 0;

        Location lc = player.getLocation().add(-radius, 0, -radius);
        Location gc = player.getLocation().add(radius, 0, radius);

        //player must have sufficient unused claim blocks
        int area = Math.abs((gc.getBlockX() - lc.getBlockX() + 1) * (gc.getBlockZ() - lc.getBlockZ() + 1));
        int remaining = playerData.getRemainingClaimBlocks();
        if (remaining < area) {
            GriefPrevention.sendMessage(
                    player,
                    TextMode.ERROR,
                    Messages.CreateClaimInsufficientBlocks,
                    String.valueOf(area - remaining)
            );
            plugin.dataStore.tryAdvertiseAdminAlternatives(player);
            return;
        }

        CreateClaimResult result = plugin.getDataStore().createClaim(lc.getWorld(),
                lc.getBlockX(), gc.getBlockX(),
                lc.getBlockY() -
                plugin.getPluginConfig().getClaimConfiguration().getCreationConfiguration().extendIntoGroundDistance -
                1,
                gc.getWorld().getHighestBlockYAt(gc) -
                plugin.getPluginConfig().getClaimConfiguration().getCreationConfiguration().extendIntoGroundDistance -
                1,
                lc.getBlockZ(), gc.getBlockZ(),
                player.getUniqueId(), null, null, player
        );
        if (!result.succeeded || result.claim == null) {
            if (result.claim != null) {
                GriefPrevention.sendMessage(player, TextMode.ERROR, Messages.CreateClaimFailOverlapShort);

                BoundaryVisualization.visualizeClaim(
                        player,
                        result.claim,
                        com.griefprevention.visualization.VisualizationType.CONFLICT_ZONE
                );
            }
            else {
                GriefPrevention.sendMessage(player, TextMode.ERROR, Messages.CreateClaimFailOverlapRegion);
            }
        }
        else {
            GriefPrevention.sendMessage(player, TextMode.SUCCESS, Messages.CreateClaimSuccess);

            //link to a video demo of land claiming, based on world type
            if (plugin.creativeRulesApply(player.getLocation())) {
                GriefPrevention.sendMessage(
                        player,
                        TextMode.INSTRUCTION,
                        Messages.CreativeBasicsVideo2,
                        DataStore.CREATIVE_VIDEO_URL
                );
            }
            else if (plugin.claimsEnabledForWorld(player.getWorld())) {
                GriefPrevention.sendMessage(
                        player,
                        TextMode.INSTRUCTION,
                        Messages.SurvivalBasicsVideo2,
                        DataStore.SURVIVAL_VIDEO_URL
                );
            }
            BoundaryVisualization.visualizeClaim(player, result.claim, VisualizationType.CLAIM);
            playerData.claimResizing = null;
            playerData.lastShovelLocation = null;

            AutoExtendClaimTask.scheduleAsync(result.claim);
        }
    }
}
