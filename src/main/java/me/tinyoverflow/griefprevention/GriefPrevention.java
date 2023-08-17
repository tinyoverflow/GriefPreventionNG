/*
    GriefPrevention Server Plugin for Minecraft
    Copyright (C) 2012 Ryan Hamshire

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package me.tinyoverflow.griefprevention;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIBukkitConfig;
import dev.jorel.commandapi.CommandTree;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import lombok.Getter;
import me.tinyoverflow.griefprevention.commands.AdminClaimListCommand;
import me.tinyoverflow.griefprevention.commands.DeleteAllClaimsCommand;
import me.tinyoverflow.griefprevention.commands.IgnoreClaimsCommand;
import me.tinyoverflow.griefprevention.commands.ToolModeCommand;
import me.tinyoverflow.griefprevention.commands.claim.ClaimCreateCommand;
import me.tinyoverflow.griefprevention.configurations.GriefPreventionConfiguration;
import me.tinyoverflow.griefprevention.data.RepositoryContainer;
import me.tinyoverflow.griefprevention.datastore.DataStore;
import me.tinyoverflow.griefprevention.datastore.FlatFileDataStore;
import me.tinyoverflow.griefprevention.events.PreventBlockBreakEvent;
import me.tinyoverflow.griefprevention.handlers.EconomyHandler;
import me.tinyoverflow.griefprevention.listeners.block.*;
import me.tinyoverflow.griefprevention.listeners.claim.ClaimPermissionCheckListener;
import me.tinyoverflow.griefprevention.listeners.entity.*;
import me.tinyoverflow.griefprevention.listeners.hanging.HangingBreakListener;
import me.tinyoverflow.griefprevention.listeners.hanging.HangingPlaceListener;
import me.tinyoverflow.griefprevention.listeners.inventory.InventoryPickupItemListener;
import me.tinyoverflow.griefprevention.listeners.player.*;
import me.tinyoverflow.griefprevention.listeners.world.PortalCreateListener;
import me.tinyoverflow.griefprevention.listeners.world.StructureGrowListener;
import me.tinyoverflow.griefprevention.logger.ActivityLogger;
import me.tinyoverflow.griefprevention.logger.ActivityType;
import me.tinyoverflow.griefprevention.tasks.*;
import me.tinyoverflow.tolker.Tolker;
import me.tinyoverflow.tolker.repositories.ResourceBundleBag;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class GriefPrevention extends JavaPlugin
{
    //for convenience, a reference to the instance of this plugin
    public static GriefPrevention instance;

    //for logging to the console and log file
    private static java.util.logging.Logger logger;
    private final GriefPreventionConfiguration configuration;
    private final RepositoryContainer repositoryContainer;
    //this handles data storage, like player and region data
    @Getter
    public DataStore dataStore;
    //this tracks item stacks expected to drop which will need protection
    public ArrayList<PendingItemProtection> pendingItemWatchList = new ArrayList<>();
    //#region Deprecated Configuration Variables
    @Deprecated(forRemoval = true)
    public boolean config_lockDeathDropsInPvpWorlds;                 // whether players' dropped on death items are protected in pvp worlds
    @Deprecated(forRemoval = true)
    public boolean config_lockDeathDropsInNonPvpWorlds;              // whether players' dropped on death items are protected in non-pvp worlds
    @Deprecated(forRemoval = true)
    public int config_economy_claimBlocksMaxBonus;                   // max "bonus" blocks a player can buy.  set to zero for no limit.
    @Deprecated(forRemoval = true)
    public double config_economy_claimBlocksPurchaseCost;            // cost to purchase a claim block.  set to zero to disable purchase.
    @Deprecated(forRemoval = true)
    public double config_economy_claimBlocksSellValue;               // return on a sold claim block.  set to zero to disable sale.
    @Deprecated(forRemoval = true)
    public boolean config_blockClaimExplosions;                      // whether explosions may destroy claimed blocks
    @Deprecated(forRemoval = true)
    public boolean config_blockSurfaceCreeperExplosions;             // whether creeper explosions near or above the surface destroy blocks
    @Deprecated(forRemoval = true)
    public boolean config_blockSurfaceOtherExplosions;               // whether non-creeper explosions near or above the surface destroy blocks
    @Deprecated(forRemoval = true)
    public boolean config_blockSkyTrees;                             // whether players can build trees on platforms in the sky
    @Deprecated(forRemoval = true)
    public boolean config_fireSpreads;                               // whether fire spreads outside of claims
    @Deprecated(forRemoval = true)
    public boolean config_fireDestroys;                              // whether fire destroys blocks outside of claims
    @Deprecated(forRemoval = true)
    public boolean config_whisperNotifications;                      // whether whispered messages will broadcast to administrators in game
    @Deprecated(forRemoval = true)
    public boolean config_signNotifications;                         // whether sign content will broadcast to administrators in game
    @Deprecated(forRemoval = true)
    public boolean config_visualizationAntiCheatCompat;              // whether to engage compatibility mode for anti-cheat plugins
    @Deprecated(forRemoval = true)
    public boolean config_endermenMoveBlocks;                        // whether endermen may move blocks around
    @Deprecated(forRemoval = true)
    public boolean config_silverfishBreakBlocks;                     // whether silverfish may break blocks
    @Deprecated(forRemoval = true)
    public boolean config_creaturesTrampleCrops;                     // whether non-player entities may trample crops
    @Deprecated(forRemoval = true)
    public boolean config_rabbitsEatCrops;                           // whether rabbits may eat crops
    @Deprecated(forRemoval = true)
    public boolean config_zombiesBreakDoors;                         // whether hard-mode zombies may break down wooden doors
    @Deprecated(forRemoval = true)
    public HashMap<String, Integer> config_seaLevelOverride;         // override for sea level, because bukkit doesn't report the right value for all situations
    @Deprecated(forRemoval = true)
    public boolean config_limitTreeGrowth;                           // whether trees should be prevented from growing into a claim from outside
    @Deprecated(forRemoval = true)
    public PistonMode config_pistonMovement;                         // Setting for piston check options
    @Deprecated(forRemoval = true)
    public boolean config_pistonExplosionSound;                      // whether pistons make an explosion sound when they get removed
    @Deprecated(forRemoval = true)
    public boolean config_advanced_fixNegativeClaimblockAmounts;     // whether to attempt to fix negative claim block amounts (some addons cause/assume players can go into negative amounts)
    @Deprecated(forRemoval = true)
    public int config_advanced_claim_expiration_check_rate;          // How often GP should check for expired claims, amount in seconds
    @Deprecated(forRemoval = true)
    public int config_advanced_offlineplayer_cache_days;             // Cache players who have logged in within the last x number of days
    //custom log settings
    @Deprecated(forRemoval = true)
    public int config_logs_daysToKeep;
    @Deprecated(forRemoval = true)
    public boolean config_logs_socialEnabled;
    @Deprecated(forRemoval = true)
    public boolean config_logs_suspiciousEnabled;
    @Deprecated(forRemoval = true)
    public boolean config_logs_adminEnabled;
    @Deprecated(forRemoval = true)
    public boolean config_logs_debugEnabled;
    @Deprecated(forRemoval = true)
    public boolean config_logs_mutedChatEnabled;
    //Track scheduled "rescues" so we can cancel them if the player happens to teleport elsewhere, so we can cancel it.
    public ConcurrentHashMap<UUID, BukkitTask> portalReturnTaskMap = new ConcurrentHashMap<>();
    //#endregion
    //log entry manager for GP's custom log files
    // Player event handler
    @Deprecated(forRemoval = true)
    HashMap<World, Boolean> config_pvp_specifiedWorlds;                //list of worlds where pvp anti-grief rules apply, according to the config file
    //helper method to resolve a player by name
    ConcurrentHashMap<String, UUID> playerNameToIDMap = new ConcurrentHashMap<>();
    private ActivityLogger activityLogger;
    @Getter
    private Tolker tolker;
    private EconomyHandler economyHandler;

    public GriefPrevention(GriefPreventionConfiguration configuration, RepositoryContainer repositoryContainer)
    {
        this.configuration = configuration;
        this.repositoryContainer = repositoryContainer;
    }

    //adds a server log entry
    public static synchronized void AddLogEntry(String entry, ActivityType activityType, boolean excludeFromServerLogs)
    {
        if (activityType != null && GriefPrevention.instance.activityLogger != null)
        {
            GriefPrevention.instance.activityLogger.log(activityType, entry);
        }
        if (!excludeFromServerLogs) logger.info(entry);
    }

    public static synchronized void AddLogEntry(String entry, ActivityType activityType)
    {
        AddLogEntry(entry, activityType, false);
        Bukkit.getOfflinePlayer("test");
    }

    public static synchronized void AddLogEntry(String entry)
    {
        AddLogEntry(entry, ActivityType.DEBUG);
    }

    public static String getFriendlyLocationString(Location location)
    {
        return location.getWorld().getName() + ": x" + location.getBlockX() + ", z" + location.getBlockZ();
    }

    //helper method to resolve a player name from the player's UUID
    public static @NotNull String lookupPlayerName(@Nullable UUID playerID)
    {
        //parameter validation
        if (playerID == null) return "someone";

        //check the cache
        OfflinePlayer player = GriefPrevention.instance.getServer().getOfflinePlayer(playerID);
        return lookupPlayerName(player);
    }

    static @NotNull String lookupPlayerName(@NotNull AnimalTamer tamer)
    {
        // If the tamer is not a player or has played, prefer their name if it exists.
        if (!(tamer instanceof OfflinePlayer player) || player.hasPlayedBefore() || player.isOnline())
        {
            String name = tamer.getName();
            if (name != null) return name;
        }

        // Fall back to tamer's UUID.
        return "someone(" + tamer.getUniqueId() + ")";
    }

    //cache for player name lookups, to save searches of all offline players
    public static void cacheUUIDNamePair(UUID playerID, String playerName)
    {
        //store the reverse mapping
        GriefPrevention.instance.playerNameToIDMap.put(playerName, playerID);
        GriefPrevention.instance.playerNameToIDMap.put(playerName.toLowerCase(), playerID);
    }

    public static boolean isInventoryEmpty(Player player)
    {
        PlayerInventory inventory = player.getInventory();
        ItemStack[] armorStacks = inventory.getArmorContents();

        //check armor slots, stop if any items are found
        for (ItemStack armorStack : armorStacks)
        {
            if (!(armorStack == null || armorStack.getType() == Material.AIR)) return false;
        }

        //check other slots, stop if any items are found
        ItemStack[] generalStacks = inventory.getContents();
        for (ItemStack generalStack : generalStacks)
        {
            if (!(generalStack == null || generalStack.getType() == Material.AIR)) return false;
        }

        return true;
    }

    //ensures a piece of the managed world is loaded into server memory
    //(generates the chunk if necessary)
    private static void GuaranteeChunkLoaded(Location location)
    {
        Chunk chunk = location.getChunk();
        while (!chunk.isLoaded() || !chunk.load(true)) ;
    }

    //sends a color-coded message to a player
    @Deprecated(forRemoval = true)
    public static void sendMessage(Player player, ChatColor color, Messages messageID, String... args)
    {
        sendMessage(player, color, messageID, 0, args);
    }

    //sends a color-coded message to a player
    @Deprecated(forRemoval = true)
    public static void sendMessage(Player player, ChatColor color, Messages messageID, long delayInTicks, String... args)
    {
        String message = GriefPrevention.instance.dataStore.getMessage(messageID, args);
        sendMessage(player, color, message, delayInTicks);
    }

    //sends a color-coded message to a player
    @Deprecated(forRemoval = true)
    public static void sendMessage(Player player, ChatColor color, String message)
    {
        if (message == null || message.length() == 0) return;

        if (player == null)
        {
            GriefPrevention.AddLogEntry(color + message);
        }
        else
        {
            player.sendMessage(color + message);
        }
    }

    @Deprecated(forRemoval = true)
    public static void sendMessage(Player player, ChatColor color, String message, long delayInTicks)
    {
        SendPlayerMessageTask task = new SendPlayerMessageTask(player, color, message);

        //Only schedule if there should be a delay. Otherwise, send the message right now, else the message will appear out of order.
        if (delayInTicks > 0)
        {
            GriefPrevention.instance.getServer().getScheduler().runTaskLater(
                    GriefPrevention.instance,
                    task,
                    delayInTicks
            );
        }
        else
        {
            task.run();
        }
    }

    public static boolean isNewToServer(Player player)
    {
        if (player.getStatistic(Statistic.PICKUP, Material.OAK_LOG) > 0 ||
            player.getStatistic(Statistic.PICKUP, Material.SPRUCE_LOG) > 0 ||
            player.getStatistic(Statistic.PICKUP, Material.BIRCH_LOG) > 0 ||
            player.getStatistic(Statistic.PICKUP, Material.JUNGLE_LOG) > 0 ||
            player.getStatistic(Statistic.PICKUP, Material.ACACIA_LOG) > 0 ||
            player.getStatistic(Statistic.PICKUP, Material.DARK_OAK_LOG) > 0)
        {
            return false;
        }

        PlayerData playerData = instance.dataStore.getPlayerData(player.getUniqueId());

        return playerData.getClaims().size() == 0;
    }

    public void onLoad()
    {
        // Load the plugin config first, so we have everything ready
        // for all dependencies that might come up.
        loadPluginConfig();

        // Load all messages and prepare Tolker for further use.
        ResourceBundle messagesBundle = ResourceBundle.getBundle("messages", Locale.forLanguageTag("de"));
        tolker = new Tolker(new ResourceBundleBag(messagesBundle));
        tolker.registerDefaultSerializers();

        // As CommandAPI is loaded as a library and not as a plugin,
        // we need to trigger this event manually.
        CommandAPI.onLoad(new CommandAPIBukkitConfig(this));
    }

    public void onEnable()
    {
        // As CommandAPI is loaded as a library and not as a plugin,
        // we need to trigger this event manually.
        CommandAPI.onEnable();

        instance = this;

        // Register all commands for this plugin.
        registerCommands();

        // Initialize custom logger and register the scheduled tasks.
        logger = instance.getLogger();
        activityLogger = new ActivityLogger(
                getLogger(),
                new File(getDataFolder(), "logs"),
                getPluginConfig().getLoggerConfiguration()
        );
        activityLogger.registerTasks(this, getServer().getScheduler());

        AddLogEntry("Finished loading configuration.");

        // if not using the database because it's not configured or because there was a problem, use the file system to store data
        // this is the preferred method, as it's simpler than the database scenario
        if (dataStore == null)
        {
            File oldclaimdata = new File(getDataFolder(), "ClaimData");
            if (oldclaimdata.exists())
            {
                if (!FlatFileDataStore.hasData())
                {
                    File claimdata = new File(
                            "plugins" + File.separator + "GriefPrevention" + File.separator + "ClaimData");
                    oldclaimdata.renameTo(claimdata);
                    File oldplayerdata = new File(getDataFolder(), "PlayerData");
                    File playerdata = new File(
                            "plugins" + File.separator + "GriefPrevention" + File.separator + "PlayerData");
                    oldplayerdata.renameTo(playerdata);
                }
            }
            try
            {
                dataStore = new FlatFileDataStore(getDataFolder());
            }
            catch (Exception e)
            {
                GriefPrevention.AddLogEntry("Unable to initialize the file system data store.  Details:");
                GriefPrevention.AddLogEntry(e.getMessage());
                e.printStackTrace();
            }
        }

        String dataMode = (dataStore instanceof FlatFileDataStore) ? "(File Mode)" : "(Database Mode)";
        AddLogEntry("Finished loading data " + dataMode + ".");

        //unless claim block accrual is disabled, start the recurring per 10-minute event to give claim blocks to online players
        //20L ~ 1 second
        if (getPluginConfig().getClaimConfiguration().getClaimBlocksConfiguration().accrued.isAccrualEnabled())
        {
            DeliverClaimBlocksTask task = new DeliverClaimBlocksTask(null, this);
            getServer().getScheduler().scheduleSyncRepeatingTask(this, task, 20L * 60 * 10, 20L * 60 * 10);
        }

        //start the recurring cleanup event for entities in creative worlds
        EntityCleanupTask task = new EntityCleanupTask(0);
        getServer().getScheduler().scheduleSyncDelayedTask(GriefPrevention.instance, task, 20L * 60 * 2);

        //start recurring cleanup scan for unused claims belonging to inactive players
        FindUnusedClaimsTask task2 = new FindUnusedClaimsTask();
        getServer().getScheduler().scheduleSyncRepeatingTask(
                this,
                task2,
                20L * 60,
                20L * config_advanced_claim_expiration_check_rate
        );

        //register for events
        PluginManager pluginManager = getServer().getPluginManager();

        // Block Events
        registerEvents(
                new BlockBreakListener(),
                new BlockBurnListener(dataStore),
                new BlockDispenseListener(dataStore),
                new BlockExplodeListener(),
                new BlockFormListener(dataStore),
                new BlockFromToListener(dataStore),
                new BlockIgniteListener(),
                new BlockMultiPlaceListener(),
                new BlockPistonListener(dataStore),
                new BlockPlaceListener(dataStore),
                new BlockSpreadListener(dataStore),
                new SignChangeListener(dataStore)
        );

        // Entity Events
        registerEvents(
                new CreatureSpawnListener(this, dataStore),
                new EntityBlockFormListener(this),
                new EntityBreakDoorListener(this),
                new EntityChangeBlockListener(this, dataStore),
                new EntityDeathListener(this, dataStore),
                new EntityExplodeListener(),
                new EntityInteractListener(this),
                new EntityPickupItemListener(this, dataStore),
                new EntityPortalEnterListener(this),
                new EntityPortalExitListener(),
                new ExpBottleListener(this),
                new ItemMergeListener(),
                new ItemSpawnListener(this),
                new ProjectileHitListener(dataStore)
        );

        // Hanging Events
        registerEvents(
                new HangingBreakListener(this, dataStore),
                new HangingPlaceListener(this, dataStore)
        );

        // Inventory Events
        registerEvents(
                new InventoryPickupItemListener(dataStore)
        );

        // Player Events
        registerEvents(
                new PlayerBucketEmptyListener(this, dataStore),
                new PlayerBucketFillListener(this),
                new PlayerChatListener(this, dataStore),
                new PlayerCommandPreprocessListener(this, dataStore),
                new PlayerDropItemListener(this, dataStore),
                new PlayerEggThrowListener(this, dataStore),
                new PlayerFishListener(this),
                new PlayerInteractAtEntityListener(),
                new PlayerInteractEntityListener(this, dataStore),
                new PlayerInteractListener(this, dataStore),
                new PlayerItemHeldListener(this),
                new PlayerJoinListener(this, dataStore),
                new PlayerKickListener(dataStore),
                new PlayerLoginListener(dataStore),
                new PlayerPortalListener(this),
                new PlayerQuitListener(this, dataStore),
                new PlayerRaidTriggerListener(this, dataStore),
                new PlayerRespawnListener(this),
                new PlayerTakeLecternBookListener(dataStore),
                new PlayerTeleportListener(this, dataStore)
        );

        // World Events
        registerEvents(
                new PortalCreateListener(dataStore),
                new StructureGrowListener(dataStore)
        );

        //siege events
        ClaimPermissionCheckListener siegeEventHandler = new ClaimPermissionCheckListener();
        pluginManager.registerEvents(siegeEventHandler, this);

        //vault-based economy integration
        economyHandler = new EconomyHandler(this);
        pluginManager.registerEvents(economyHandler, this);

        //cache offline players
        OfflinePlayer[] offlinePlayers = getServer().getOfflinePlayers();
        CacheOfflinePlayerNamesThread namesThread = new CacheOfflinePlayerNamesThread(
                offlinePlayers,
                playerNameToIDMap
        );
        namesThread.setPriority(Thread.MIN_PRIORITY);
        namesThread.start();

        AddLogEntry("Boot finished.");
    }

    private void registerCommands()
    {
        new CommandTree("claimadmin")
                .withPermission(Permissions.Commands.CLAIM_ADMIN)
                .then(new LiteralArgument("list")
                        .withPermission(Permissions.Commands.ClaimAdmin.LIST)
                        .executesPlayer(new AdminClaimListCommand(this))
                )
                .then(new LiteralArgument("ignore")
                        .withPermission(Permissions.Commands.ClaimAdmin.IGNORE)
                        .executesPlayer(new IgnoreClaimsCommand(this))
                )
                .then(new LiteralArgument("delete-all")
                        .withPermission(Permissions.Commands.ClaimAdmin.DELETE_ALL)
                        .then(new MultiLiteralArgument("type", List.of("user", "admin", "all"))
                                .executesPlayer(new DeleteAllClaimsCommand(this))
                        )
                )
                .register();

        new CommandTree("toolmode")
                .withPermission(Permissions.Commands.TOOLMODE)
                .executesPlayer(new ToolModeCommand(this)::help)
                .then(new LiteralArgument("mode", "admin")
                        .setListed(true)
                        .withPermission(Permissions.Commands.ToolMode.ADMIN)
                        .executesPlayer(new ToolModeCommand(this))
                )
                .then(new LiteralArgument("mode", "basic")
                        .setListed(true)
                        .withPermission(Permissions.Commands.ToolMode.BASIC)
                        .executesPlayer(new ToolModeCommand(this))
                )
                .then(new LiteralArgument("mode", "subdivide")
                        .setListed(true)
                        .withPermission(Permissions.Commands.ToolMode.SUBDIVIDE)
                        .executesPlayer(new ToolModeCommand(this))
                )
                .then(new LiteralArgument("mode", "restore-nature")
                        .setListed(true)
                        .withPermission(Permissions.Commands.ToolMode.RESTORE_NATURE)
                        .executesPlayer(new ToolModeCommand(this))
                )
                .then(new LiteralArgument("mode", "restore-nature-aggressive")
                        .setListed(true)
                        .withPermission(Permissions.Commands.ToolMode.RESTORE_NATURE_AGGRESSIVE)
                        .executesPlayer(new ToolModeCommand(this))
                )
                .then(new LiteralArgument("mode", "restore-nature-fill")
                        .setListed(true)
                        .withPermission(Permissions.Commands.ToolMode.RESTORE_NATURE_FILL)
                        .then(new IntegerArgument("radius", 1, 10)
                                .executesPlayer(new ToolModeCommand(this))
                        )
                )
                .register();

        new CommandTree("claim")
                .withPermission(Permissions.Commands.CLAIM)
                .executesPlayer(new ClaimCreateCommand(
                        configuration.getClaimConfiguration(),
                        tolker,
                        repositoryContainer
                ))
                .register();

        /*
          /claimadmin
               list
               delete-all (user/admin)
               delete-in-world (user/admin)
               ignore         *

          /claim
               create
               abandon / delete?
               abandon-all
               list
               extend
               transfer (player)
               config
                   explosions (true/false)
                   inherit-permission (true/false)

          /claimblocks
               buy (amount)
               sell (amount)
               adjust (player) (bonus/accrued) (modification)
               limit (player) (limit)

          /toolmode
               basic
               subdivide
               admin

          /trust (player) [level=build]   // none = remove, otherwise build, access, switch, container

          /untrust (player)

          /trapped

          /siege (player)

          /givepet (player)

          /unlockitems [player]
         */

        // CommandManager commandManager = new CommandManager();

        // Admin
        //        commandManager.add(new AdjustBonusClaimBlocksAllCommand("adjustbonusclaimblocksall", this));
        //        commandManager.add(new AdjustBonusClaimBlocksCommand("adjustbonusclaimblocks", this));
        //        commandManager.add(new AdjustClaimBlockLimitCommand("adjustclaimblocklimit", this));
        //        commandManager.add(new DeleteAllAdminClaimsCommand("deleteuserclaimsinworld", this));
        //        commandManager.add(new DeleteClaimsInWorldCommand("deleteclaimsinworld", this));
        //        commandManager.add(new DeleteUserClaimsInWorldCommand("deleteuserclaimsinworld", this));
        //        commandManager.add(new RestoreNatureCommand("restorenature", this));
        //        commandManager.add(new RestoreNatureFillCommand("restorenaturefill", this));
        //        commandManager.add(new SetAccruedClaimBlocksCommand("setaccruedclaimblocks", this));

        // Claim            DONE
        //        commandManager.add(new ClaimCommand("claim", this));
        //        commandManager.add(new AbandonAllClaimsCommand("abandonallclaims", this));
        //        commandManager.add(new ClaimAbandonCommand("abandonclaim", this));
        //        commandManager.add(new ClaimExplosionsCommand("claimexplosions", this));
        //        commandManager.add(new ClaimExtendCommand("extendclaim", this));
        //        commandManager.add(new ClaimsListCommand("claimslist", this));
        //        commandManager.add(new DeleteClaimCommand("deleteclaim", this));
        //        commandManager.add(new RestrictSubClaimCommand("restrictsubclaim", this));
        //        commandManager.add(new TransferClaimCommand("transferclaim", this));

        // Claimblocks      DONE
        //        commandManager.add(new SellClaimBlocksCommand("sellclaimblocks", this));
        //        commandManager.add(new BuyClaimBlocksCommand("buyclaimblocks", this));

        //         Tool Modes       DONE
        //        commandManager.add(new AdminClaimsCommand("adminclaims", this));
        //        commandManager.add(new BasicClaimsCommand("basicclaims", this));
        //        commandManager.add(new SubdivideClaimsCommand("subdivideclaims", this));

        // Trust            DONE
        //        commandManager.add(new TrustCommand("trust", this));
        //        commandManager.add(new UntrustCommand("untrust", this));
        //        commandManager.add(new TrustListCommand("trustlist", this));

        // Help             DONE
        //        commandManager.add(new ClaimBookCommand("claimbook", this));

        // Misc
        //        commandManager.add(new GivePetCommand("givepet", this));
        //        commandManager.add(new TrappedCommand("trapped", this));
        //        commandManager.add(new SiegeCommand("siege", this));
        //        commandManager.add(new UnlockItemsCommand("unlockitems", this));

        // commandManager.register();
    }

    private void registerEvents(Listener... listeners)
    {
        for (Listener listener : listeners)
        {
            getServer().getPluginManager().registerEvents(listener, this);
        }
    }

    public GriefPreventionConfiguration getPluginConfig()
    {
        return configuration;
    }

    /**
     * @deprecated Loading the configuration is done inside the bootstrapper.
     * TODO: Remove legacy code once all references have been updated.
     */
    private void loadPluginConfig()
    {
        //load the config if it exists
        FileConfiguration config = YamlConfiguration.loadConfiguration(new File(DataStore.configFilePath));
        FileConfiguration outConfig = new YamlConfiguration();

        //get (deprecated node) claims world names from the config file
        List<World> worlds = getServer().getWorlds();
        List<String> deprecated_claimsEnabledWorldNames = config.getStringList("GriefPrevention.Claims.Worlds");

        //validate that list
        for (int i = 0; i < deprecated_claimsEnabledWorldNames.size(); i++)
        {
            String worldName = deprecated_claimsEnabledWorldNames.get(i);
            World world = getServer().getWorld(worldName);
            if (world == null)
            {
                deprecated_claimsEnabledWorldNames.remove(i--);
            }
        }

        //get (deprecated node) creative world names from the config file
        List<String> deprecated_creativeClaimsEnabledWorldNames = config.getStringList(
                "GriefPrevention.Claims.CreativeRulesWorlds");

        //validate that list
        for (int i = 0; i < deprecated_creativeClaimsEnabledWorldNames.size(); i++)
        {
            String worldName = deprecated_creativeClaimsEnabledWorldNames.get(i);
            World world = getServer().getWorld(worldName);
            if (world == null)
            {
                deprecated_claimsEnabledWorldNames.remove(i--);
            }
        }

        //sea level
        config_seaLevelOverride = new HashMap<>();
        for (World world : worlds)
        {
            int seaLevelOverride = config.getInt("GriefPrevention.SeaLevelOverrides." + world.getName(), -1);
            outConfig.set("GriefPrevention.SeaLevelOverrides." + world.getName(), seaLevelOverride);
            config_seaLevelOverride.put(world.getName(), seaLevelOverride);
        }

        String bannedPvPCommandsList = config.getString(
                "GriefPrevention.PvP.BlockedSlashCommands",
                "/home;/vanish;/spawn;/tpa"
        );

        config_economy_claimBlocksMaxBonus = config.getInt("GriefPrevention.Economy.ClaimBlocksMaxBonus", 0);
        config_economy_claimBlocksPurchaseCost = config.getDouble(
                "GriefPrevention.Economy.ClaimBlocksPurchaseCost",
                0
        );
        config_economy_claimBlocksSellValue = config.getDouble("GriefPrevention.Economy.ClaimBlocksSellValue", 0);

        config_lockDeathDropsInPvpWorlds = config.getBoolean(
                "GriefPrevention.ProtectItemsDroppedOnDeath.PvPWorlds",
                false
        );
        config_lockDeathDropsInNonPvpWorlds = config.getBoolean(
                "GriefPrevention.ProtectItemsDroppedOnDeath.NonPvPWorlds",
                true
        );

        config_blockClaimExplosions = config.getBoolean("GriefPrevention.BlockLandClaimExplosions", true);
        config_blockSurfaceCreeperExplosions = config.getBoolean(
                "GriefPrevention.BlockSurfaceCreeperExplosions",
                true
        );
        config_blockSurfaceOtherExplosions = config.getBoolean(
                "GriefPrevention.BlockSurfaceOtherExplosions",
                true
        );
        config_blockSkyTrees = config.getBoolean("GriefPrevention.LimitSkyTrees", true);
        config_limitTreeGrowth = config.getBoolean("GriefPrevention.LimitTreeGrowth", false);
        config_pistonExplosionSound = config.getBoolean("GriefPrevention.PistonExplosionSound", true);
        config_pistonMovement = PistonMode.of(config.getString("GriefPrevention.PistonMovement", "CLAIMS_ONLY"));
        if (config.isBoolean("GriefPrevention.LimitPistonsToLandClaims") && !config.getBoolean(
                "GriefPrevention.LimitPistonsToLandClaims"))
        {
            config_pistonMovement = PistonMode.EVERYWHERE_SIMPLE;
        }
        if (config.isBoolean("GriefPrevention.CheckPistonMovement") && !config.getBoolean(
                "GriefPrevention.CheckPistonMovement"))
        {
            config_pistonMovement = PistonMode.IGNORED;
        }

        config_fireSpreads = config.getBoolean("GriefPrevention.FireSpreads", false);
        config_fireDestroys = config.getBoolean("GriefPrevention.FireDestroys", false);

        config_visualizationAntiCheatCompat = config.getBoolean(
                "GriefPrevention.VisualizationAntiCheatCompatMode",
                false
        );

        config_endermenMoveBlocks = config.getBoolean("GriefPrevention.EndermenMoveBlocks", false);
        config_silverfishBreakBlocks = config.getBoolean("GriefPrevention.SilverfishBreakBlocks", false);
        config_creaturesTrampleCrops = config.getBoolean("GriefPrevention.CreaturesTrampleCrops", false);
        config_rabbitsEatCrops = config.getBoolean("GriefPrevention.RabbitsEatCrops", true);
        config_zombiesBreakDoors = config.getBoolean("GriefPrevention.HardModeZombiesBreakDoors", false);

        config_advanced_fixNegativeClaimblockAmounts = config.getBoolean(
                "GriefPrevention.Advanced.fixNegativeClaimblockAmounts",
                true
        );
        config_advanced_claim_expiration_check_rate = config.getInt(
                "GriefPrevention.Advanced.ClaimExpirationCheckRate",
                60
        );
        config_advanced_offlineplayer_cache_days = config.getInt(
                "GriefPrevention.Advanced.OfflinePlayer_cache_days",
                90
        );

        for (World world : worlds)
        {
            outConfig.set("GriefPrevention.PvP.RulesEnabledInWorld." + world.getName(), pvpRulesApply(world));
        }

        outConfig.set("GriefPrevention.Economy.ClaimBlocksMaxBonus", config_economy_claimBlocksMaxBonus);
        outConfig.set("GriefPrevention.Economy.ClaimBlocksPurchaseCost", config_economy_claimBlocksPurchaseCost);
        outConfig.set("GriefPrevention.Economy.ClaimBlocksSellValue", config_economy_claimBlocksSellValue);

        outConfig.set("GriefPrevention.ProtectItemsDroppedOnDeath.PvPWorlds", config_lockDeathDropsInPvpWorlds);
        outConfig.set(
                "GriefPrevention.ProtectItemsDroppedOnDeath.NonPvPWorlds",
                config_lockDeathDropsInNonPvpWorlds
        );

        outConfig.set("GriefPrevention.BlockLandClaimExplosions", config_blockClaimExplosions);
        outConfig.set("GriefPrevention.BlockSurfaceCreeperExplosions", config_blockSurfaceCreeperExplosions);
        outConfig.set("GriefPrevention.BlockSurfaceOtherExplosions", config_blockSurfaceOtherExplosions);
        outConfig.set("GriefPrevention.LimitSkyTrees", config_blockSkyTrees);
        outConfig.set("GriefPrevention.LimitTreeGrowth", config_limitTreeGrowth);
        outConfig.set("GriefPrevention.PistonMovement", config_pistonMovement.name());
        outConfig.set("GriefPrevention.CheckPistonMovement", null);
        outConfig.set("GriefPrevention.LimitPistonsToLandClaims", null);
        outConfig.set("GriefPrevention.PistonExplosionSound", config_pistonExplosionSound);

        outConfig.set("GriefPrevention.FireSpreads", config_fireSpreads);
        outConfig.set("GriefPrevention.FireDestroys", config_fireDestroys);

        outConfig.set("GriefPrevention.AdminsGetWhispers", config_whisperNotifications);
        outConfig.set("GriefPrevention.AdminsGetSignNotifications", config_signNotifications);

        outConfig.set("GriefPrevention.VisualizationAntiCheatCompatMode", config_visualizationAntiCheatCompat);

        outConfig.set("GriefPrevention.EndermenMoveBlocks", config_endermenMoveBlocks);
        outConfig.set("GriefPrevention.SilverfishBreakBlocks", config_silverfishBreakBlocks);
        outConfig.set("GriefPrevention.CreaturesTrampleCrops", config_creaturesTrampleCrops);
        outConfig.set("GriefPrevention.RabbitsEatCrops", config_rabbitsEatCrops);
        outConfig.set("GriefPrevention.HardModeZombiesBreakDoors", config_zombiesBreakDoors);

        outConfig.set(
                "GriefPrevention.Advanced.fixNegativeClaimblockAmounts",
                config_advanced_fixNegativeClaimblockAmounts
        );
        outConfig.set(
                "GriefPrevention.Advanced.ClaimExpirationCheckRate",
                config_advanced_claim_expiration_check_rate
        );
        outConfig.set(
                "GriefPrevention.Advanced.OfflinePlayer_cache_days",
                config_advanced_offlineplayer_cache_days
        );

        outConfig.set("GriefPrevention.ConfigVersion", 1);
    }

    public void onDisable()
    {
        //save data for any online players
        @SuppressWarnings("unchecked")
        Collection<Player> players = (Collection<Player>) getServer().getOnlinePlayers();
        for (Player player : players)
        {
            UUID playerID = player.getUniqueId();
            PlayerData playerData = dataStore.getPlayerData(playerID);
            dataStore.savePlayerDataSync(playerID, playerData);
        }

        dataStore.close();

        //dump any remaining unwritten log entries
        activityLogger.flush();

        AddLogEntry("GriefPrevention disabled.");
    }

    //called when a player spawns, applies protection for that player if necessary
    @Deprecated(forRemoval = true)
    public void checkPvpProtectionNeeded(Player player)
    {
        //if anti spawn camping feature is not enabled, do nothing
        if (!getPluginConfig().getPvpConfiguration().isProtectFreshSpawns()) return;

        //if pvp is disabled, do nothing
        if (!pvpRulesApply(player.getWorld())) return;

        //if player is in creative mode, do nothing
        if (player.getGameMode() == GameMode.CREATIVE) return;

        //if the player has the damage any player permission enabled, do nothing
        if (player.hasPermission("griefprevention.nopvpimmunity")) return;

        //check inventory for well, anything
        if (GriefPrevention.isInventoryEmpty(player))
        {
            //if empty, apply immunity
            PlayerData playerData = dataStore.getPlayerData(player.getUniqueId());
            playerData.pvpImmune = true;

            //inform the player after he finishes respawning
            sendMessage(player, TextMode.SUCCESS, Messages.PvPImmunityStart, 5L);

            //start a task to re-check this player's inventory every minute until his immunity is gone
            PvPImmunityValidationTask task = new PvPImmunityValidationTask(player);
            getServer().getScheduler().scheduleSyncDelayedTask(this, task, 1200L);
        }
    }

    //moves a player from the claim he's in to a nearby wilderness location
    @Deprecated(forRemoval = true)
    public Location ejectPlayer(Player player)
    {
        //look for a suitable location
        Location candidateLocation = player.getLocation();
        while (true)
        {
            Claim claim = GriefPrevention.instance.dataStore.getClaimAt(candidateLocation, false, null);

            //if there's a claim here, keep looking
            if (claim != null)
            {
                candidateLocation = new Location(
                        claim.lesserBoundaryCorner.getWorld(),
                        claim.lesserBoundaryCorner.getBlockX() - 1,
                        claim.lesserBoundaryCorner.getBlockY(),
                        claim.lesserBoundaryCorner.getBlockZ() - 1
                );
            }

            //otherwise find a safe place to teleport the player
            else
            {
                //find a safe height, a couple of blocks above the surface
                GuaranteeChunkLoaded(candidateLocation);
                Block highestBlock = candidateLocation.getWorld().getHighestBlockAt(
                        candidateLocation.getBlockX(),
                        candidateLocation.getBlockZ()
                );
                Location destination = new Location(
                        highestBlock.getWorld(),
                        highestBlock.getX(),
                        highestBlock.getY() + 2,
                        highestBlock.getZ()
                );
                player.teleport(destination);
                return destination;
            }
        }
    }

    /**
     * TODO: Remove this method.
     *
     * @param world The world to check for.
     * @return True if protection is not disabled, false otherwise.
     */
    @Deprecated(forRemoval = true)
    public boolean claimsEnabledForWorld(World world)
    {
        return getPluginConfig().getClaimConfiguration().isWorldEnabled(world);
    }

    //determines whether creative anti-grief rules apply at a location
    @Deprecated(forRemoval = true)
    public boolean creativeRulesApply(Location location)
    {
        return getPluginConfig().getClaimConfiguration().getWorldMode(location.getWorld()) == ClaimsMode.Creative;
    }

    @Deprecated(forRemoval = true)
    public String allowBuild(Player player, Location location, Material material)
    {
        if (!GriefPrevention.instance.claimsEnabledForWorld(location.getWorld())) return null;

        PlayerData playerData = dataStore.getPlayerData(player.getUniqueId());
        Claim claim = dataStore.getClaimAt(location, false, playerData.lastClaim);

        //exception: administrators in ignore claims mode
        if (playerData.ignoreClaims) return null;

        //wilderness rules
        if (claim == null)
        {
            //no building in the wilderness in creative mode
            if (creativeRulesApply(location) ||
                getPluginConfig().getClaimConfiguration().getWorldMode(location.getWorld()) ==
                ClaimsMode.SurvivalRequiringClaims)
            {
                //exception: when chest claims are enabled, players who have zero land claims and are placing a chest
                if (material != Material.CHEST || playerData.getClaims().size() > 0 ||
                    GriefPrevention.instance.getPluginConfig()
                                            .getClaimConfiguration()
                                            .getCreationConfiguration().automaticPreferredRadius ==
                    -1)
                {
                    String reason = dataStore.getMessage(Messages.NoBuildOutsideClaims);
                    if (player.hasPermission("griefprevention.ignoreclaims"))
                    {
                        reason += "  " + dataStore.getMessage(Messages.IgnoreClaimsAdvertisement);
                    }
                    reason += "  " + dataStore.getMessage(
                            Messages.CreativeBasicsVideo2,
                            DataStore.CREATIVE_VIDEO_URL
                    );
                    return reason;
                }
                else
                {
                    return null;
                }
            }

            //but it's fine in survival mode
            else
            {
                return null;
            }
        }

        //if not in the wilderness, then apply claim rules (permissions, etc)
        else
        {
            //cache the claim for later reference
            playerData.lastClaim = claim;
            Block block = location.getBlock();

            Supplier<String> supplier = claim.checkPermission(
                    player,
                    ClaimPermission.Build,
                    new BlockPlaceEvent(
                            block,
                            block.getState(),
                            block,
                            new ItemStack(material),
                            player,
                            true,
                            EquipmentSlot.HAND
                    )
            );

            if (supplier == null) return null;

            return supplier.get();
        }
    }

    @Deprecated(forRemoval = true)
    public String allowBreak(Player player, Block block, Location location)
    {
        return allowBreak(player, block, location, new BlockBreakEvent(block, player));
    }

    @Deprecated(forRemoval = true)
    public String allowBreak(Player player, Block block, Location location, BlockBreakEvent breakEvent)
    {
        if (!GriefPrevention.instance.claimsEnabledForWorld(location.getWorld())) return null;

        PlayerData playerData = dataStore.getPlayerData(player.getUniqueId());
        Claim claim = dataStore.getClaimAt(location, false, playerData.lastClaim);

        //exception: administrators in ignore claims mode
        if (playerData.ignoreClaims) return null;

        //wilderness rules
        if (claim == null)
        {
            //no building in the wilderness in creative mode
            if (creativeRulesApply(location) ||
                getPluginConfig().getClaimConfiguration().getWorldMode(location.getWorld()) ==
                ClaimsMode.SurvivalRequiringClaims)
            {
                String reason = dataStore.getMessage(Messages.NoBuildOutsideClaims);
                if (player.hasPermission("griefprevention.ignoreclaims"))
                {
                    reason += "  " + dataStore.getMessage(Messages.IgnoreClaimsAdvertisement);
                }
                reason += "  " + dataStore.getMessage(Messages.CreativeBasicsVideo2, DataStore.CREATIVE_VIDEO_URL);
                return reason;
            }

            //but it's fine in survival mode
            else
            {
                return null;
            }
        }
        else
        {
            //cache the claim for later reference
            playerData.lastClaim = claim;

            //if not in the wilderness, then apply claim rules (permissions, etc)
            Supplier<String> cancel = claim.checkPermission(player, ClaimPermission.Build, breakEvent);
            if (cancel != null && breakEvent != null)
            {
                PreventBlockBreakEvent preventionEvent = new PreventBlockBreakEvent(breakEvent);
                Bukkit.getPluginManager().callEvent(preventionEvent);
                if (preventionEvent.isCancelled())
                {
                    cancel = null;
                }
            }

            if (cancel == null) return null;

            return cancel.get();
        }
    }

    //restores nature in multiple chunks, as described by a claim instance
    //this restores all chunks which have ANY number of claim blocks from this claim in them
    //if the claim is still active (in the data store), then the claimed blocks will not be changed (only the area bordering the claim)
    @Deprecated(forRemoval = true)
    public void restoreClaim(Claim claim, long delayInTicks)
    {
        //admin claims aren't automatically cleaned up when deleted or abandoned
        if (claim.isAdminClaim()) return;

        //it's too expensive to do this for huge claims
        if (claim.getArea() > 10000) return;

        ArrayList<Chunk> chunks = claim.getChunks();
        for (Chunk chunk : chunks)
        {
            restoreChunk(chunk, getSeaLevel(chunk.getWorld()) - 15, false, delayInTicks, null);
        }
    }

    @Deprecated(forRemoval = true)
    public void restoreChunk(Chunk chunk, int miny, boolean aggressiveMode, long delayInTicks, Player playerReceivingVisualization)
    {
        //build a snapshot of this chunk, including 1 block boundary outside the chunk all the way around
        int maxHeight = chunk.getWorld().getMaxHeight();
        BlockSnapshot[][][] snapshots = new BlockSnapshot[18][maxHeight][18];
        Block startBlock = chunk.getBlock(0, 0, 0);
        Location startLocation = new Location(chunk.getWorld(), startBlock.getX() - 1, 0, startBlock.getZ() - 1);
        for (int x = 0; x < snapshots.length; x++)
        {
            for (int z = 0; z < snapshots[0][0].length; z++)
            {
                for (int y = 0; y < snapshots[0].length; y++)
                {
                    Block block = chunk.getWorld().getBlockAt(
                            startLocation.getBlockX() + x,
                            startLocation.getBlockY() + y,
                            startLocation.getBlockZ() + z
                    );
                    snapshots[x][y][z] = new BlockSnapshot(block.getLocation(), block.getType(), block.getBlockData());
                }
            }
        }

        //create task to process those data in another thread
        Location lesserBoundaryCorner = chunk.getBlock(0, 0, 0).getLocation();
        Location greaterBoundaryCorner = chunk.getBlock(15, 0, 15).getLocation();

        //create task
        //when done processing, this task will create a main thread task to actually update the world with processing results
        RestoreNatureProcessingTask task = new RestoreNatureProcessingTask(
                snapshots,
                miny,
                chunk.getWorld().getEnvironment(),
                lesserBoundaryCorner.getBlock().getBiome(),
                lesserBoundaryCorner,
                greaterBoundaryCorner,
                getSeaLevel(chunk.getWorld()),
                aggressiveMode,
                GriefPrevention.instance.creativeRulesApply(lesserBoundaryCorner),
                playerReceivingVisualization
        );
        GriefPrevention.instance.getServer().getScheduler().runTaskLaterAsynchronously(
                GriefPrevention.instance,
                task,
                delayInTicks
        );
    }

    @Deprecated(forRemoval = true)
    public int getSeaLevel(World world)
    {
        Integer overrideValue = config_seaLevelOverride.get(world.getName());
        if (overrideValue == null || overrideValue == -1)
        {
            return world.getSeaLevel();
        }
        else
        {
            return overrideValue;
        }
    }

    @Deprecated(forRemoval = true)
    public boolean pvpRulesApply(World world)
    {
        return getPluginConfig().getPvpConfiguration().getWorlds().contains(world.getName()) ||
               world.getPVP();
    }

    @Deprecated(forRemoval = true)
    public ItemStack getItemInHand(Player player, EquipmentSlot hand)
    {
        if (hand == EquipmentSlot.OFF_HAND) return player.getInventory().getItemInOffHand();
        return player.getInventory().getItemInMainHand();
    }

    @Deprecated(forRemoval = true)
    public boolean claimIsPvPSafeZone(Claim claim)
    {
        if (claim.siegeData != null)
        {
            return false;
        }
        return claim.isAdminClaim() && claim.parent == null &&
               getPluginConfig().getPvpConfiguration().isProtectInAdminClaims() ||
               claim.isAdminClaim() && claim.parent != null &&
               GriefPrevention.instance.getPluginConfig().getPvpConfiguration().isProtectInAdminSubdivisions() ||
               !claim.isAdminClaim() && getPluginConfig().getPvpConfiguration().isProtectInPlayerClaims();
    }

    /*
    protected boolean isPlayerTrappedInPortal(Block block)
	{
		Material playerBlock = block.getType();
		if (playerBlock == Material.PORTAL)
			return true;
		//Most blocks you can "stand" inside but cannot pass through (isSolid) usually can be seen through (!isOccluding)
		//This can cause players to technically be considered not in a portal block, yet in reality is still stuck in the portal animation.
		if ((!playerBlock.isSolid() || playerBlock.isOccluding())) //If it is _not_ such a block,
		{
			//Check the block above
			playerBlock = block.getRelative(BlockFace.UP).getType();
			if ((!playerBlock.isSolid() || playerBlock.isOccluding()))
				return false; //player is not stuck
		}
		//Check if this block is also adjacent to a portal
		return block.getRelative(BlockFace.EAST).getType() == Material.PORTAL
				|| block.getRelative(BlockFace.WEST).getType() == Material.PORTAL
				|| block.getRelative(BlockFace.NORTH).getType() == Material.PORTAL
				|| block.getRelative(BlockFace.SOUTH).getType() == Material.PORTAL;
	}

	public void rescuePlayerTrappedInPortal(final Player player)
	{
		final Location oldLocation = player.getLocation();
		if (!isPlayerTrappedInPortal(oldLocation.getBlock()))
		{
			//Note that he 'escaped' the portal frame
			instance.portalReturnMap.remove(player.getUniqueId());
			instance.portalReturnTaskMap.remove(player.getUniqueId());
			return;
		}

		Location rescueLocation = portalReturnMap.get(player.getUniqueId());

		if (rescueLocation == null)
			return;

		//Temporarily store the old location, in case the player wishes to undo the rescue
		dataStore.getPlayerData(player.getUniqueId()).portalTrappedLocation = oldLocation;

		player.teleport(rescueLocation);
		sendMessage(player, TextMode.Info, Messages.RescuedFromPortalTrap);
		portalReturnMap.remove(player.getUniqueId());

		new BukkitRunnable()
		{
			public void run()
			{
				if (oldLocation == dataStore.getPlayerData(player.getUniqueId()).portalTrappedLocation)
					dataStore.getPlayerData(player.getUniqueId()).portalTrappedLocation = null;
			}
		}.runTaskLater(this, 600L);
	}
	*/

    @Deprecated(forRemoval = true)
    public void startRescueTask(Player player, Location location)
    {
        //Schedule task to reset player's portal cooldown after 30 seconds (Maximum timeout time for client, in case their network is slow and taking forever to load chunks)
        BukkitTask task = new CheckForPortalTrapTask(player, this, location).runTaskLater(
                GriefPrevention.instance,
                600L
        );

        //Cancel existing rescue task
        if (portalReturnTaskMap.containsKey(player.getUniqueId()))
        {
            portalReturnTaskMap.put(player.getUniqueId(), task).cancel();
        }
        else
        {
            portalReturnTaskMap.put(player.getUniqueId(), task);
        }
    }

    @Deprecated(forRemoval = true)
    public EconomyHandler getEconomyHandler()
    {
        return economyHandler;
    }

    @Deprecated(forRemoval = true)
    //thread to build the above cache
    private class CacheOfflinePlayerNamesThread extends Thread
    {
        private final OfflinePlayer[] offlinePlayers;
        private final ConcurrentHashMap<String, UUID> playerNameToIDMap;

        CacheOfflinePlayerNamesThread(OfflinePlayer[] offlinePlayers, ConcurrentHashMap<String, UUID> playerNameToIDMap)
        {
            this.offlinePlayers = offlinePlayers;
            this.playerNameToIDMap = playerNameToIDMap;
        }

        public void run()
        {
            long now = System.currentTimeMillis();
            final long millisecondsPerDay = 1000 * 60 * 60 * 24;
            for (OfflinePlayer player : offlinePlayers)
            {
                try
                {
                    UUID playerID = player.getUniqueId();
                    long lastSeen = player.getLastLogin();

                    //if the player has been seen in the last 90 days, cache his name/UUID pair
                    long diff = now - lastSeen;
                    long daysDiff = diff / millisecondsPerDay;
                    if (daysDiff <= config_advanced_offlineplayer_cache_days)
                    {
                        String playerName = player.getName();
                        if (playerName == null) continue;
                        playerNameToIDMap.put(playerName, playerID);
                        playerNameToIDMap.put(playerName.toLowerCase(), playerID);
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }
    }
}
