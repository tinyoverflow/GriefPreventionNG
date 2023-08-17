package me.tinyoverflow.griefprevention.data.repositories;

import me.tinyoverflow.griefprevention.data.models.UserModel;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class UserFileRepository implements UserRepository
{
    private static final String PATH_ACCRUED_BLOCKS = "accrued.blocks";
    private static final String PATH_ACCRUED_LIMIT = "accrued.limit";
    private static final String PATH_BONUS_BLOCKS = "bonus.blocks";

    private final ComponentLogger logger;
    private final File dataFolder;

    private final Map<UUID, UserModel> users = new HashMap<>();

    public UserFileRepository(ComponentLogger logger, File dataFolder)
    {
        this.logger = logger;
        this.dataFolder = Paths.get(dataFolder.getPath(), "users").toFile();
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

    public void loadFromFiles(File @NotNull [] files)
    {
        for (File file : files)
        {
            // Get UUID from filename
            String fileName = file.getName().substring(0, file.getName().lastIndexOf(".yml"));
            UUID uuid = UUID.fromString(fileName);
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

            // Hydrate the PlayerData object with the saved data
            UserModel userModel = new UserModel(uuid);
            userModel.setAccruedBlocks(yaml.getInt(PATH_ACCRUED_BLOCKS));
            userModel.setAccruedBlockLimit(yaml.getInt(PATH_ACCRUED_LIMIT));
            userModel.setBonusBlocks(yaml.getInt(PATH_ACCRUED_BLOCKS));

            // Store player in memory
            users.put(uuid, userModel);
        }
    }

    @Override
    public boolean save(@NotNull UserModel userModel)
    {
        File file = new File(dataFolder, userModel.getId() + ".yml");

        try
        {
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.set(PATH_ACCRUED_BLOCKS, userModel.getAccruedBlocks());
            yaml.set(PATH_ACCRUED_LIMIT, userModel.getAccruedBlockLimit());
            yaml.set(PATH_BONUS_BLOCKS, userModel.getBonusBlocks());
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
        for (UserModel userModel : users.values())
        {
            save(userModel);
        }
    }

    @Override
    public Optional<UserModel> get(UUID key)
    {
        if (users.containsKey(key))
        {
            return Optional.of(users.get(key));
        }

        return Optional.empty();
    }

    @Override
    public UserModel get(OfflinePlayer player)
    {
        if (users.containsKey(player.getUniqueId()))
        {
            return users.get(player.getUniqueId());
        }

        UserModel userModel = new UserModel(player.getUniqueId());
        users.put(player.getUniqueId(), userModel);

        return userModel;
    }
}
