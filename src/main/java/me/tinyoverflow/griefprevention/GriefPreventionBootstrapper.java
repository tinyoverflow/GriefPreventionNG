package me.tinyoverflow.griefprevention;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.bootstrap.PluginProviderContext;
import me.tinyoverflow.griefprevention.configurations.GriefPreventionConfiguration;
import me.tinyoverflow.griefprevention.data.RepositoryContainer;
import me.tinyoverflow.griefprevention.data.repositories.ClaimFileRepository;
import me.tinyoverflow.griefprevention.data.repositories.ClaimRepository;
import me.tinyoverflow.griefprevention.data.repositories.UserFileRepository;
import me.tinyoverflow.griefprevention.data.repositories.UserRepository;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;

import java.io.File;
import java.nio.file.Paths;

@SuppressWarnings("UnstableApiUsage")
public class GriefPreventionBootstrapper implements PluginBootstrap
{
    private ComponentLogger logger;

    @Override
    public void bootstrap(@NotNull BootstrapContext context)
    {
        logger = context.getLogger();
    }

    @Override
    public @NotNull JavaPlugin createPlugin(@NotNull PluginProviderContext context)
    {
        // Prepare Configuration
        GriefPreventionConfiguration configuration = getConfiguration(
                context.getDataDirectory().toFile()
        );

        // Prepare Repositories
        RepositoryContainer repositoryContainer = getRepositoryContainer(
                context.getLogger(),
                context.getDataDirectory().toFile()
        );

        // Instantiate New Plugin
        return new GriefPrevention(configuration, repositoryContainer);
    }

    /**
     * Loads the HOCON configuration with Configurate and returns it.
     *
     * @param dataDirectory Data directory to look into.
     * @return The configuration object.
     */
    private GriefPreventionConfiguration getConfiguration(File dataDirectory)
    {
        try
        {
            HoconConfigurationLoader configurationLoader = HoconConfigurationLoader.builder()
                    .path(Paths.get(dataDirectory.getPath(), "config.conf"))
                    .build();

            // Load and parse configuration from file.
            ConfigurationNode configurationNode = configurationLoader.load();
            GriefPreventionConfiguration configuration = configurationNode.get(GriefPreventionConfiguration.class);
            GriefPreventionConfiguration.setInstance(configuration);

            // Always saving the file on load to make sure that it exists
            // and is always up-to-date.
            configurationNode.set(configuration);
            configurationLoader.save(configurationNode);

            return configuration;
        }
        catch (ConfigurateException e)
        {
            logger.error("Could not load configuration file: " + e.getMessage());
        }

        return null;
    }

    private RepositoryContainer getRepositoryContainer(ComponentLogger logger, File dataDirectory)
    {
        UserRepository userRepository = new UserFileRepository(
                logger,
                dataDirectory
        );

        ClaimRepository claimRepository = new ClaimFileRepository(
                logger,
                dataDirectory
        );

        return RepositoryContainer.builder()
                .withUserRepository(userRepository)
                .withClaimRepository(claimRepository)
                .build();
    }
}
