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
import me.tinyoverflow.griefprevention.commands.*;
import me.tinyoverflow.griefprevention.configurations.GriefPreventionConfiguration;
import me.tinyoverflow.griefprevention.datastore.DataStore;
import me.tinyoverflow.griefprevention.datastore.DatabaseDataStore;
import me.tinyoverflow.griefprevention.datastore.FlatFileDataStore;
import me.tinyoverflow.griefprevention.events.PreventBlockBreakEvent;
import me.tinyoverflow.griefprevention.handlers.BlockEventHandler;
import me.tinyoverflow.griefprevention.handlers.EconomyHandler;
import me.tinyoverflow.griefprevention.handlers.EntityDamageHandler;
import me.tinyoverflow.griefprevention.handlers.EntityEventHandler;
import me.tinyoverflow.griefprevention.listeners.claim.ClaimPermissionCheckListener;
import me.tinyoverflow.griefprevention.listeners.player.*;
import me.tinyoverflow.griefprevention.tasks.*;
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
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Logger;

public class GriefPrevention extends JavaPlugin {
    //for convenience, a reference to the instance of this plugin
    public static GriefPrevention instance;
    //for logging to the console and log file
    private static Logger log;
    private final Path configurationFile;
    //this handles data storage, like player and region data
    public DataStore dataStore;
    // Event handlers with common functionality
    public EntityEventHandler entityEventHandler;
    public EntityDamageHandler entityDamageHandler;
    //this tracks item stacks expected to drop which will need protection
    public ArrayList<PendingItemProtection> pendingItemWatchList = new ArrayList<>();
    //claim mode for each world
    public ConcurrentHashMap<World, ClaimsMode> config_claims_worldModes;
    public boolean config_pvp_protectFreshSpawns;                    //whether to make newly spawned players immune until they pick up an item
    public boolean config_pvp_punishLogout;                            //whether to kill players who log out during PvP combat
    public int config_pvp_combatTimeoutSeconds;                        //how long combat is considered to continue after the most recent damage
    public boolean config_pvp_allowCombatItemDrop;                    //whether a player can drop items during combat to hide them
    public ArrayList<String> config_pvp_blockedCommands;            //list of commands which may not be used during pvp combat
    public boolean config_pvp_noCombatInPlayerLandClaims;            //whether players may fight in player-owned land claims
    public boolean config_pvp_noCombatInAdminLandClaims;            //whether players may fight in admin-owned land claims
    public boolean config_pvp_noCombatInAdminSubdivisions;          //whether players may fight in subdivisions of admin-owned land claims
    public boolean config_pvp_allowLavaNearPlayers;                 //whether players may dump lava near other players in pvp worlds
    public boolean config_pvp_allowLavaNearPlayers_NonPvp;            //whather this applies in non-PVP rules worlds <ArchdukeLiamus>
    public boolean config_pvp_allowFireNearPlayers;                 //whether players may start flint/steel fires near other players in pvp worlds
    public boolean config_pvp_allowFireNearPlayers_NonPvp;            //whether this applies in non-PVP rules worlds <ArchdukeLiamus>
    public boolean config_pvp_protectPets;                          //whether players may damage pets outside of land claims in pvp worlds
    public boolean config_lockDeathDropsInPvpWorlds;                //whether players' dropped on death items are protected in pvp worlds
    public boolean config_lockDeathDropsInNonPvpWorlds;             //whether players' dropped on death items are protected in non-pvp worlds
    public int config_economy_claimBlocksMaxBonus;                  //max "bonus" blocks a player can buy.  set to zero for no limit.
    public double config_economy_claimBlocksPurchaseCost;            //cost to purchase a claim block.  set to zero to disable purchase.
    public double config_economy_claimBlocksSellValue;                //return on a sold claim block.  set to zero to disable sale.
    public boolean config_blockClaimExplosions;                     //whether explosions may destroy claimed blocks
    public boolean config_blockSurfaceCreeperExplosions;            //whether creeper explosions near or above the surface destroy blocks
    public boolean config_blockSurfaceOtherExplosions;                //whether non-creeper explosions near or above the surface destroy blocks
    public boolean config_blockSkyTrees;                            //whether players can build trees on platforms in the sky
    public boolean config_fireSpreads;                                //whether fire spreads outside of claims
    public boolean config_fireDestroys;                                //whether fire destroys blocks outside of claims
    public boolean config_whisperNotifications;                    //whether whispered messages will broadcast to administrators in game
    public boolean config_signNotifications;                        //whether sign content will broadcast to administrators in game
    public boolean config_visualizationAntiCheatCompat;              // whether to engage compatibility mode for anti-cheat plugins
    public boolean config_endermenMoveBlocks;                        //whether endermen may move blocks around
    public boolean config_claims_ravagersBreakBlocks;                //whether ravagers may break blocks in claims
    public boolean config_silverfishBreakBlocks;                    //whether silverfish may break blocks
    public boolean config_creaturesTrampleCrops;                    //whether non-player entities may trample crops
    public boolean config_rabbitsEatCrops;                          //whether rabbits may eat crops
    public boolean config_zombiesBreakDoors;                        //whether hard-mode zombies may break down wooden doors
    public HashMap<String, Integer> config_seaLevelOverride;        //override for sea level, because bukkit doesn't report the right value for all situations
    public boolean config_limitTreeGrowth;                          //whether trees should be prevented from growing into a claim from outside
    public PistonMode config_pistonMovement;                            //Setting for piston check options
    public boolean config_pistonExplosionSound;                     //whether pistons make an explosion sound when they get removed
    public boolean config_advanced_fixNegativeClaimblockAmounts;    //whether to attempt to fix negative claim block amounts (some addons cause/assume players can go into negative amounts)
    public int config_advanced_claim_expiration_check_rate;            //How often GP should check for expired claims, amount in seconds
    public int config_advanced_offlineplayer_cache_days;            //Cache players who have logged in within the last x number of days
    //custom log settings
    public int config_logs_daysToKeep;
    public boolean config_logs_socialEnabled;
    public boolean config_logs_suspiciousEnabled;
    public boolean config_logs_adminEnabled;
    public boolean config_logs_debugEnabled;
    public boolean config_logs_mutedChatEnabled;
    //Track scheduled "rescues" so we can cancel them if the player happens to teleport elsewhere, so we can cancel it.
    public ConcurrentHashMap<UUID, BukkitTask> portalReturnTaskMap = new ConcurrentHashMap<>();
    //log entry manager for GP's custom log files
    CustomLogger customLogger;
    // Player event handler
    HashMap<World, Boolean> config_pvp_specifiedWorlds;                //list of worlds where pvp anti-grief rules apply, according to the config file
    //helper method to resolve a player by name
    ConcurrentHashMap<String, UUID> playerNameToIDMap = new ConcurrentHashMap<>();
    private HoconConfigurationLoader configurationLoader;
    private ConfigurationNode configurationNode;
    private GriefPreventionConfiguration configuration;
    private EconomyHandler economyHandler;
    private String databaseUrl;
    private String databaseUserName;
    private String databasePassword;

    public GriefPrevention() {
        configurationFile = Paths.get(
                this.getDataFolder().getPath(),
                "config.conf"
        );
    }

    //adds a server log entry
    public static synchronized void AddLogEntry(String entry, CustomLogEntryTypes customLogType, boolean excludeFromServerLogs) {
        if (customLogType != null && GriefPrevention.instance.customLogger != null) {
            GriefPrevention.instance.customLogger.AddEntry(entry, customLogType);
        }
        if (!excludeFromServerLogs) log.info(entry);
    }

    public static synchronized void AddLogEntry(String entry, CustomLogEntryTypes customLogType) {
        AddLogEntry(entry, customLogType, false);
    }

    public static synchronized void AddLogEntry(String entry) {
        AddLogEntry(entry, CustomLogEntryTypes.Debug);
    }

    public static String getFriendlyLocationString(Location location) {
        return location.getWorld().getName() + ": x" + location.getBlockX() + ", z" + location.getBlockZ();
    }

    //helper method to resolve a player name from the player's UUID
    public static @NotNull String lookupPlayerName(@Nullable UUID playerID) {
        //parameter validation
        if (playerID == null) return "someone";

        //check the cache
        OfflinePlayer player = GriefPrevention.instance.getServer().getOfflinePlayer(playerID);
        return lookupPlayerName(player);
    }

    static @NotNull String lookupPlayerName(@NotNull AnimalTamer tamer) {
        // If the tamer is not a player or has played, prefer their name if it exists.
        if (!(tamer instanceof OfflinePlayer player) || player.hasPlayedBefore() || player.isOnline()) {
            String name = tamer.getName();
            if (name != null) return name;
        }

        // Fall back to tamer's UUID.
        return "someone(" + tamer.getUniqueId() + ")";
    }

    //cache for player name lookups, to save searches of all offline players
    public static void cacheUUIDNamePair(UUID playerID, String playerName) {
        //store the reverse mapping
        GriefPrevention.instance.playerNameToIDMap.put(playerName, playerID);
        GriefPrevention.instance.playerNameToIDMap.put(playerName.toLowerCase(), playerID);
    }

    public static boolean isInventoryEmpty(Player player) {
        PlayerInventory inventory = player.getInventory();
        ItemStack[] armorStacks = inventory.getArmorContents();

        //check armor slots, stop if any items are found
        for (ItemStack armorStack : armorStacks) {
            if (!(armorStack == null || armorStack.getType() == Material.AIR)) return false;
        }

        //check other slots, stop if any items are found
        ItemStack[] generalStacks = inventory.getContents();
        for (ItemStack generalStack : generalStacks) {
            if (!(generalStack == null || generalStack.getType() == Material.AIR)) return false;
        }

        return true;
    }

    //ensures a piece of the managed world is loaded into server memory
    //(generates the chunk if necessary)
    private static void GuaranteeChunkLoaded(Location location) {
        Chunk chunk = location.getChunk();
        while (!chunk.isLoaded() || !chunk.load(true)) ;
    }

    //sends a color-coded message to a player
    public static void sendMessage(Player player, ChatColor color, Messages messageID, String... args) {
        sendMessage(player, color, messageID, 0, args);
    }

    //sends a color-coded message to a player
    public static void sendMessage(Player player, ChatColor color, Messages messageID, long delayInTicks, String... args) {
        String message = GriefPrevention.instance.dataStore.getMessage(messageID, args);
        sendMessage(player, color, message, delayInTicks);
    }

    //sends a color-coded message to a player
    public static void sendMessage(Player player, ChatColor color, String message) {
        if (message == null || message.length() == 0) return;

        if (player == null) {
            GriefPrevention.AddLogEntry(color + message);
        }
        else {
            player.sendMessage(color + message);
        }
    }

    public static void sendMessage(Player player, ChatColor color, String message, long delayInTicks) {
        SendPlayerMessageTask task = new SendPlayerMessageTask(player, color, message);

        //Only schedule if there should be a delay. Otherwise, send the message right now, else the message will appear out of order.
        if (delayInTicks > 0) {
            GriefPrevention.instance.getServer().getScheduler().runTaskLater(
                    GriefPrevention.instance,
                    task,
                    delayInTicks
            );
        }
        else {
            task.run();
        }
    }

    public static boolean isNewToServer(Player player) {
        if (player.getStatistic(Statistic.PICKUP, Material.OAK_LOG) > 0 ||
                player.getStatistic(Statistic.PICKUP, Material.SPRUCE_LOG) > 0 ||
                player.getStatistic(Statistic.PICKUP, Material.BIRCH_LOG) > 0 ||
                player.getStatistic(Statistic.PICKUP, Material.JUNGLE_LOG) > 0 ||
                player.getStatistic(Statistic.PICKUP, Material.ACACIA_LOG) > 0 ||
                player.getStatistic(Statistic.PICKUP, Material.DARK_OAK_LOG) > 0)
            return false;

        PlayerData playerData = instance.dataStore.getPlayerData(player.getUniqueId());

        return playerData.getClaims().size() == 0;
    }

    public void onLoad() {
        // Load the plugin config first, so we have everything ready
        // for all dependencies that might come up.
        this.loadPluginConfig();

        // As CommandAPI is loaded as a library and not as a plugin,
        // we need to trigger this event manually.
        CommandAPI.onLoad(new CommandAPIBukkitConfig(this));
    }

    public void onEnable() {
        // As CommandAPI is loaded as a library and not as a plugin,
        // we need to trigger this event manually.
        CommandAPI.onEnable();

        instance = this;
        log = instance.getLogger();

        this.registerCommands();

        this.customLogger = new CustomLogger();

        AddLogEntry("Finished loading configuration.");

        //when datastore initializes, it loads player and claim data, and posts some stats to the log
        if (this.databaseUrl.length() > 0) {
            try {
                DatabaseDataStore databaseStore = new DatabaseDataStore(
                        this.databaseUrl,
                        this.databaseUserName,
                        this.databasePassword
                );

                if (FlatFileDataStore.hasData()) {
                    GriefPrevention.AddLogEntry(
                            "There appears to be some data on the hard drive.  Migrating those data to the database...");
                    FlatFileDataStore flatFileStore = new FlatFileDataStore(this.getDataFolder());
                    this.dataStore = flatFileStore;
                    flatFileStore.migrateData(databaseStore);
                    GriefPrevention.AddLogEntry("Data migration process complete.");
                }

                this.dataStore = databaseStore;
            } catch (Exception e) {
                GriefPrevention.AddLogEntry(
                        "Because there was a problem with the database, GriefPrevention will not function properly.  Either update the database config settings resolve the issue, or delete those lines from your config.yml so that GriefPrevention can use the file system to store data.");
                e.printStackTrace();
                this.getServer().getPluginManager().disablePlugin(this);
                return;
            }
        }

        //if not using the database because it's not configured or because there was a problem, use the file system to store data
        //this is the preferred method, as it's simpler than the database scenario
        if (this.dataStore == null) {
            File oldclaimdata = new File(getDataFolder(), "ClaimData");
            if (oldclaimdata.exists()) {
                if (!FlatFileDataStore.hasData()) {
                    File claimdata = new File("plugins" + File.separator + "GriefPrevention" + File.separator + "ClaimData");
                    oldclaimdata.renameTo(claimdata);
                    File oldplayerdata = new File(getDataFolder(), "PlayerData");
                    File playerdata = new File("plugins" + File.separator + "GriefPrevention" + File.separator + "PlayerData");
                    oldplayerdata.renameTo(playerdata);
                }
            }
            try {
                this.dataStore = new FlatFileDataStore(this.getDataFolder());
            } catch (Exception e) {
                GriefPrevention.AddLogEntry("Unable to initialize the file system data store.  Details:");
                GriefPrevention.AddLogEntry(e.getMessage());
                e.printStackTrace();
            }
        }

        String dataMode = (this.dataStore instanceof FlatFileDataStore) ? "(File Mode)" : "(Database Mode)";
        AddLogEntry("Finished loading data " + dataMode + ".");

        //unless claim block accrual is disabled, start the recurring per 10-minute event to give claim blocks to online players
        //20L ~ 1 second
        if (getPluginConfig().getClaimConfiguration().getClaimBlocksConfiguration().accrued.isAccrualEnabled()) {
            DeliverClaimBlocksTask task = new DeliverClaimBlocksTask(null, this);
            getServer().getScheduler().scheduleSyncRepeatingTask(this, task, 20L * 60 * 10, 20L * 60 * 10);
        }

        //start the recurring cleanup event for entities in creative worlds
        EntityCleanupTask task = new EntityCleanupTask(0);
        this.getServer().getScheduler().scheduleSyncDelayedTask(GriefPrevention.instance, task, 20L * 60 * 2);

        //start recurring cleanup scan for unused claims belonging to inactive players
        FindUnusedClaimsTask task2 = new FindUnusedClaimsTask();
        this.getServer().getScheduler().scheduleSyncRepeatingTask(
                this,
                task2,
                20L * 60,
                20L * config_advanced_claim_expiration_check_rate
        );

        //register for events
        PluginManager pluginManager = this.getServer().getPluginManager();

        //player events
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

        //block events
        BlockEventHandler blockEventHandler = new BlockEventHandler(this.dataStore);
        pluginManager.registerEvents(blockEventHandler, this);

        //entity events
        entityEventHandler = new EntityEventHandler(this.dataStore, this);
        pluginManager.registerEvents(entityEventHandler, this);

        //combat/damage-specific entity events
        entityDamageHandler = new EntityDamageHandler(this.dataStore, this);
        pluginManager.registerEvents(entityDamageHandler, this);

        //siege events
        ClaimPermissionCheckListener siegeEventHandler = new ClaimPermissionCheckListener();
        pluginManager.registerEvents(siegeEventHandler, this);

        //vault-based economy integration
        economyHandler = new EconomyHandler(this);
        pluginManager.registerEvents(economyHandler, this);

        //cache offline players
        OfflinePlayer[] offlinePlayers = this.getServer().getOfflinePlayers();
        CacheOfflinePlayerNamesThread namesThread = new CacheOfflinePlayerNamesThread(
                offlinePlayers,
                this.playerNameToIDMap
        );
        namesThread.setPriority(Thread.MIN_PRIORITY);
        namesThread.start();

        AddLogEntry("Boot finished.");
    }

    private void registerCommands() {
        CommandManager commandManager = new CommandManager();
        commandManager.add(new AbandonAllClaimsCommand("abandonallclaims", this));
        commandManager.add(new AdjustBonusClaimBlocksAllCommand("adjustbonusclaimblocksall", this));
        commandManager.add(new AdjustBonusClaimBlocksCommand("adjustbonusclaimblocks", this));
        commandManager.add(new AdjustClaimBlockLimitCommand("adjustclaimblocklimit", this));
        commandManager.add(new AdminClaimListCommand("adminclaimlist", this));
        commandManager.add(new AdminClaimsCommand("adminclaims", this));
        commandManager.add(new BasicClaimsCommand("basicclaims", this));
        commandManager.add(new BuyClaimBlocksCommand("buyclaimblocks", this));
        commandManager.add(new ClaimAbandonCommand("abandonclaim", this));
        commandManager.add(new ClaimBookCommand("claimbook", this));
        commandManager.add(new ClaimCommand("claim", this));
        commandManager.add(new ClaimExplosionsCommand("claimexplosions", this));
        commandManager.add(new ClaimExtendCommand("extendclaim", this));
        commandManager.add(new ClaimsListCommand("claimslist", this));
        commandManager.add(new DeleteAllAdminClaimsCommand("deleteuserclaimsinworld", this));
        commandManager.add(new DeleteAllClaimsCommand("deleteallclaims", this));
        commandManager.add(new DeleteClaimCommand("deleteclaim", this));
        commandManager.add(new DeleteClaimsInWorldCommand("deleteclaimsinworld", this));
        commandManager.add(new DeleteUserClaimsInWorldCommand("deleteuserclaimsinworld", this));
        commandManager.add(new GivePetCommand("givepet", this));
        commandManager.add(new IgnoreClaimsCommand("ignoreclaims", this));
        commandManager.add(new RestoreNatureCommand("restorenature", this));
        commandManager.add(new RestoreNatureFillCommand("restorenaturefill", this));
        commandManager.add(new RestrictSubClaimCommand("restrictsubclaim", this));
        commandManager.add(new SellClaimBlocksCommand("sellclaimblocks", this));
        commandManager.add(new SetAccruedClaimBlocksCommand("setaccruedclaimblocks", this));
        commandManager.add(new SiegeCommand("siege", this));
        commandManager.add(new SubdivideClaimsCommand("subdivideclaims", this));
        commandManager.add(new TransferClaimCommand("transferclaim", this));
        commandManager.add(new TrappedCommand("trapped", this));
        commandManager.add(new TrustCommand("trust", this));
        commandManager.add(new TrustListCommand("trustlist", this));
        commandManager.add(new UnlockItemsCommand("unlockitems", this));
        commandManager.add(new UntrustCommand("untrust", this));
        commandManager.register();
    }

    private void registerEvents(Listener... listeners) {
        for (Listener listener : listeners) {
            getServer().getPluginManager().registerEvents(listener, this);
        }
    }

    private void savePluginConfig() {
        try {
            this.configurationNode.set(this.configuration);
            this.configurationLoader.save(this.configurationNode);
        } catch (ConfigurateException e) {
            throw new RuntimeException(e);
        }
    }

    public GriefPreventionConfiguration getPluginConfig() {
        return this.configuration;
    }

    private void loadPluginConfig() {
        try {
            configurationLoader = HoconConfigurationLoader.builder()
                    .path(configurationFile)
                    .build();

            configurationNode = configurationLoader.load();
            configuration = configurationNode.get(GriefPreventionConfiguration.class);

            // Always saving the file on load to make sure that it exists
            // and is always up-to-date.
            savePluginConfig();
        } catch (ConfigurateException e) {
            throw new RuntimeException(e);
        }


        // TODO: Remove legacy code once all references have been updated.
        //load the config if it exists
        FileConfiguration config = YamlConfiguration.loadConfiguration(new File(DataStore.configFilePath));
        FileConfiguration outConfig = new YamlConfiguration();

        //get (deprecated node) claims world names from the config file
        List<World> worlds = this.getServer().getWorlds();
        List<String> deprecated_claimsEnabledWorldNames = config.getStringList("GriefPrevention.Claims.Worlds");

        //validate that list
        for (int i = 0; i < deprecated_claimsEnabledWorldNames.size(); i++) {
            String worldName = deprecated_claimsEnabledWorldNames.get(i);
            World world = this.getServer().getWorld(worldName);
            if (world == null) {
                deprecated_claimsEnabledWorldNames.remove(i--);
            }
        }

        //get (deprecated node) creative world names from the config file
        List<String> deprecated_creativeClaimsEnabledWorldNames = config.getStringList(
                "GriefPrevention.Claims.CreativeRulesWorlds");

        //validate that list
        for (int i = 0; i < deprecated_creativeClaimsEnabledWorldNames.size(); i++) {
            String worldName = deprecated_creativeClaimsEnabledWorldNames.get(i);
            World world = this.getServer().getWorld(worldName);
            if (world == null) {
                deprecated_claimsEnabledWorldNames.remove(i--);
            }
        }

        //get (deprecated) pvp fire placement proximity note and use it if it exists (in the new config format it will be overwritten later).
        config_pvp_allowFireNearPlayers = config.getBoolean(
                "GriefPrevention.PvP.AllowFlintAndSteelNearOtherPlayers",
                false
        );
        //get (deprecated) pvp lava dump proximity note and use it if it exists (in the new config format it will be overwritten later).
        config_pvp_allowLavaNearPlayers = config.getBoolean(
                "GriefPrevention.PvP.AllowLavaDumpingNearOtherPlayers",
                false
        );

        //pvp worlds list
        this.config_pvp_specifiedWorlds = new HashMap<>();
        for (World world : worlds) {
            boolean pvpWorld = config.getBoolean(
                    "GriefPrevention.PvP.RulesEnabledInWorld." + world.getName(),
                    world.getPVP()
            );
            this.config_pvp_specifiedWorlds.put(world, pvpWorld);
        }

        //sea level
        this.config_seaLevelOverride = new HashMap<>();
        for (World world : worlds) {
            int seaLevelOverride = config.getInt("GriefPrevention.SeaLevelOverrides." + world.getName(), -1);
            outConfig.set("GriefPrevention.SeaLevelOverrides." + world.getName(), seaLevelOverride);
            this.config_seaLevelOverride.put(world.getName(), seaLevelOverride);
        }

        this.config_pvp_protectFreshSpawns = config.getBoolean("GriefPrevention.PvP.ProtectFreshSpawns", true);
        this.config_pvp_punishLogout = config.getBoolean("GriefPrevention.PvP.PunishLogout", true);
        this.config_pvp_combatTimeoutSeconds = config.getInt("GriefPrevention.PvP.CombatTimeoutSeconds", 15);
        this.config_pvp_allowCombatItemDrop = config.getBoolean("GriefPrevention.PvP.AllowCombatItemDrop", false);
        String bannedPvPCommandsList = config.getString(
                "GriefPrevention.PvP.BlockedSlashCommands",
                "/home;/vanish;/spawn;/tpa"
        );

        this.config_economy_claimBlocksMaxBonus = config.getInt("GriefPrevention.Economy.ClaimBlocksMaxBonus", 0);
        this.config_economy_claimBlocksPurchaseCost = config.getDouble(
                "GriefPrevention.Economy.ClaimBlocksPurchaseCost",
                0
        );
        this.config_economy_claimBlocksSellValue = config.getDouble("GriefPrevention.Economy.ClaimBlocksSellValue", 0);

        this.config_lockDeathDropsInPvpWorlds = config.getBoolean(
                "GriefPrevention.ProtectItemsDroppedOnDeath.PvPWorlds",
                false
        );
        this.config_lockDeathDropsInNonPvpWorlds = config.getBoolean(
                "GriefPrevention.ProtectItemsDroppedOnDeath.NonPvPWorlds",
                true
        );

        this.config_blockClaimExplosions = config.getBoolean("GriefPrevention.BlockLandClaimExplosions", true);
        this.config_blockSurfaceCreeperExplosions = config.getBoolean(
                "GriefPrevention.BlockSurfaceCreeperExplosions",
                true
        );
        this.config_blockSurfaceOtherExplosions = config.getBoolean(
                "GriefPrevention.BlockSurfaceOtherExplosions",
                true
        );
        this.config_blockSkyTrees = config.getBoolean("GriefPrevention.LimitSkyTrees", true);
        this.config_limitTreeGrowth = config.getBoolean("GriefPrevention.LimitTreeGrowth", false);
        this.config_pistonExplosionSound = config.getBoolean("GriefPrevention.PistonExplosionSound", true);
        this.config_pistonMovement = PistonMode.of(config.getString("GriefPrevention.PistonMovement", "CLAIMS_ONLY"));
        if (config.isBoolean("GriefPrevention.LimitPistonsToLandClaims") && !config.getBoolean(
                "GriefPrevention.LimitPistonsToLandClaims"))
            this.config_pistonMovement = PistonMode.EVERYWHERE_SIMPLE;
        if (config.isBoolean("GriefPrevention.CheckPistonMovement") && !config.getBoolean(
                "GriefPrevention.CheckPistonMovement"))
            this.config_pistonMovement = PistonMode.IGNORED;

        this.config_fireSpreads = config.getBoolean("GriefPrevention.FireSpreads", false);
        this.config_fireDestroys = config.getBoolean("GriefPrevention.FireDestroys", false);

        this.config_visualizationAntiCheatCompat = config.getBoolean(
                "GriefPrevention.VisualizationAntiCheatCompatMode",
                false
        );

        this.config_endermenMoveBlocks = config.getBoolean("GriefPrevention.EndermenMoveBlocks", false);
        this.config_silverfishBreakBlocks = config.getBoolean("GriefPrevention.SilverfishBreakBlocks", false);
        this.config_creaturesTrampleCrops = config.getBoolean("GriefPrevention.CreaturesTrampleCrops", false);
        this.config_rabbitsEatCrops = config.getBoolean("GriefPrevention.RabbitsEatCrops", true);
        this.config_zombiesBreakDoors = config.getBoolean("GriefPrevention.HardModeZombiesBreakDoors", false);

        this.config_pvp_noCombatInPlayerLandClaims = config.getBoolean(
                "GriefPrevention.PvP.ProtectPlayersInLandClaims.PlayerOwnedClaims",
                true
        );
        this.config_pvp_noCombatInAdminLandClaims = config.getBoolean(
                "GriefPrevention.PvP.ProtectPlayersInLandClaims.AdministrativeClaims",
                true
        );
        this.config_pvp_noCombatInAdminSubdivisions = config.getBoolean(
                "GriefPrevention.PvP.ProtectPlayersInLandClaims.AdministrativeSubdivisions",
                true
        );
        this.config_pvp_allowLavaNearPlayers = config.getBoolean(
                "GriefPrevention.PvP.AllowLavaDumpingNearOtherPlayers.PvPWorlds",
                true
        );
        this.config_pvp_allowLavaNearPlayers_NonPvp = config.getBoolean(
                "GriefPrevention.PvP.AllowLavaDumpingNearOtherPlayers.NonPvPWorlds",
                false
        );
        this.config_pvp_allowFireNearPlayers = config.getBoolean(
                "GriefPrevention.PvP.AllowFlintAndSteelNearOtherPlayers.PvPWorlds",
                true
        );
        this.config_pvp_allowFireNearPlayers_NonPvp = config.getBoolean(
                "GriefPrevention.PvP.AllowFlintAndSteelNearOtherPlayers.NonPvPWorlds",
                false
        );
        this.config_pvp_protectPets = config.getBoolean("GriefPrevention.PvP.ProtectPetsOutsideLandClaims", false);

        //optional database settings
        this.databaseUrl = config.getString("GriefPrevention.Database.URL", "");
        this.databaseUserName = config.getString("GriefPrevention.Database.UserName", "");
        this.databasePassword = config.getString("GriefPrevention.Database.Password", "");

        this.config_advanced_fixNegativeClaimblockAmounts = config.getBoolean(
                "GriefPrevention.Advanced.fixNegativeClaimblockAmounts",
                true
        );
        this.config_advanced_claim_expiration_check_rate = config.getInt(
                "GriefPrevention.Advanced.ClaimExpirationCheckRate",
                60
        );
        this.config_advanced_offlineplayer_cache_days = config.getInt(
                "GriefPrevention.Advanced.OfflinePlayer_cache_days",
                90
        );

        //custom logger settings
        this.config_logs_daysToKeep = config.getInt("GriefPrevention.Abridged Logs.Days To Keep", 7);
        this.config_logs_socialEnabled = config.getBoolean(
                "GriefPrevention.Abridged Logs.Included Entry Types.Social Activity",
                true
        );
        this.config_logs_suspiciousEnabled = config.getBoolean(
                "GriefPrevention.Abridged Logs.Included Entry Types.Suspicious Activity",
                true
        );
        this.config_logs_adminEnabled = config.getBoolean(
                "GriefPrevention.Abridged Logs.Included Entry Types.Administrative Activity",
                false
        );
        this.config_logs_debugEnabled = config.getBoolean(
                "GriefPrevention.Abridged Logs.Included Entry Types.Debug",
                false
        );
        this.config_logs_mutedChatEnabled = config.getBoolean(
                "GriefPrevention.Abridged Logs.Included Entry Types.Muted Chat Messages",
                false
        );

        for (World world : worlds) {
            outConfig.set("GriefPrevention.PvP.RulesEnabledInWorld." + world.getName(), this.pvpRulesApply(world));
        }
        outConfig.set("GriefPrevention.PvP.ProtectFreshSpawns", this.config_pvp_protectFreshSpawns);
        outConfig.set("GriefPrevention.PvP.PunishLogout", this.config_pvp_punishLogout);
        outConfig.set("GriefPrevention.PvP.CombatTimeoutSeconds", this.config_pvp_combatTimeoutSeconds);
        outConfig.set("GriefPrevention.PvP.AllowCombatItemDrop", this.config_pvp_allowCombatItemDrop);
        outConfig.set("GriefPrevention.PvP.BlockedSlashCommands", bannedPvPCommandsList);
        outConfig.set(
                "GriefPrevention.PvP.ProtectPlayersInLandClaims.PlayerOwnedClaims",
                this.config_pvp_noCombatInPlayerLandClaims
        );
        outConfig.set(
                "GriefPrevention.PvP.ProtectPlayersInLandClaims.AdministrativeClaims",
                this.config_pvp_noCombatInAdminLandClaims
        );
        outConfig.set(
                "GriefPrevention.PvP.ProtectPlayersInLandClaims.AdministrativeSubdivisions",
                this.config_pvp_noCombatInAdminSubdivisions
        );
        outConfig.set(
                "GriefPrevention.PvP.AllowLavaDumpingNearOtherPlayers.PvPWorlds",
                this.config_pvp_allowLavaNearPlayers
        );
        outConfig.set(
                "GriefPrevention.PvP.AllowLavaDumpingNearOtherPlayers.NonPvPWorlds",
                this.config_pvp_allowLavaNearPlayers_NonPvp
        );
        outConfig.set(
                "GriefPrevention.PvP.AllowFlintAndSteelNearOtherPlayers.PvPWorlds",
                this.config_pvp_allowFireNearPlayers
        );
        outConfig.set(
                "GriefPrevention.PvP.AllowFlintAndSteelNearOtherPlayers.NonPvPWorlds",
                this.config_pvp_allowFireNearPlayers_NonPvp
        );
        outConfig.set("GriefPrevention.PvP.ProtectPetsOutsideLandClaims", this.config_pvp_protectPets);

        outConfig.set("GriefPrevention.Economy.ClaimBlocksMaxBonus", this.config_economy_claimBlocksMaxBonus);
        outConfig.set("GriefPrevention.Economy.ClaimBlocksPurchaseCost", this.config_economy_claimBlocksPurchaseCost);
        outConfig.set("GriefPrevention.Economy.ClaimBlocksSellValue", this.config_economy_claimBlocksSellValue);

        outConfig.set("GriefPrevention.ProtectItemsDroppedOnDeath.PvPWorlds", this.config_lockDeathDropsInPvpWorlds);
        outConfig.set(
                "GriefPrevention.ProtectItemsDroppedOnDeath.NonPvPWorlds",
                this.config_lockDeathDropsInNonPvpWorlds
        );

        outConfig.set("GriefPrevention.BlockLandClaimExplosions", this.config_blockClaimExplosions);
        outConfig.set("GriefPrevention.BlockSurfaceCreeperExplosions", this.config_blockSurfaceCreeperExplosions);
        outConfig.set("GriefPrevention.BlockSurfaceOtherExplosions", this.config_blockSurfaceOtherExplosions);
        outConfig.set("GriefPrevention.LimitSkyTrees", this.config_blockSkyTrees);
        outConfig.set("GriefPrevention.LimitTreeGrowth", this.config_limitTreeGrowth);
        outConfig.set("GriefPrevention.PistonMovement", this.config_pistonMovement.name());
        outConfig.set("GriefPrevention.CheckPistonMovement", null);
        outConfig.set("GriefPrevention.LimitPistonsToLandClaims", null);
        outConfig.set("GriefPrevention.PistonExplosionSound", this.config_pistonExplosionSound);

        outConfig.set("GriefPrevention.FireSpreads", this.config_fireSpreads);
        outConfig.set("GriefPrevention.FireDestroys", this.config_fireDestroys);

        outConfig.set("GriefPrevention.AdminsGetWhispers", this.config_whisperNotifications);
        outConfig.set("GriefPrevention.AdminsGetSignNotifications", this.config_signNotifications);

        outConfig.set("GriefPrevention.VisualizationAntiCheatCompatMode", this.config_visualizationAntiCheatCompat);

        outConfig.set("GriefPrevention.EndermenMoveBlocks", this.config_endermenMoveBlocks);
        outConfig.set("GriefPrevention.SilverfishBreakBlocks", this.config_silverfishBreakBlocks);
        outConfig.set("GriefPrevention.CreaturesTrampleCrops", this.config_creaturesTrampleCrops);
        outConfig.set("GriefPrevention.RabbitsEatCrops", this.config_rabbitsEatCrops);
        outConfig.set("GriefPrevention.HardModeZombiesBreakDoors", this.config_zombiesBreakDoors);

        outConfig.set("GriefPrevention.Database.URL", this.databaseUrl);
        outConfig.set("GriefPrevention.Database.UserName", this.databaseUserName);
        outConfig.set("GriefPrevention.Database.Password", this.databasePassword);

        outConfig.set(
                "GriefPrevention.Advanced.fixNegativeClaimblockAmounts",
                this.config_advanced_fixNegativeClaimblockAmounts
        );
        outConfig.set(
                "GriefPrevention.Advanced.ClaimExpirationCheckRate",
                this.config_advanced_claim_expiration_check_rate
        );
        outConfig.set(
                "GriefPrevention.Advanced.OfflinePlayer_cache_days",
                this.config_advanced_offlineplayer_cache_days
        );

        //custom logger settings
        outConfig.set("GriefPrevention.Abridged Logs.Days To Keep", this.config_logs_daysToKeep);
        outConfig.set(
                "GriefPrevention.Abridged Logs.Included Entry Types.Social Activity",
                this.config_logs_socialEnabled
        );
        outConfig.set(
                "GriefPrevention.Abridged Logs.Included Entry Types.Suspicious Activity",
                this.config_logs_suspiciousEnabled
        );
        outConfig.set(
                "GriefPrevention.Abridged Logs.Included Entry Types.Administrative Activity",
                this.config_logs_adminEnabled
        );
        outConfig.set("GriefPrevention.Abridged Logs.Included Entry Types.Debug", this.config_logs_debugEnabled);
        outConfig.set(
                "GriefPrevention.Abridged Logs.Included Entry Types.Muted Chat Messages",
                this.config_logs_mutedChatEnabled
        );
        outConfig.set("GriefPrevention.ConfigVersion", 1);
    }

    public DataStore getDataStore() {
        return this.dataStore;
    }

    public void onDisable() {
        //save data for any online players
        @SuppressWarnings("unchecked")
        Collection<Player> players = (Collection<Player>) this.getServer().getOnlinePlayers();
        for (Player player : players) {
            UUID playerID = player.getUniqueId();
            PlayerData playerData = this.dataStore.getPlayerData(playerID);
            this.dataStore.savePlayerDataSync(playerID, playerData);
        }

        this.dataStore.close();

        //dump any remaining unwritten log entries
        this.customLogger.WriteEntries();

        AddLogEntry("GriefPrevention disabled.");
    }

    //called when a player spawns, applies protection for that player if necessary
    public void checkPvpProtectionNeeded(Player player) {
        //if anti spawn camping feature is not enabled, do nothing
        if (!this.config_pvp_protectFreshSpawns) return;

        //if pvp is disabled, do nothing
        if (!pvpRulesApply(player.getWorld())) return;

        //if player is in creative mode, do nothing
        if (player.getGameMode() == GameMode.CREATIVE) return;

        //if the player has the damage any player permission enabled, do nothing
        if (player.hasPermission("griefprevention.nopvpimmunity")) return;

        //check inventory for well, anything
        if (GriefPrevention.isInventoryEmpty(player)) {
            //if empty, apply immunity
            PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());
            playerData.pvpImmune = true;

            //inform the player after he finishes respawning
            sendMessage(player, TextMode.Success, Messages.PvPImmunityStart, 5L);

            //start a task to re-check this player's inventory every minute until his immunity is gone
            PvPImmunityValidationTask task = new PvPImmunityValidationTask(player);
            this.getServer().getScheduler().scheduleSyncDelayedTask(this, task, 1200L);
        }
    }

    //moves a player from the claim he's in to a nearby wilderness location
    public Location ejectPlayer(Player player) {
        //look for a suitable location
        Location candidateLocation = player.getLocation();
        while (true) {
            Claim claim = GriefPrevention.instance.dataStore.getClaimAt(candidateLocation, false, null);

            //if there's a claim here, keep looking
            if (claim != null) {
                candidateLocation = new Location(
                        claim.lesserBoundaryCorner.getWorld(),
                        claim.lesserBoundaryCorner.getBlockX() - 1,
                        claim.lesserBoundaryCorner.getBlockY(),
                        claim.lesserBoundaryCorner.getBlockZ() - 1
                );
            }

            //otherwise find a safe place to teleport the player
            else {
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

    //checks whether players can create claims in a world
    public boolean claimsEnabledForWorld(World world) {
        ClaimsMode mode = this.config_claims_worldModes.get(world);
        return mode != null && mode != ClaimsMode.Disabled;
    }

    //determines whether creative anti-grief rules apply at a location
    public boolean creativeRulesApply(Location location) {
        return this.config_claims_worldModes.get((location.getWorld())) == ClaimsMode.Creative;
    }

    public String allowBuild(Player player, Location location, Material material) {
        if (!GriefPrevention.instance.claimsEnabledForWorld(location.getWorld())) return null;

        PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());
        Claim claim = this.dataStore.getClaimAt(location, false, playerData.lastClaim);

        //exception: administrators in ignore claims mode
        if (playerData.ignoreClaims) return null;

        //wilderness rules
        if (claim == null) {
            //no building in the wilderness in creative mode
            if (this.creativeRulesApply(location) || this.config_claims_worldModes.get(location.getWorld()) == ClaimsMode.SurvivalRequiringClaims) {
                //exception: when chest claims are enabled, players who have zero land claims and are placing a chest
                if (material != Material.CHEST || playerData.getClaims().size() > 0 || GriefPrevention.instance.getPluginConfig().getClaimConfiguration().getCreationConfiguration().automaticPreferredRadius == -1) {
                    String reason = this.dataStore.getMessage(Messages.NoBuildOutsideClaims);
                    if (player.hasPermission("griefprevention.ignoreclaims"))
                        reason += "  " + this.dataStore.getMessage(Messages.IgnoreClaimsAdvertisement);
                    reason += "  " + this.dataStore.getMessage(
                            Messages.CreativeBasicsVideo2,
                            DataStore.CREATIVE_VIDEO_URL
                    );
                    return reason;
                }
                else {
                    return null;
                }
            }

            //but it's fine in survival mode
            else {
                return null;
            }
        }

        //if not in the wilderness, then apply claim rules (permissions, etc)
        else {
            //cache the claim for later reference
            playerData.lastClaim = claim;
            Block block = location.getBlock();

            Supplier<String> supplier = claim.checkPermission(
                    player,
                    ClaimPermission.Build,
                    new BlockPlaceEvent(block,
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

    public String allowBreak(Player player, Block block, Location location) {
        return this.allowBreak(player, block, location, new BlockBreakEvent(block, player));
    }

    public String allowBreak(Player player, Block block, Location location, BlockBreakEvent breakEvent) {
        if (!GriefPrevention.instance.claimsEnabledForWorld(location.getWorld())) return null;

        PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());
        Claim claim = this.dataStore.getClaimAt(location, false, playerData.lastClaim);

        //exception: administrators in ignore claims mode
        if (playerData.ignoreClaims) return null;

        //wilderness rules
        if (claim == null) {
            //no building in the wilderness in creative mode
            if (this.creativeRulesApply(location) || this.config_claims_worldModes.get(location.getWorld()) == ClaimsMode.SurvivalRequiringClaims) {
                String reason = this.dataStore.getMessage(Messages.NoBuildOutsideClaims);
                if (player.hasPermission("griefprevention.ignoreclaims"))
                    reason += "  " + this.dataStore.getMessage(Messages.IgnoreClaimsAdvertisement);
                reason += "  " + this.dataStore.getMessage(Messages.CreativeBasicsVideo2, DataStore.CREATIVE_VIDEO_URL);
                return reason;
            }

            //but it's fine in survival mode
            else {
                return null;
            }
        }
        else {
            //cache the claim for later reference
            playerData.lastClaim = claim;

            //if not in the wilderness, then apply claim rules (permissions, etc)
            Supplier<String> cancel = claim.checkPermission(player, ClaimPermission.Build, breakEvent);
            if (cancel != null && breakEvent != null) {
                PreventBlockBreakEvent preventionEvent = new PreventBlockBreakEvent(breakEvent);
                Bukkit.getPluginManager().callEvent(preventionEvent);
                if (preventionEvent.isCancelled()) {
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
    public void restoreClaim(Claim claim, long delayInTicks) {
        //admin claims aren't automatically cleaned up when deleted or abandoned
        if (claim.isAdminClaim()) return;

        //it's too expensive to do this for huge claims
        if (claim.getArea() > 10000) return;

        ArrayList<Chunk> chunks = claim.getChunks();
        for (Chunk chunk : chunks) {
            this.restoreChunk(chunk, this.getSeaLevel(chunk.getWorld()) - 15, false, delayInTicks, null);
        }
    }

    public void restoreChunk(Chunk chunk, int miny, boolean aggressiveMode, long delayInTicks, Player playerReceivingVisualization) {
        //build a snapshot of this chunk, including 1 block boundary outside the chunk all the way around
        int maxHeight = chunk.getWorld().getMaxHeight();
        BlockSnapshot[][][] snapshots = new BlockSnapshot[18][maxHeight][18];
        Block startBlock = chunk.getBlock(0, 0, 0);
        Location startLocation = new Location(chunk.getWorld(), startBlock.getX() - 1, 0, startBlock.getZ() - 1);
        for (int x = 0; x < snapshots.length; x++) {
            for (int z = 0; z < snapshots[0][0].length; z++) {
                for (int y = 0; y < snapshots[0].length; y++) {
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
                this.getSeaLevel(chunk.getWorld()),
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

    public int getSeaLevel(World world) {
        Integer overrideValue = this.config_seaLevelOverride.get(world.getName());
        if (overrideValue == null || overrideValue == -1) {
            return world.getSeaLevel();
        }
        else {
            return overrideValue;
        }
    }

    public boolean pvpRulesApply(World world) {
        Boolean configSetting = this.config_pvp_specifiedWorlds.get(world);
        if (configSetting != null) return configSetting;
        return world.getPVP();
    }

    public ItemStack getItemInHand(Player player, EquipmentSlot hand) {
        if (hand == EquipmentSlot.OFF_HAND) return player.getInventory().getItemInOffHand();
        return player.getInventory().getItemInMainHand();
    }

    public boolean claimIsPvPSafeZone(Claim claim) {
        if (claim.siegeData != null)
            return false;
        return claim.isAdminClaim() && claim.parent == null && GriefPrevention.instance.config_pvp_noCombatInAdminLandClaims ||
                claim.isAdminClaim() && claim.parent != null && GriefPrevention.instance.config_pvp_noCombatInAdminSubdivisions ||
                !claim.isAdminClaim() && GriefPrevention.instance.config_pvp_noCombatInPlayerLandClaims;
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

    public void startRescueTask(Player player, Location location) {
        //Schedule task to reset player's portal cooldown after 30 seconds (Maximum timeout time for client, in case their network is slow and taking forever to load chunks)
        BukkitTask task = new CheckForPortalTrapTask(player, this, location).runTaskLater(
                GriefPrevention.instance,
                600L
        );

        //Cancel existing rescue task
        if (portalReturnTaskMap.containsKey(player.getUniqueId()))
            portalReturnTaskMap.put(player.getUniqueId(), task).cancel();
        else
            portalReturnTaskMap.put(player.getUniqueId(), task);
    }

    public EconomyHandler getEconomyHandler() {
        return this.economyHandler;
    }

    //thread to build the above cache
    private class CacheOfflinePlayerNamesThread extends Thread {
        private final OfflinePlayer[] offlinePlayers;
        private final ConcurrentHashMap<String, UUID> playerNameToIDMap;

        CacheOfflinePlayerNamesThread(OfflinePlayer[] offlinePlayers, ConcurrentHashMap<String, UUID> playerNameToIDMap) {
            this.offlinePlayers = offlinePlayers;
            this.playerNameToIDMap = playerNameToIDMap;
        }

        public void run() {
            long now = System.currentTimeMillis();
            final long millisecondsPerDay = 1000 * 60 * 60 * 24;
            for (OfflinePlayer player : offlinePlayers) {
                try {
                    UUID playerID = player.getUniqueId();
                    long lastSeen = player.getLastLogin();

                    //if the player has been seen in the last 90 days, cache his name/UUID pair
                    long diff = now - lastSeen;
                    long daysDiff = diff / millisecondsPerDay;
                    if (daysDiff <= config_advanced_offlineplayer_cache_days) {
                        String playerName = player.getName();
                        if (playerName == null) continue;
                        this.playerNameToIDMap.put(playerName, playerID);
                        this.playerNameToIDMap.put(playerName.toLowerCase(), playerID);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
