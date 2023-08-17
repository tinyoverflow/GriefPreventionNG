package me.tinyoverflow.griefprevention.data.repositories;

import me.tinyoverflow.griefprevention.ClaimPermission;
import me.tinyoverflow.griefprevention.data.models.ClaimBoundaries;
import me.tinyoverflow.griefprevention.data.models.ClaimModel;
import me.tinyoverflow.griefprevention.data.models.UserModel;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

public class ClaimFileRepository implements ClaimRepository
{
    private final String PATH_OWNER = "owner";
    private final String PATH_PARENT = "parent";
    private final String PATH_BOUNDARIES_WORLD = "boundaries.world";
    private final String PATH_BOUNDARIES_LESSER_X = "boundaries.lesser.x";
    private final String PATH_BOUNDARIES_LESSER_Y = "boundaries.lesser.y";
    private final String PATH_BOUNDARIES_LESSER_Z = "boundaries.lesser.z";
    private final String PATH_BOUNDARIES_GREATER_X = "boundaries.greater.x";
    private final String PATH_BOUNDARIES_GREATER_Y = "boundaries.greater.y";
    private final String PATH_BOUNDARIES_GREATER_Z = "boundaries.greater.z";
    private final String PATH_PERMISSIONS = "permissions";

    private final ComponentLogger logger;
    private final File dataFolder;

    private final Map<UUID, ClaimModel> claims = new HashMap<>();

    public ClaimFileRepository(ComponentLogger logger, File dataFolder)
    {
        this.logger = logger;
        this.dataFolder = Paths.get(dataFolder.getPath(), "claims").toFile();
    }

    @Override
    public void load()
    {
        File[] files = dataFolder.listFiles((dir, name) -> name.endsWith(".yml"));

        if (files != null)
        {
            loadFromFiles(files);
        }
    }

    /**
     * Loads all claims from the files given.
     *
     * @param files Array of File to load from.
     */
    private void loadFromFiles(@NotNull File[] files)
    {
        Map<ClaimModel, UUID> children = new HashMap<>();

        for (File file : files)
        {
            // Get UUID from filename
            String fileName = file.getName().substring(0, file.getName().lastIndexOf(".yml"));
            UUID uuid = UUID.fromString(fileName);
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

            // Extract Data from File
            OfflinePlayer owner = yaml.getOfflinePlayer(PATH_OWNER);
            HashMap<OfflinePlayer, ClaimPermission> permissions = new HashMap<>();
            ClaimBoundaries boundaries = new ClaimBoundaries(
                    Bukkit.getWorld(yaml.getString(PATH_BOUNDARIES_WORLD, "")),
                    new Vector(
                            yaml.getDouble(PATH_BOUNDARIES_LESSER_X),
                            yaml.getDouble(PATH_BOUNDARIES_LESSER_Y),
                            yaml.getDouble(PATH_BOUNDARIES_LESSER_Z)
                    ),
                    new Vector(
                            yaml.getDouble(PATH_BOUNDARIES_GREATER_X),
                            yaml.getDouble(PATH_BOUNDARIES_GREATER_Y),
                            yaml.getDouble(PATH_BOUNDARIES_GREATER_Z)
                    )
            );

            // TODO: Load Permissions

            // Create Claim Model
            ClaimModel claim = new ClaimModel(uuid);
            claim.setOwner(owner);
            claim.setPermissions(permissions);
            claim.setBoundaries(boundaries);

            // Store claim in memory
            claims.put(claim.getId(), claim);

            // If this claim has a parent, store it for later reference
            String parentClaimId = yaml.getString(PATH_PARENT);
            if (parentClaimId != null)
            {
                children.put(claim, UUID.fromString(parentClaimId));
            }
        }

        // Assign Parent Relationships
        for (Map.Entry<ClaimModel, UUID> child : children.entrySet())
        {
            ClaimModel claim = child.getKey();
            UUID parentId = child.getValue();

            // Ignore this entry if the parent does not exist.
            if (!children.containsValue(parentId))
            {
                logger.error("Missing parent claim " + parentId + " for child " + claim.getId() + ". Skipping.");
                claims.remove(claim.getId());
                continue;
            }

            claim.setParentClaim(claims.get(parentId));
        }
    }

    @Override
    public boolean save(ClaimModel claim)
    {
        if (!claims.containsKey(claim.getId()))
        {
            claims.put(claim.getId(), claim);
        }

        File file = new File(dataFolder, claim.getId() + ".yml");

        try
        {
            YamlConfiguration yaml = new YamlConfiguration();

            // Claim Owner and Permissions
            yaml.set(PATH_OWNER, claim.getOwner());
            yaml.set(PATH_PERMISSIONS, claim.getPermissions());

            // Parent Claim, if existent
            if (claim.getParentClaim() != null)
            {
                yaml.set(PATH_PARENT, claim.getParentClaim().getId());
            }

            // Claim Boundaries
            yaml.set(PATH_BOUNDARIES_WORLD, claim.getBoundaries().getWorld().getName());
            yaml.set(PATH_BOUNDARIES_LESSER_X, claim.getBoundaries().getLesser().getX());
            yaml.set(PATH_BOUNDARIES_LESSER_Y, claim.getBoundaries().getLesser().getY());
            yaml.set(PATH_BOUNDARIES_LESSER_Z, claim.getBoundaries().getLesser().getZ());
            yaml.set(PATH_BOUNDARIES_GREATER_X, claim.getBoundaries().getLesser().getX());
            yaml.set(PATH_BOUNDARIES_GREATER_Y, claim.getBoundaries().getLesser().getY());
            yaml.set(PATH_BOUNDARIES_GREATER_Z, claim.getBoundaries().getLesser().getZ());

            // Save File
            yaml.save(file);

            return true;
        }
        catch (IOException e)
        {
            logger.error("Cannot write file " + file.getPath() + ":\n" + e.getMessage());
            return false;
        }
    }

    @Override
    public void save()
    {
        for (ClaimModel claim : claims.values())
        {
            save(claim);
        }
    }

    @Override
    public Optional<ClaimModel> get(UUID key)
    {
        if (claims.containsKey(key))
        {
            return Optional.of(claims.get(key));
        }

        return Optional.empty();
    }

    @Override
    public List<ClaimModel> getByUser(UserModel userModel)
    {
        return claims
                .values()
                .stream()
                .filter(claimModel -> claimModel
                        .getOwner()
                        .getUniqueId()
                        .equals(userModel.getId()))
                .toList();
    }

    @Override
    public List<ClaimModel> all()
    {
        return claims
                .values()
                .stream()
                .toList();
    }

    @Override
    public List<ClaimModel> childrenOf(ClaimModel parentClaim)
    {
        return claims
                .values()
                .stream()
                .filter(claimModel -> claimModel
                        .getParentClaim()
                        .getId()
                        .equals(parentClaim.getId()))
                .toList();
    }
}
