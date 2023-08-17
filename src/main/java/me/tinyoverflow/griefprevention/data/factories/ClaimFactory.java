package me.tinyoverflow.griefprevention.data.factories;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.tinyoverflow.griefprevention.Permissions;
import me.tinyoverflow.griefprevention.configurations.ClaimConfiguration;
import me.tinyoverflow.griefprevention.data.RepositoryContainer;
import me.tinyoverflow.griefprevention.data.models.ClaimBoundaries;
import me.tinyoverflow.griefprevention.data.models.ClaimModel;
import me.tinyoverflow.griefprevention.data.models.UserModel;
import me.tinyoverflow.griefprevention.exceptions.ClaimLimitExceededException;
import me.tinyoverflow.griefprevention.exceptions.ClaimOverlapException;
import me.tinyoverflow.griefprevention.exceptions.InsufficientClaimBlocksException;
import me.tinyoverflow.griefprevention.exceptions.WorldDisabledException;
import org.apache.commons.lang.NotImplementedException;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;

import java.util.List;

@Setter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ClaimFactory
{
    private ClaimConfiguration configuration;
    private RepositoryContainer repositoryContainer;

    private OfflinePlayer player;
    private ClaimModel parentClaim;
    private ClaimBoundaries boundaries;

    private ClaimFactory(ClaimConfiguration configuration, RepositoryContainer repositoryContainer)
    {
        this.configuration = configuration;
        this.repositoryContainer = repositoryContainer;
    }

    public static ClaimFactory builder(ClaimConfiguration configuration, RepositoryContainer repositoryContainer)
    {
        return new ClaimFactory(configuration, repositoryContainer);
    }

    public ClaimFactory forPlayer(OfflinePlayer player)
    {
        this.player = player;
        return this;
    }

    public ClaimFactory childOf(ClaimModel parentClaim)
    {

        this.parentClaim = parentClaim;
        return this;
    }

    public ClaimFactory withDefaultRadius(Location location)
    {
        return withRadius(location, configuration.getCreationConfiguration().getAutomaticPreferredRadius());
    }

    public ClaimFactory withRadius(Location location, int radius)
    {
        boundaries = new ClaimBoundaries(
                location.getWorld(),
                location.clone().add(-radius, 0, -radius).toVector(),
                location.clone().add(radius, 0, radius).toVector()
        );

        return this;
    }

    public ClaimFactory withExplicit(Location first, Location second)
    {
        // TODO: Implement this
        throw new NotImplementedException("This feature hasn't been implemented yet.");
    }

    public ClaimModel build() throws WorldDisabledException, ClaimLimitExceededException, InsufficientClaimBlocksException, ClaimOverlapException
    {
        // Abort if claiming is disabled in the given world.
        if (configuration.isWorldEnabled(boundaries.getWorld()))
        {
            throw new WorldDisabledException(boundaries.getWorld());
        }

        // Check if the user would not exceed his claim limit
        UserModel userModel = repositoryContainer.getUserRepository().get(player);
        List<ClaimModel> userClaims = repositoryContainer.getClaimRepository().getByUser(userModel);
        if (configuration.getCreationConfiguration().hasClaimLimit() &&
            userClaims.size() >= configuration.getCreationConfiguration().getMaximumClaims() &&
            player.getPlayer() != null && !player.getPlayer().hasPermission(Permissions.Claim.OVERRIDE_LIMIT))
        {
            throw new ClaimLimitExceededException(
                    userModel,
                    configuration.getCreationConfiguration().getMaximumClaims()
            );
        }

        // Check that the user has enough claim blocks left
        int userClaimArea = userClaims.stream().mapToInt(claim -> claim.getBoundaries().area()).sum();
        int totalClaimBlocks = userModel.getAccruedBlocks() + userModel.getBonusBlocks();
        if (userClaimArea + boundaries.area() > totalClaimBlocks)
        {
            throw new InsufficientClaimBlocksException(userModel, boundaries.area(), totalClaimBlocks - userClaimArea);
        }

        // Check if the claim would overlap any existing claim
        List<ClaimModel> otherClaims = parentClaim != null
                ? repositoryContainer.getClaimRepository().childrenOf(parentClaim)
                : repositoryContainer.getClaimRepository().all();

        for (ClaimModel otherClaim : otherClaims)
        {
            if (boundaries.overlaps(otherClaim.getBoundaries()))
            {
                throw new ClaimOverlapException(otherClaim);
            }
        }

        // Finally create and return the new {@code ClaimModel}.
        ClaimModel claimModel = new ClaimModel();
        claimModel.setOwner(player);
        claimModel.setBoundaries(boundaries);
        claimModel.setParentClaim(parentClaim);

        return claimModel;
    }
}
