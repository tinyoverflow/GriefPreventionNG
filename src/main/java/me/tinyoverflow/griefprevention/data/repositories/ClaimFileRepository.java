package me.tinyoverflow.griefprevention.data.repositories;

import me.tinyoverflow.griefprevention.ClaimPermission;
import me.tinyoverflow.griefprevention.data.models.ClaimBoundaries;
import me.tinyoverflow.griefprevention.data.models.ClaimModel;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.UUID;

public class ClaimFileRepository implements ClaimRepository
{
    private final String PATH_OWNER = "owner";
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

    public ClaimFileRepository(ComponentLogger logger, File dataFolder)
    {
        this.logger = logger;
        this.dataFolder = dataFolder;
    }

    @Override
    public ClaimModel load(UUID id)
    {
        File file = getClaimFile(id);
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

        // Create Claim Model
        ClaimModel claim = new ClaimModel(id);
        claim.setOwner(owner);
        claim.setPermissions(permissions);
        claim.setBoundaries(boundaries);

        return claim;
    }

    @Override
    public boolean save(ClaimModel claim)
    {
        YamlConfiguration yaml = new YamlConfiguration();

        // Claim Owner and Permissions
        yaml.set(PATH_OWNER, claim.getOwner());
        yaml.set(PATH_PERMISSIONS, claim.getPermissions());

        // Claim Boundaries
        yaml.set(PATH_BOUNDARIES_WORLD, claim.getBoundaries().getWorld().getName());
        yaml.set(PATH_BOUNDARIES_LESSER_X, claim.getBoundaries().getLesser().getX());
        yaml.set(PATH_BOUNDARIES_LESSER_Y, claim.getBoundaries().getLesser().getY());
        yaml.set(PATH_BOUNDARIES_LESSER_Z, claim.getBoundaries().getLesser().getZ());
        yaml.set(PATH_BOUNDARIES_GREATER_X, claim.getBoundaries().getLesser().getX());
        yaml.set(PATH_BOUNDARIES_GREATER_Y, claim.getBoundaries().getLesser().getY());
        yaml.set(PATH_BOUNDARIES_GREATER_Z, claim.getBoundaries().getLesser().getZ());

        // Save File
        File file = getClaimFile(claim.getId());
        try {
            yaml.save(file);
            return true;
        }
        catch (IOException e) {
            logger.error("Cannot write file " + file.getPath() + ":\n" + e.getMessage());
            return false;
        }
    }

    public File getClaimFile(UUID uuid)
    {
        Path filePath = Paths.get(
                dataFolder.getPath(),
                "claims",
                uuid.toString() + ".yml"
        );

        return filePath.toFile();
    }
}
