package me.tinyoverflow.griefprevention.data.repositories;

import me.tinyoverflow.griefprevention.data.factories.UserFactory;
import me.tinyoverflow.griefprevention.data.models.UserModel;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class UserFileRepository implements UserRepository
{
    private static final String PATH_ACCRUED_BLOCKS = "accrued.blocks";
    private static final String PATH_ACCRUED_LIMIT = "accrued.limit";
    private static final String PATH_BONUS_BLOCKS = "bonus.blocks";

    private final Map<UUID, UserModel> playerDataCache = new HashMap<>();
    private final ComponentLogger logger;
    private final File dataFolder;

    public UserFileRepository(ComponentLogger logger, File dataFolder)
    {
        this.logger = logger;
        this.dataFolder = dataFolder;
    }

    @Override
    public UserModel load(UUID uuid)
    {
        // Return PlayerData object from cache, if present.
        if (playerDataCache.containsKey(uuid)) {
            return playerDataCache.get(uuid);
        }

        //
        File file = getPlayerFile(uuid);
        UserModel userModel = new UserModel(uuid);

        if (!file.exists()) {
            // Create new PlayerData object if the file doesn't exist.
            userModel = UserFactory.builder(uuid).applyDefaults().build();
        }
        else {
            // Otherwise hydrate the PlayerData object with the saved data.
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            userModel.setAccruedBlocks(yaml.getInt(PATH_ACCRUED_BLOCKS));
            userModel.setAccruedBlockLimit(yaml.getInt(PATH_ACCRUED_LIMIT));
            userModel.setBonusBlocks(yaml.getInt(PATH_ACCRUED_BLOCKS));
        }

        playerDataCache.put(uuid, userModel);
        return userModel;
    }

    public UserModel load(OfflinePlayer player)
    {
        return null;
    }

    @Override
    public boolean save(@NotNull UserModel userModel)
    {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set(PATH_ACCRUED_BLOCKS, userModel.getAccruedBlocks());
        yaml.set(PATH_ACCRUED_LIMIT, userModel.getAccruedBlockLimit());
        yaml.set(PATH_BONUS_BLOCKS, userModel.getBonusBlocks());

        File file = getPlayerFile(userModel.getId());
        try {
            yaml.save(file);
            return true;
        }
        catch (IOException e) {
            logger.error("Cannot write file " + file.getPath() + ":\n" + e.getMessage());
            return false;
        }
    }

    private File getPlayerFile(UUID uuid)
    {
        return new File(dataFolder, uuid.toString() + ".yml");
    }
}
