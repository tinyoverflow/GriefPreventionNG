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
import me.tinyoverflow.griefprevention.handlers.*;
import me.tinyoverflow.griefprevention.tasks.*;
import org.bukkit.*;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Player;
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
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class GriefPrevention extends JavaPlugin
{
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
    public boolean config_claims_preventGlobalMonsterEggs; //whether monster eggs can be placed regardless of trust.
    public boolean config_claims_preventTheft;                        //whether containers and crafting blocks are protectable
    public boolean config_claims_protectCreatures;                    //whether claimed animals may be injured by players without permission
    public boolean config_claims_protectHorses;                        //whether horses on a claim should be protected by that claim's rules
    //configuration variables, loaded/saved from a config.yml
    public boolean config_claims_protectDonkeys;                    //whether donkeys on a claim should be protected by that claim's rules
    public boolean config_claims_protectLlamas;                        //whether llamas on a claim should be protected by that claim's rules
    public boolean config_claims_preventButtonsSwitches;            //whether buttons and switches are protectable
    public boolean config_claims_lockWoodenDoors;                    //whether wooden doors should be locked by default (require /accesstrust)
    public boolean config_claims_lockTrapDoors;                        //whether trap doors should be locked by default (require /accesstrust)
    public boolean config_claims_lockFenceGates;                    //whether fence gates should be locked by default (require /accesstrust)
    public boolean config_claims_preventNonPlayerCreatedPortals;    // whether portals where we cannot determine the creating player should be prevented from creation in claims
    public boolean config_claims_enderPearlsRequireAccessTrust;        //whether teleporting into a claim with a pearl requires access trust
    public boolean config_claims_raidTriggersRequireBuildTrust;      //whether raids are triggered by a player that doesn't have build permission in that claim
    public int config_claims_maxClaimsPerPlayer;                    //maximum number of claims per player
    public boolean config_claims_respectWorldGuard;                 //whether claim creations requires WG build permission in creation area
    public boolean config_claims_villagerTradingRequiresTrust;      //whether trading with a claimed villager requires permission
    public int config_claims_initialBlocks;                            //the number of claim blocks a new player starts with
    public double config_claims_abandonReturnRatio;                 //the portion of claim blocks returned to a player when a claim is abandoned
    public int config_claims_blocksAccruedPerHour_default;            //how many additional blocks players get each hour of play (can be zero) without any special permissions
    public int config_claims_maxAccruedBlocks_default;                //the limit on accrued blocks (over time) for players without any special permissions.  doesn't limit purchased or admin-gifted blocks
    public int config_claims_accruedIdleThreshold;                    //how far (in blocks) a player must move in order to not be considered afk/idle when determining accrued claim blocks
    public int config_claims_accruedIdlePercent;                    //how much percentage of claim block accruals should idle players get
    public int config_claims_maxDepth;                                //limit on how deep claims can go
    public int config_claims_expirationDays;                        //how many days of inactivity before a player loses his claims
    public int config_claims_expirationExemptionTotalBlocks;        //total claim blocks amount which will exempt a player from claim expiration
    public int config_claims_expirationExemptionBonusBlocks;        //bonus claim blocks amount which will exempt a player from claim expiration
    public int config_claims_automaticClaimsForNewPlayersRadius;    //how big automatic new player claims (when they place a chest) should be.  -1 to disable
    public int config_claims_automaticClaimsForNewPlayersRadiusMin; //how big automatic new player claims must be. 0 to disable
    public int config_claims_claimsExtendIntoGroundDistance;        //how far below the shoveled block a new claim will reach
    public int config_claims_minWidth;                                //minimum width for non-admin claims
    public int config_claims_minArea;                               //minimum area for non-admin claims
    public int config_claims_chestClaimExpirationDays;                //number of days of inactivity before an automatic chest claim will be deleted
    public int config_claims_unusedClaimExpirationDays;                //number of days of inactivity before an unused (nothing build) claim will be deleted
    public boolean config_claims_survivalAutoNatureRestoration;        //whether survival claims will be automatically restored to nature when auto-deleted
    public boolean config_claims_allowTrappedInAdminClaims;            //whether it should be allowed to use /trapped in adminclaims.
    public Material config_claims_investigationTool;                //which material will be used to investigate claims with a right click
    public Material config_claims_modificationTool;                    //which material will be used to create/resize claims with a right click
    public ArrayList<String> config_claims_commandsRequiringAccessTrust; //the list of slash commands requiring access trust when in a claim
    public boolean config_claims_supplyPlayerManual;                //whether to give new players a book with land claim help in it
    public int config_claims_manualDeliveryDelaySeconds;            //how long to wait before giving a book to a new player
    public boolean config_claims_firespreads;                        //whether fire will spread in claims
    public boolean config_claims_firedamages;                        //whether fire will damage in claims
    public boolean config_claims_lecternReadingRequiresAccessTrust;                    //reading lecterns requires access trust
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
    public boolean config_endermenMoveBlocks;                        //whether or not endermen may move blocks around
    public boolean config_claims_ravagersBreakBlocks;                //whether or not ravagers may break blocks in claims
    public boolean config_silverfishBreakBlocks;                    //whether silverfish may break blocks
    public boolean config_creaturesTrampleCrops;                    //whether or not non-player entities may trample crops
    public boolean config_rabbitsEatCrops;                          //whether or not rabbits may eat crops
    public boolean config_zombiesBreakDoors;                        //whether or not hard-mode zombies may break down wooden doors
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
    //Track scheduled "rescues" so we can cancel them if the player happens to teleport elsewhere so we can cancel it.
    public ConcurrentHashMap<UUID, BukkitTask> portalReturnTaskMap = new ConcurrentHashMap<>();
    //log entry manager for GP's custom log files
    CustomLogger customLogger;
    // Player event handler
    PlayerEventHandler playerEventHandler;
    HashMap<World, Boolean> config_pvp_specifiedWorlds;                //list of worlds where pvp anti-grief rules apply, according to the config file
    //helper method to resolve a player by name
    ConcurrentHashMap<String, UUID> playerNameToIDMap = new ConcurrentHashMap<>();
    private HoconConfigurationLoader configurationLoader;
    private ConfigurationNode configurationNode;
    private GriefPreventionConfiguration configuration;
    private boolean config_creativeWorldsExist;                     //note on whether there are any creative mode worlds, to save cpu cycles on a common hash lookup
    private EconomyHandler economyHandler;
    private String databaseUrl;
    private String databaseUserName;
    private String databasePassword;

    public GriefPrevention()
    {
        configurationFile = Paths.get(
                this.getDataFolder().getPath(),
                "config.conf"
        );
    }

    //adds a server log entry
    public static synchronized void AddLogEntry(String entry, CustomLogEntryTypes customLogType, boolean excludeFromServerLogs)
    {
        if (customLogType != null && GriefPrevention.instance.customLogger != null)
        {
            GriefPrevention.instance.customLogger.AddEntry(entry, customLogType);
        }
        if (!excludeFromServerLogs) log.info(entry);
    }

    public static synchronized void AddLogEntry(String entry, CustomLogEntryTypes customLogType)
    {
        AddLogEntry(entry, customLogType, false);
    }

    public static synchronized void AddLogEntry(String entry)
    {
        AddLogEntry(entry, CustomLogEntryTypes.Debug);
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
    public static void sendMessage(Player player, ChatColor color, Messages messageID, String... args)
    {
        sendMessage(player, color, messageID, 0, args);
    }

    //sends a color-coded message to a player
    public static void sendMessage(Player player, ChatColor color, Messages messageID, long delayInTicks, String... args)
    {
        String message = GriefPrevention.instance.dataStore.getMessage(messageID, args);
        sendMessage(player, color, message, delayInTicks);
    }

    //sends a color-coded message to a player
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

    public static void sendMessage(Player player, ChatColor color, String message, long delayInTicks)
    {
        SendPlayerMessageTask task = new SendPlayerMessageTask(player, color, message);

        //Only schedule if there should be a delay. Otherwise, send the message right now, else the message will appear out of order.
        if (delayInTicks > 0)
        {
            GriefPrevention.instance.getServer().getScheduler().runTaskLater(GriefPrevention.instance, task, delayInTicks);
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
                player.getStatistic(Statistic.PICKUP, Material.DARK_OAK_LOG) > 0) return false;

        PlayerData playerData = instance.dataStore.getPlayerData(player.getUniqueId());
        if (playerData.getClaims().size() > 0) return false;

        return true;
    }

    public void onLoad()
    {
        // Load the plugin config first, so we have everything ready
        // for all dependencies that might come up.
        this.loadPluginConfig();

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
        log = instance.getLogger();

        this.registerCommands();

        this.customLogger = new CustomLogger();

        AddLogEntry("Finished loading configuration.");

        //when datastore initializes, it loads player and claim data, and posts some stats to the log
        if (this.databaseUrl.length() > 0)
        {
            try
            {
                DatabaseDataStore databaseStore = new DatabaseDataStore(this.databaseUrl, this.databaseUserName, this.databasePassword);

                if (FlatFileDataStore.hasData())
                {
                    GriefPrevention.AddLogEntry("There appears to be some data on the hard drive.  Migrating those data to the database...");
                    FlatFileDataStore flatFileStore = new FlatFileDataStore(this.getDataFolder());
                    this.dataStore = flatFileStore;
                    flatFileStore.migrateData(databaseStore);
                    GriefPrevention.AddLogEntry("Data migration process complete.");
                }

                this.dataStore = databaseStore;
            }
            catch (Exception e)
            {
                GriefPrevention.AddLogEntry("Because there was a problem with the database, GriefPrevention will not function properly.  Either update the database config settings resolve the issue, or delete those lines from your config.yml so that GriefPrevention can use the file system to store data.");
                e.printStackTrace();
                this.getServer().getPluginManager().disablePlugin(this);
                return;
            }
        }

        //if not using the database because it's not configured or because there was a problem, use the file system to store data
        //this is the preferred method, as it's simpler than the database scenario
        if (this.dataStore == null)
        {
            File oldclaimdata = new File(getDataFolder(), "ClaimData");
            if (oldclaimdata.exists())
            {
                if (!FlatFileDataStore.hasData())
                {
                    File claimdata = new File("plugins" + File.separator + "GriefPrevention" + File.separator + "ClaimData");
                    oldclaimdata.renameTo(claimdata);
                    File oldplayerdata = new File(getDataFolder(), "PlayerData");
                    File playerdata = new File("plugins" + File.separator + "GriefPrevention" + File.separator + "PlayerData");
                    oldplayerdata.renameTo(playerdata);
                }
            }
            try
            {
                this.dataStore = new FlatFileDataStore(this.getDataFolder());
            }
            catch (Exception e)
            {
                GriefPrevention.AddLogEntry("Unable to initialize the file system data store.  Details:");
                GriefPrevention.AddLogEntry(e.getMessage());
                e.printStackTrace();
            }
        }

        String dataMode = (this.dataStore instanceof FlatFileDataStore) ? "(File Mode)" : "(Database Mode)";
        AddLogEntry("Finished loading data " + dataMode + ".");

        //unless claim block accrual is disabled, start the recurring per 10 minute event to give claim blocks to online players
        //20L ~ 1 second
        if (this.config_claims_blocksAccruedPerHour_default > 0)
        {
            DeliverClaimBlocksTask task = new DeliverClaimBlocksTask(null, this);
            this.getServer().getScheduler().scheduleSyncRepeatingTask(this, task, 20L * 60 * 10, 20L * 60 * 10);
        }

        //start the recurring cleanup event for entities in creative worlds
        EntityCleanupTask task = new EntityCleanupTask(0);
        this.getServer().getScheduler().scheduleSyncDelayedTask(GriefPrevention.instance, task, 20L * 60 * 2);

        //start recurring cleanup scan for unused claims belonging to inactive players
        FindUnusedClaimsTask task2 = new FindUnusedClaimsTask();
        this.getServer().getScheduler().scheduleSyncRepeatingTask(this, task2, 20L * 60, 20L * config_advanced_claim_expiration_check_rate);

        //register for events
        PluginManager pluginManager = this.getServer().getPluginManager();

        //player events
        playerEventHandler = new PlayerEventHandler(this.dataStore, this);
        pluginManager.registerEvents(playerEventHandler, this);

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
        SiegeEventHandler siegeEventHandler = new SiegeEventHandler();
        pluginManager.registerEvents(siegeEventHandler, this);

        //vault-based economy integration
        economyHandler = new EconomyHandler(this);
        pluginManager.registerEvents(economyHandler, this);

        //cache offline players
        OfflinePlayer[] offlinePlayers = this.getServer().getOfflinePlayers();
        CacheOfflinePlayerNamesThread namesThread = new CacheOfflinePlayerNamesThread(offlinePlayers, this.playerNameToIDMap);
        namesThread.setPriority(Thread.MIN_PRIORITY);
        namesThread.start();

        AddLogEntry("Boot finished.");
    }

    private void registerCommands()
    {
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

    private void savePluginConfig()
    {
        try
        {
            this.configurationNode.set(this.configuration);
            this.configurationLoader.save(this.configurationNode);
        }
        catch (ConfigurateException e)
        {
            throw new RuntimeException(e);
        }
    }

    public GriefPreventionConfiguration getPluginConfig()
    {
        return this.configuration;
    }

    private void loadPluginConfig()
    {
        try
        {
            configurationLoader = HoconConfigurationLoader.builder()
                    .path(configurationFile)
                    .build();

            configurationNode = configurationLoader.load();
            configuration = configurationNode.get(GriefPreventionConfiguration.class);

            // Always saving the file on load to make sure that it exists
            // and is always up-to-date.
            savePluginConfig();
        }
        catch (ConfigurateException e)
        {
            throw new RuntimeException(e);
        }


        // TODO: Remove legacy code once all references have been updated.
        //load the config if it exists
        FileConfiguration config = YamlConfiguration.loadConfiguration(new File(DataStore.configFilePath));
        FileConfiguration outConfig = new YamlConfiguration();
        outConfig.options().header("Default values are perfect for most servers.  If you want to customize and have a question, look for the answer here first: http://dev.bukkit.org/bukkit-plugins/grief-prevention/pages/setup-and-configuration/");

        //read configuration settings (note defaults)
        int configVersion = config.getInt("GriefPrevention.ConfigVersion", 0);

        //get (deprecated node) claims world names from the config file
        List<World> worlds = this.getServer().getWorlds();
        List<String> deprecated_claimsEnabledWorldNames = config.getStringList("GriefPrevention.Claims.Worlds");

        //validate that list
        for (int i = 0; i < deprecated_claimsEnabledWorldNames.size(); i++)
        {
            String worldName = deprecated_claimsEnabledWorldNames.get(i);
            World world = this.getServer().getWorld(worldName);
            if (world == null)
            {
                deprecated_claimsEnabledWorldNames.remove(i--);
            }
        }

        //get (deprecated node) creative world names from the config file
        List<String> deprecated_creativeClaimsEnabledWorldNames = config.getStringList("GriefPrevention.Claims.CreativeRulesWorlds");

        //validate that list
        for (int i = 0; i < deprecated_creativeClaimsEnabledWorldNames.size(); i++)
        {
            String worldName = deprecated_creativeClaimsEnabledWorldNames.get(i);
            World world = this.getServer().getWorld(worldName);
            if (world == null)
            {
                deprecated_claimsEnabledWorldNames.remove(i--);
            }
        }

        //get (deprecated) pvp fire placement proximity note and use it if it exists (in the new config format it will be overwritten later).
        config_pvp_allowFireNearPlayers = config.getBoolean("GriefPrevention.PvP.AllowFlintAndSteelNearOtherPlayers", false);
        //get (deprecated) pvp lava dump proximity note and use it if it exists (in the new config format it will be overwritten later).
        config_pvp_allowLavaNearPlayers = config.getBoolean("GriefPrevention.PvP.AllowLavaDumpingNearOtherPlayers", false);

        //decide claim mode for each world
        this.config_claims_worldModes = new ConcurrentHashMap<>();
        this.config_creativeWorldsExist = false;
        for (World world : worlds)
        {
            //is it specified in the config file?
            String configSetting = config.getString("GriefPrevention.Claims.Mode." + world.getName());
            if (configSetting != null)
            {
                ClaimsMode claimsMode = this.configStringToClaimsMode(configSetting);
                if (claimsMode != null)
                {
                    this.config_claims_worldModes.put(world, claimsMode);
                    if (claimsMode == ClaimsMode.Creative) this.config_creativeWorldsExist = true;
                    continue;
                }
                else
                {
                    GriefPrevention.AddLogEntry("Error: Invalid claim mode \"" + configSetting + "\".  Options are Survival, Creative, and Disabled.");
                    this.config_claims_worldModes.put(world, ClaimsMode.Creative);
                    this.config_creativeWorldsExist = true;
                }
            }

            //was it specified in a deprecated config node?
            if (deprecated_creativeClaimsEnabledWorldNames.contains(world.getName()))
            {
                this.config_claims_worldModes.put(world, ClaimsMode.Creative);
                this.config_creativeWorldsExist = true;
            }
            else if (deprecated_claimsEnabledWorldNames.contains(world.getName()))
            {
                this.config_claims_worldModes.put(world, ClaimsMode.Survival);
            }

            //does the world's name indicate its purpose?
            else if (world.getName().toLowerCase().contains("survival"))
            {
                this.config_claims_worldModes.put(world, ClaimsMode.Survival);
            }
            else if (world.getName().toLowerCase().contains("creative"))
            {
                this.config_claims_worldModes.put(world, ClaimsMode.Creative);
                this.config_creativeWorldsExist = true;
            }

            //decide a default based on server type and world type
            else if (this.getServer().getDefaultGameMode() == GameMode.CREATIVE)
            {
                this.config_claims_worldModes.put(world, ClaimsMode.Creative);
                this.config_creativeWorldsExist = true;
            }
            else if (world.getEnvironment() == Environment.NORMAL)
            {
                this.config_claims_worldModes.put(world, ClaimsMode.Survival);
            }
            else
            {
                this.config_claims_worldModes.put(world, ClaimsMode.Disabled);
            }

            //if the setting WOULD be disabled but this is a server upgrading from the old config format,
            //then default to survival mode for safety's sake (to protect any admin claims which may
            //have been created there)
            if (this.config_claims_worldModes.get(world) == ClaimsMode.Disabled &&
                    deprecated_claimsEnabledWorldNames.size() > 0)
            {
                this.config_claims_worldModes.put(world, ClaimsMode.Survival);
            }
        }

        //pvp worlds list
        this.config_pvp_specifiedWorlds = new HashMap<>();
        for (World world : worlds)
        {
            boolean pvpWorld = config.getBoolean("GriefPrevention.PvP.RulesEnabledInWorld." + world.getName(), world.getPVP());
            this.config_pvp_specifiedWorlds.put(world, pvpWorld);
        }

        //sea level
        this.config_seaLevelOverride = new HashMap<>();
        for (World world : worlds)
        {
            int seaLevelOverride = config.getInt("GriefPrevention.SeaLevelOverrides." + world.getName(), -1);
            outConfig.set("GriefPrevention.SeaLevelOverrides." + world.getName(), seaLevelOverride);
            this.config_seaLevelOverride.put(world.getName(), seaLevelOverride);
        }

        this.config_claims_preventGlobalMonsterEggs = config.getBoolean("GriefPrevention.Claims.PreventGlobalMonsterEggs", true);
        this.config_claims_preventTheft = config.getBoolean("GriefPrevention.Claims.PreventTheft", true);
        this.config_claims_protectCreatures = config.getBoolean("GriefPrevention.Claims.ProtectCreatures", true);
        this.config_claims_protectHorses = config.getBoolean("GriefPrevention.Claims.ProtectHorses", true);
        this.config_claims_protectDonkeys = config.getBoolean("GriefPrevention.Claims.ProtectDonkeys", true);
        this.config_claims_protectLlamas = config.getBoolean("GriefPrevention.Claims.ProtectLlamas", true);
        this.config_claims_preventButtonsSwitches = config.getBoolean("GriefPrevention.Claims.PreventButtonsSwitches", true);
        this.config_claims_lockWoodenDoors = config.getBoolean("GriefPrevention.Claims.LockWoodenDoors", false);
        this.config_claims_lockTrapDoors = config.getBoolean("GriefPrevention.Claims.LockTrapDoors", false);
        this.config_claims_lockFenceGates = config.getBoolean("GriefPrevention.Claims.LockFenceGates", true);
        this.config_claims_preventNonPlayerCreatedPortals = config.getBoolean("GriefPrevention.Claims.PreventNonPlayerCreatedPortals", false);
        this.config_claims_enderPearlsRequireAccessTrust = config.getBoolean("GriefPrevention.Claims.EnderPearlsRequireAccessTrust", true);
        this.config_claims_raidTriggersRequireBuildTrust = config.getBoolean("GriefPrevention.Claims.RaidTriggersRequireBuildTrust", true);
        this.config_claims_initialBlocks = config.getInt("GriefPrevention.Claims.InitialBlocks", 100);
        this.config_claims_blocksAccruedPerHour_default = config.getInt("GriefPrevention.Claims.BlocksAccruedPerHour", 100);
        this.config_claims_blocksAccruedPerHour_default = config.getInt("GriefPrevention.Claims.Claim Blocks Accrued Per Hour.Default", config_claims_blocksAccruedPerHour_default);
        this.config_claims_maxAccruedBlocks_default = config.getInt("GriefPrevention.Claims.MaxAccruedBlocks", 80000);
        this.config_claims_maxAccruedBlocks_default = config.getInt("GriefPrevention.Claims.Max Accrued Claim Blocks.Default", this.config_claims_maxAccruedBlocks_default);
        this.config_claims_accruedIdleThreshold = config.getInt("GriefPrevention.Claims.AccruedIdleThreshold", 0);
        this.config_claims_accruedIdleThreshold = config.getInt("GriefPrevention.Claims.Accrued Idle Threshold", this.config_claims_accruedIdleThreshold);
        this.config_claims_accruedIdlePercent = config.getInt("GriefPrevention.Claims.AccruedIdlePercent", 0);
        this.config_claims_abandonReturnRatio = config.getDouble("GriefPrevention.Claims.AbandonReturnRatio", 1.0D);
        this.config_claims_automaticClaimsForNewPlayersRadius = config.getInt("GriefPrevention.Claims.AutomaticNewPlayerClaimsRadius", 4);
        this.config_claims_automaticClaimsForNewPlayersRadiusMin = Math.max(0, Math.min(this.config_claims_automaticClaimsForNewPlayersRadius,
                config.getInt("GriefPrevention.Claims.AutomaticNewPlayerClaimsRadiusMinimum", 0)));
        this.config_claims_claimsExtendIntoGroundDistance = Math.abs(config.getInt("GriefPrevention.Claims.ExtendIntoGroundDistance", 5));
        this.config_claims_minWidth = config.getInt("GriefPrevention.Claims.MinimumWidth", 5);
        this.config_claims_minArea = config.getInt("GriefPrevention.Claims.MinimumArea", 100);
        this.config_claims_maxDepth = config.getInt("GriefPrevention.Claims.MaximumDepth", Integer.MIN_VALUE);
        if (configVersion < 1 && this.config_claims_maxDepth == 0)
        {
            // If MaximumDepth is untouched in an older configuration, correct it.
            this.config_claims_maxDepth = Integer.MIN_VALUE;
            AddLogEntry("Updated default value for GriefPrevention.Claims.MaximumDepth to " + Integer.MIN_VALUE);
        }
        this.config_claims_chestClaimExpirationDays = config.getInt("GriefPrevention.Claims.Expiration.ChestClaimDays", 7);
        this.config_claims_unusedClaimExpirationDays = config.getInt("GriefPrevention.Claims.Expiration.UnusedClaimDays", 14);
        this.config_claims_expirationDays = config.getInt("GriefPrevention.Claims.Expiration.AllClaims.DaysInactive", 60);
        this.config_claims_expirationExemptionTotalBlocks = config.getInt("GriefPrevention.Claims.Expiration.AllClaims.ExceptWhenOwnerHasTotalClaimBlocks", 10000);
        this.config_claims_expirationExemptionBonusBlocks = config.getInt("GriefPrevention.Claims.Expiration.AllClaims.ExceptWhenOwnerHasBonusClaimBlocks", 5000);
        this.config_claims_survivalAutoNatureRestoration = config.getBoolean("GriefPrevention.Claims.Expiration.AutomaticNatureRestoration.SurvivalWorlds", false);
        this.config_claims_allowTrappedInAdminClaims = config.getBoolean("GriefPrevention.Claims.AllowTrappedInAdminClaims", false);

        this.config_claims_maxClaimsPerPlayer = config.getInt("GriefPrevention.Claims.MaximumNumberOfClaimsPerPlayer", 0);
        this.config_claims_respectWorldGuard = config.getBoolean("GriefPrevention.Claims.CreationRequiresWorldGuardBuildPermission", true);
        this.config_claims_villagerTradingRequiresTrust = config.getBoolean("GriefPrevention.Claims.VillagerTradingRequiresPermission", true);
        String accessTrustSlashCommands = config.getString("GriefPrevention.Claims.CommandsRequiringAccessTrust", "/sethome");
        this.config_claims_supplyPlayerManual = config.getBoolean("GriefPrevention.Claims.DeliverManuals", true);
        this.config_claims_manualDeliveryDelaySeconds = config.getInt("GriefPrevention.Claims.ManualDeliveryDelaySeconds", 30);
        this.config_claims_ravagersBreakBlocks = config.getBoolean("GriefPrevention.Claims.RavagersBreakBlocks", true);

        this.config_claims_firespreads = config.getBoolean("GriefPrevention.Claims.FireSpreadsInClaims", false);
        this.config_claims_firedamages = config.getBoolean("GriefPrevention.Claims.FireDamagesInClaims", false);
        this.config_claims_lecternReadingRequiresAccessTrust = config.getBoolean("GriefPrevention.Claims.LecternReadingRequiresAccessTrust", true);

        this.config_pvp_protectFreshSpawns = config.getBoolean("GriefPrevention.PvP.ProtectFreshSpawns", true);
        this.config_pvp_punishLogout = config.getBoolean("GriefPrevention.PvP.PunishLogout", true);
        this.config_pvp_combatTimeoutSeconds = config.getInt("GriefPrevention.PvP.CombatTimeoutSeconds", 15);
        this.config_pvp_allowCombatItemDrop = config.getBoolean("GriefPrevention.PvP.AllowCombatItemDrop", false);
        String bannedPvPCommandsList = config.getString("GriefPrevention.PvP.BlockedSlashCommands", "/home;/vanish;/spawn;/tpa");

        this.config_economy_claimBlocksMaxBonus = config.getInt("GriefPrevention.Economy.ClaimBlocksMaxBonus", 0);
        this.config_economy_claimBlocksPurchaseCost = config.getDouble("GriefPrevention.Economy.ClaimBlocksPurchaseCost", 0);
        this.config_economy_claimBlocksSellValue = config.getDouble("GriefPrevention.Economy.ClaimBlocksSellValue", 0);

        this.config_lockDeathDropsInPvpWorlds = config.getBoolean("GriefPrevention.ProtectItemsDroppedOnDeath.PvPWorlds", false);
        this.config_lockDeathDropsInNonPvpWorlds = config.getBoolean("GriefPrevention.ProtectItemsDroppedOnDeath.NonPvPWorlds", true);

        this.config_blockClaimExplosions = config.getBoolean("GriefPrevention.BlockLandClaimExplosions", true);
        this.config_blockSurfaceCreeperExplosions = config.getBoolean("GriefPrevention.BlockSurfaceCreeperExplosions", true);
        this.config_blockSurfaceOtherExplosions = config.getBoolean("GriefPrevention.BlockSurfaceOtherExplosions", true);
        this.config_blockSkyTrees = config.getBoolean("GriefPrevention.LimitSkyTrees", true);
        this.config_limitTreeGrowth = config.getBoolean("GriefPrevention.LimitTreeGrowth", false);
        this.config_pistonExplosionSound = config.getBoolean("GriefPrevention.PistonExplosionSound", true);
        this.config_pistonMovement = PistonMode.of(config.getString("GriefPrevention.PistonMovement", "CLAIMS_ONLY"));
        if (config.isBoolean("GriefPrevention.LimitPistonsToLandClaims") && !config.getBoolean("GriefPrevention.LimitPistonsToLandClaims"))
            this.config_pistonMovement = PistonMode.EVERYWHERE_SIMPLE;
        if (config.isBoolean("GriefPrevention.CheckPistonMovement") && !config.getBoolean("GriefPrevention.CheckPistonMovement"))
            this.config_pistonMovement = PistonMode.IGNORED;

        this.config_fireSpreads = config.getBoolean("GriefPrevention.FireSpreads", false);
        this.config_fireDestroys = config.getBoolean("GriefPrevention.FireDestroys", false);

        this.config_visualizationAntiCheatCompat = config.getBoolean("GriefPrevention.VisualizationAntiCheatCompatMode", false);

        this.config_endermenMoveBlocks = config.getBoolean("GriefPrevention.EndermenMoveBlocks", false);
        this.config_silverfishBreakBlocks = config.getBoolean("GriefPrevention.SilverfishBreakBlocks", false);
        this.config_creaturesTrampleCrops = config.getBoolean("GriefPrevention.CreaturesTrampleCrops", false);
        this.config_rabbitsEatCrops = config.getBoolean("GriefPrevention.RabbitsEatCrops", true);
        this.config_zombiesBreakDoors = config.getBoolean("GriefPrevention.HardModeZombiesBreakDoors", false);

        //default for claim investigation tool
        String investigationToolMaterialName = Material.STICK.name();

        //get investigation tool from config
        investigationToolMaterialName = config.getString("GriefPrevention.Claims.InvestigationTool", investigationToolMaterialName);

        //validate investigation tool
        this.config_claims_investigationTool = Material.getMaterial(investigationToolMaterialName);
        if (this.config_claims_investigationTool == null)
        {
            GriefPrevention.AddLogEntry("ERROR: Material " + investigationToolMaterialName + " not found.  Defaulting to the stick.  Please update your config.yml.");
            this.config_claims_investigationTool = Material.STICK;
        }

        //default for claim creation/modification tool
        String modificationToolMaterialName = Material.GOLDEN_SHOVEL.name();

        //get modification tool from config
        modificationToolMaterialName = config.getString("GriefPrevention.Claims.ModificationTool", modificationToolMaterialName);

        //validate modification tool
        this.config_claims_modificationTool = Material.getMaterial(modificationToolMaterialName);
        if (this.config_claims_modificationTool == null)
        {
            GriefPrevention.AddLogEntry("ERROR: Material " + modificationToolMaterialName + " not found.  Defaulting to the golden shovel.  Please update your config.yml.");
            this.config_claims_modificationTool = Material.GOLDEN_SHOVEL;
        }

        this.config_pvp_noCombatInPlayerLandClaims = config.getBoolean("GriefPrevention.PvP.ProtectPlayersInLandClaims.PlayerOwnedClaims", true);
        this.config_pvp_noCombatInAdminLandClaims = config.getBoolean("GriefPrevention.PvP.ProtectPlayersInLandClaims.AdministrativeClaims", true);
        this.config_pvp_noCombatInAdminSubdivisions = config.getBoolean("GriefPrevention.PvP.ProtectPlayersInLandClaims.AdministrativeSubdivisions", true);
        this.config_pvp_allowLavaNearPlayers = config.getBoolean("GriefPrevention.PvP.AllowLavaDumpingNearOtherPlayers.PvPWorlds", true);
        this.config_pvp_allowLavaNearPlayers_NonPvp = config.getBoolean("GriefPrevention.PvP.AllowLavaDumpingNearOtherPlayers.NonPvPWorlds", false);
        this.config_pvp_allowFireNearPlayers = config.getBoolean("GriefPrevention.PvP.AllowFlintAndSteelNearOtherPlayers.PvPWorlds", true);
        this.config_pvp_allowFireNearPlayers_NonPvp = config.getBoolean("GriefPrevention.PvP.AllowFlintAndSteelNearOtherPlayers.NonPvPWorlds", false);
        this.config_pvp_protectPets = config.getBoolean("GriefPrevention.PvP.ProtectPetsOutsideLandClaims", false);

        //optional database settings
        this.databaseUrl = config.getString("GriefPrevention.Database.URL", "");
        this.databaseUserName = config.getString("GriefPrevention.Database.UserName", "");
        this.databasePassword = config.getString("GriefPrevention.Database.Password", "");

        this.config_advanced_fixNegativeClaimblockAmounts = config.getBoolean("GriefPrevention.Advanced.fixNegativeClaimblockAmounts", true);
        this.config_advanced_claim_expiration_check_rate = config.getInt("GriefPrevention.Advanced.ClaimExpirationCheckRate", 60);
        this.config_advanced_offlineplayer_cache_days = config.getInt("GriefPrevention.Advanced.OfflinePlayer_cache_days", 90);

        //custom logger settings
        this.config_logs_daysToKeep = config.getInt("GriefPrevention.Abridged Logs.Days To Keep", 7);
        this.config_logs_socialEnabled = config.getBoolean("GriefPrevention.Abridged Logs.Included Entry Types.Social Activity", true);
        this.config_logs_suspiciousEnabled = config.getBoolean("GriefPrevention.Abridged Logs.Included Entry Types.Suspicious Activity", true);
        this.config_logs_adminEnabled = config.getBoolean("GriefPrevention.Abridged Logs.Included Entry Types.Administrative Activity", false);
        this.config_logs_debugEnabled = config.getBoolean("GriefPrevention.Abridged Logs.Included Entry Types.Debug", false);
        this.config_logs_mutedChatEnabled = config.getBoolean("GriefPrevention.Abridged Logs.Included Entry Types.Muted Chat Messages", false);

        //claims mode by world
        for (World world : this.config_claims_worldModes.keySet())
        {
            outConfig.set(
                    "GriefPrevention.Claims.Mode." + world.getName(),
                    this.config_claims_worldModes.get(world).name());
        }


        outConfig.set("GriefPrevention.Claims.PreventGlobalMonsterEggs", this.config_claims_preventGlobalMonsterEggs);
        outConfig.set("GriefPrevention.Claims.PreventTheft", this.config_claims_preventTheft);
        outConfig.set("GriefPrevention.Claims.ProtectCreatures", this.config_claims_protectCreatures);
        outConfig.set("GriefPrevention.Claims.PreventButtonsSwitches", this.config_claims_preventButtonsSwitches);
        outConfig.set("GriefPrevention.Claims.LockWoodenDoors", this.config_claims_lockWoodenDoors);
        outConfig.set("GriefPrevention.Claims.LockTrapDoors", this.config_claims_lockTrapDoors);
        outConfig.set("GriefPrevention.Claims.LockFenceGates", this.config_claims_lockFenceGates);
        outConfig.set("GriefPrevention.Claims.EnderPearlsRequireAccessTrust", this.config_claims_enderPearlsRequireAccessTrust);
        outConfig.set("GriefPrevention.Claims.RaidTriggersRequireBuildTrust", this.config_claims_raidTriggersRequireBuildTrust);
        outConfig.set("GriefPrevention.Claims.ProtectHorses", this.config_claims_protectHorses);
        outConfig.set("GriefPrevention.Claims.ProtectDonkeys", this.config_claims_protectDonkeys);
        outConfig.set("GriefPrevention.Claims.ProtectLlamas", this.config_claims_protectLlamas);
        outConfig.set("GriefPrevention.Claims.InitialBlocks", this.config_claims_initialBlocks);
        outConfig.set("GriefPrevention.Claims.Claim Blocks Accrued Per Hour.Default", this.config_claims_blocksAccruedPerHour_default);
        outConfig.set("GriefPrevention.Claims.Max Accrued Claim Blocks.Default", this.config_claims_maxAccruedBlocks_default);
        outConfig.set("GriefPrevention.Claims.Accrued Idle Threshold", this.config_claims_accruedIdleThreshold);
        outConfig.set("GriefPrevention.Claims.AccruedIdlePercent", this.config_claims_accruedIdlePercent);
        outConfig.set("GriefPrevention.Claims.AbandonReturnRatio", this.config_claims_abandonReturnRatio);
        outConfig.set("GriefPrevention.Claims.AutomaticNewPlayerClaimsRadius", this.config_claims_automaticClaimsForNewPlayersRadius);
        outConfig.set("GriefPrevention.Claims.AutomaticNewPlayerClaimsRadiusMinimum", this.config_claims_automaticClaimsForNewPlayersRadiusMin);
        outConfig.set("GriefPrevention.Claims.ExtendIntoGroundDistance", this.config_claims_claimsExtendIntoGroundDistance);
        outConfig.set("GriefPrevention.Claims.MinimumWidth", this.config_claims_minWidth);
        outConfig.set("GriefPrevention.Claims.MinimumArea", this.config_claims_minArea);
        outConfig.set("GriefPrevention.Claims.MaximumDepth", this.config_claims_maxDepth);
        outConfig.set("GriefPrevention.Claims.InvestigationTool", this.config_claims_investigationTool.name());
        outConfig.set("GriefPrevention.Claims.ModificationTool", this.config_claims_modificationTool.name());
        outConfig.set("GriefPrevention.Claims.Expiration.ChestClaimDays", this.config_claims_chestClaimExpirationDays);
        outConfig.set("GriefPrevention.Claims.Expiration.UnusedClaimDays", this.config_claims_unusedClaimExpirationDays);
        outConfig.set("GriefPrevention.Claims.Expiration.AllClaims.DaysInactive", this.config_claims_expirationDays);
        outConfig.set("GriefPrevention.Claims.Expiration.AllClaims.ExceptWhenOwnerHasTotalClaimBlocks", this.config_claims_expirationExemptionTotalBlocks);
        outConfig.set("GriefPrevention.Claims.Expiration.AllClaims.ExceptWhenOwnerHasBonusClaimBlocks", this.config_claims_expirationExemptionBonusBlocks);
        outConfig.set("GriefPrevention.Claims.Expiration.AutomaticNatureRestoration.SurvivalWorlds", this.config_claims_survivalAutoNatureRestoration);
        outConfig.set("GriefPrevention.Claims.AllowTrappedInAdminClaims", this.config_claims_allowTrappedInAdminClaims);
        outConfig.set("GriefPrevention.Claims.MaximumNumberOfClaimsPerPlayer", this.config_claims_maxClaimsPerPlayer);
        outConfig.set("GriefPrevention.Claims.CreationRequiresWorldGuardBuildPermission", this.config_claims_respectWorldGuard);
        outConfig.set("GriefPrevention.Claims.VillagerTradingRequiresPermission", this.config_claims_villagerTradingRequiresTrust);
        outConfig.set("GriefPrevention.Claims.CommandsRequiringAccessTrust", accessTrustSlashCommands);
        outConfig.set("GriefPrevention.Claims.DeliverManuals", config_claims_supplyPlayerManual);
        outConfig.set("GriefPrevention.Claims.ManualDeliveryDelaySeconds", config_claims_manualDeliveryDelaySeconds);
        outConfig.set("GriefPrevention.Claims.RavagersBreakBlocks", config_claims_ravagersBreakBlocks);

        outConfig.set("GriefPrevention.Claims.FireSpreadsInClaims", config_claims_firespreads);
        outConfig.set("GriefPrevention.Claims.FireDamagesInClaims", config_claims_firedamages);
        outConfig.set("GriefPrevention.Claims.LecternReadingRequiresAccessTrust", config_claims_lecternReadingRequiresAccessTrust);

        for (World world : worlds)
        {
            outConfig.set("GriefPrevention.PvP.RulesEnabledInWorld." + world.getName(), this.pvpRulesApply(world));
        }
        outConfig.set("GriefPrevention.PvP.ProtectFreshSpawns", this.config_pvp_protectFreshSpawns);
        outConfig.set("GriefPrevention.PvP.PunishLogout", this.config_pvp_punishLogout);
        outConfig.set("GriefPrevention.PvP.CombatTimeoutSeconds", this.config_pvp_combatTimeoutSeconds);
        outConfig.set("GriefPrevention.PvP.AllowCombatItemDrop", this.config_pvp_allowCombatItemDrop);
        outConfig.set("GriefPrevention.PvP.BlockedSlashCommands", bannedPvPCommandsList);
        outConfig.set("GriefPrevention.PvP.ProtectPlayersInLandClaims.PlayerOwnedClaims", this.config_pvp_noCombatInPlayerLandClaims);
        outConfig.set("GriefPrevention.PvP.ProtectPlayersInLandClaims.AdministrativeClaims", this.config_pvp_noCombatInAdminLandClaims);
        outConfig.set("GriefPrevention.PvP.ProtectPlayersInLandClaims.AdministrativeSubdivisions", this.config_pvp_noCombatInAdminSubdivisions);
        outConfig.set("GriefPrevention.PvP.AllowLavaDumpingNearOtherPlayers.PvPWorlds", this.config_pvp_allowLavaNearPlayers);
        outConfig.set("GriefPrevention.PvP.AllowLavaDumpingNearOtherPlayers.NonPvPWorlds", this.config_pvp_allowLavaNearPlayers_NonPvp);
        outConfig.set("GriefPrevention.PvP.AllowFlintAndSteelNearOtherPlayers.PvPWorlds", this.config_pvp_allowFireNearPlayers);
        outConfig.set("GriefPrevention.PvP.AllowFlintAndSteelNearOtherPlayers.NonPvPWorlds", this.config_pvp_allowFireNearPlayers_NonPvp);
        outConfig.set("GriefPrevention.PvP.ProtectPetsOutsideLandClaims", this.config_pvp_protectPets);

        outConfig.set("GriefPrevention.Economy.ClaimBlocksMaxBonus", this.config_economy_claimBlocksMaxBonus);
        outConfig.set("GriefPrevention.Economy.ClaimBlocksPurchaseCost", this.config_economy_claimBlocksPurchaseCost);
        outConfig.set("GriefPrevention.Economy.ClaimBlocksSellValue", this.config_economy_claimBlocksSellValue);

        outConfig.set("GriefPrevention.ProtectItemsDroppedOnDeath.PvPWorlds", this.config_lockDeathDropsInPvpWorlds);
        outConfig.set("GriefPrevention.ProtectItemsDroppedOnDeath.NonPvPWorlds", this.config_lockDeathDropsInNonPvpWorlds);

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

        outConfig.set("GriefPrevention.Advanced.fixNegativeClaimblockAmounts", this.config_advanced_fixNegativeClaimblockAmounts);
        outConfig.set("GriefPrevention.Advanced.ClaimExpirationCheckRate", this.config_advanced_claim_expiration_check_rate);
        outConfig.set("GriefPrevention.Advanced.OfflinePlayer_cache_days", this.config_advanced_offlineplayer_cache_days);

        //custom logger settings
        outConfig.set("GriefPrevention.Abridged Logs.Days To Keep", this.config_logs_daysToKeep);
        outConfig.set("GriefPrevention.Abridged Logs.Included Entry Types.Social Activity", this.config_logs_socialEnabled);
        outConfig.set("GriefPrevention.Abridged Logs.Included Entry Types.Suspicious Activity", this.config_logs_suspiciousEnabled);
        outConfig.set("GriefPrevention.Abridged Logs.Included Entry Types.Administrative Activity", this.config_logs_adminEnabled);
        outConfig.set("GriefPrevention.Abridged Logs.Included Entry Types.Debug", this.config_logs_debugEnabled);
        outConfig.set("GriefPrevention.Abridged Logs.Included Entry Types.Muted Chat Messages", this.config_logs_mutedChatEnabled);
        outConfig.set("GriefPrevention.ConfigVersion", 1);

        try
        {
            outConfig.save(DataStore.configFilePath);
        }
        catch (IOException exception)
        {
            AddLogEntry("Unable to write to the configuration file at \"" + DataStore.configFilePath + "\"");
        }

        //try to parse the list of commands requiring access trust in land claims
        this.config_claims_commandsRequiringAccessTrust = new ArrayList<>();
        String[] commands = accessTrustSlashCommands.split(";");
        for (String command : commands)
        {
            if (!command.isEmpty())
            {
                this.config_claims_commandsRequiringAccessTrust.add(command.trim().toLowerCase());
            }
        }

        //try to parse the list of commands which should be banned during pvp combat
        this.config_pvp_blockedCommands = new ArrayList<>();
        commands = bannedPvPCommandsList.split(";");
        for (String command : commands)
        {
            this.config_pvp_blockedCommands.add(command.trim().toLowerCase());
        }
    }

    private ClaimsMode configStringToClaimsMode(String configSetting)
    {
        if (configSetting.equalsIgnoreCase("Survival"))
        {
            return ClaimsMode.Survival;
        }
        else if (configSetting.equalsIgnoreCase("Creative"))
        {
            return ClaimsMode.Creative;
        }
        else if (configSetting.equalsIgnoreCase("Disabled"))
        {
            return ClaimsMode.Disabled;
        }
        else if (configSetting.equalsIgnoreCase("SurvivalRequiringClaims"))
        {
            return ClaimsMode.SurvivalRequiringClaims;
        }
        else
        {
            return null;
        }
    }

    public DataStore getDataStore()
    {
        return this.dataStore;
    }

    public void onDisable()
    {
        //save data for any online players
        @SuppressWarnings("unchecked")
        Collection<Player> players = (Collection<Player>) this.getServer().getOnlinePlayers();
        for (Player player : players)
        {
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
    public void checkPvpProtectionNeeded(Player player)
    {
        //if anti spawn camping feature is not enabled, do nothing
        if (!this.config_pvp_protectFreshSpawns) return;

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
    public Location ejectPlayer(Player player)
    {
        //look for a suitable location
        Location candidateLocation = player.getLocation();
        while (true)
        {
            Claim claim = null;
            claim = GriefPrevention.instance.dataStore.getClaimAt(candidateLocation, false, null);

            //if there's a claim here, keep looking
            if (claim != null)
            {
                candidateLocation = new Location(claim.lesserBoundaryCorner.getWorld(), claim.lesserBoundaryCorner.getBlockX() - 1, claim.lesserBoundaryCorner.getBlockY(), claim.lesserBoundaryCorner.getBlockZ() - 1);
                continue;
            }

            //otherwise find a safe place to teleport the player
            else
            {
                //find a safe height, a couple of blocks above the surface
                GuaranteeChunkLoaded(candidateLocation);
                Block highestBlock = candidateLocation.getWorld().getHighestBlockAt(candidateLocation.getBlockX(), candidateLocation.getBlockZ());
                Location destination = new Location(highestBlock.getWorld(), highestBlock.getX(), highestBlock.getY() + 2, highestBlock.getZ());
                player.teleport(destination);
                return destination;
            }
        }
    }

    //checks whether players can create claims in a world
    public boolean claimsEnabledForWorld(World world)
    {
        ClaimsMode mode = this.config_claims_worldModes.get(world);
        return mode != null && mode != ClaimsMode.Disabled;
    }

    //determines whether creative anti-grief rules apply at a location
    public boolean creativeRulesApply(Location location)
    {
        if (!this.config_creativeWorldsExist) return false;

        return this.config_claims_worldModes.get((location.getWorld())) == ClaimsMode.Creative;
    }

    public String allowBuild(Player player, Location location)
    {
        // TODO check all derivatives and rework API
        return this.allowBuild(player, location, location.getBlock().getType());
    }

    public String allowBuild(Player player, Location location, Material material)
    {
        if (!GriefPrevention.instance.claimsEnabledForWorld(location.getWorld())) return null;

        PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());
        Claim claim = this.dataStore.getClaimAt(location, false, playerData.lastClaim);

        //exception: administrators in ignore claims mode
        if (playerData.ignoreClaims) return null;

        //wilderness rules
        if (claim == null)
        {
            //no building in the wilderness in creative mode
            if (this.creativeRulesApply(location) || this.config_claims_worldModes.get(location.getWorld()) == ClaimsMode.SurvivalRequiringClaims)
            {
                //exception: when chest claims are enabled, players who have zero land claims and are placing a chest
                if (material != Material.CHEST || playerData.getClaims().size() > 0 || GriefPrevention.instance.config_claims_automaticClaimsForNewPlayersRadius == -1)
                {
                    String reason = this.dataStore.getMessage(Messages.NoBuildOutsideClaims);
                    if (player.hasPermission("griefprevention.ignoreclaims"))
                        reason += "  " + this.dataStore.getMessage(Messages.IgnoreClaimsAdvertisement);
                    reason += "  " + this.dataStore.getMessage(Messages.CreativeBasicsVideo2, DataStore.CREATIVE_VIDEO_URL);
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

            Supplier<String> supplier = claim.checkPermission(player, ClaimPermission.Build, new BlockPlaceEvent(block, block.getState(), block, new ItemStack(material), player, true, EquipmentSlot.HAND));

            if (supplier == null) return null;

            return supplier.get();
        }
    }

    public String allowBreak(Player player, Block block, Location location)
    {
        return this.allowBreak(player, block, location, new BlockBreakEvent(block, player));
    }

    public String allowBreak(Player player, Material material, Location location, BlockBreakEvent breakEvent)
    {
        return this.allowBreak(player, location.getBlock(), location, breakEvent);
    }

    public String allowBreak(Player player, Block block, Location location, BlockBreakEvent breakEvent)
    {
        if (!GriefPrevention.instance.claimsEnabledForWorld(location.getWorld())) return null;

        PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());
        Claim claim = this.dataStore.getClaimAt(location, false, playerData.lastClaim);

        //exception: administrators in ignore claims mode
        if (playerData.ignoreClaims) return null;

        //wilderness rules
        if (claim == null)
        {
            //no building in the wilderness in creative mode
            if (this.creativeRulesApply(location) || this.config_claims_worldModes.get(location.getWorld()) == ClaimsMode.SurvivalRequiringClaims)
            {
                String reason = this.dataStore.getMessage(Messages.NoBuildOutsideClaims);
                if (player.hasPermission("griefprevention.ignoreclaims"))
                    reason += "  " + this.dataStore.getMessage(Messages.IgnoreClaimsAdvertisement);
                reason += "  " + this.dataStore.getMessage(Messages.CreativeBasicsVideo2, DataStore.CREATIVE_VIDEO_URL);
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
    public void restoreClaim(Claim claim, long delayInTicks)
    {
        //admin claims aren't automatically cleaned up when deleted or abandoned
        if (claim.isAdminClaim()) return;

        //it's too expensive to do this for huge claims
        if (claim.getArea() > 10000) return;

        ArrayList<Chunk> chunks = claim.getChunks();
        for (Chunk chunk : chunks)
        {
            this.restoreChunk(chunk, this.getSeaLevel(chunk.getWorld()) - 15, false, delayInTicks, null);
        }
    }

    public void restoreChunk(Chunk chunk, int miny, boolean aggressiveMode, long delayInTicks, Player playerReceivingVisualization)
    {
        //build a snapshot of this chunk, including 1 block boundary outside of the chunk all the way around
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
                    Block block = chunk.getWorld().getBlockAt(startLocation.getBlockX() + x, startLocation.getBlockY() + y, startLocation.getBlockZ() + z);
                    snapshots[x][y][z] = new BlockSnapshot(block.getLocation(), block.getType(), block.getBlockData());
                }
            }
        }

        //create task to process those data in another thread
        Location lesserBoundaryCorner = chunk.getBlock(0, 0, 0).getLocation();
        Location greaterBoundaryCorner = chunk.getBlock(15, 0, 15).getLocation();

        //create task
        //when done processing, this task will create a main thread task to actually update the world with processing results
        RestoreNatureProcessingTask task = new RestoreNatureProcessingTask(snapshots, miny, chunk.getWorld().getEnvironment(), lesserBoundaryCorner.getBlock().getBiome(), lesserBoundaryCorner, greaterBoundaryCorner, this.getSeaLevel(chunk.getWorld()), aggressiveMode, GriefPrevention.instance.creativeRulesApply(lesserBoundaryCorner), playerReceivingVisualization);
        GriefPrevention.instance.getServer().getScheduler().runTaskLaterAsynchronously(GriefPrevention.instance, task, delayInTicks);
    }

    private Set<Material> parseMaterialListFromConfig(List<String> stringsToParse)
    {
        Set<Material> materials = EnumSet.noneOf(Material.class);

        //for each string in the list
        for (int i = 0; i < stringsToParse.size(); i++)
        {
            String string = stringsToParse.get(i);

            //defensive coding
            if (string == null) continue;

            //try to parse the string value into a material
            Material material = Material.getMaterial(string.toUpperCase());

            //null value returned indicates an error parsing the string from the config file
            if (material == null)
            {
                //check if string has failed validity before
                if (!string.contains("can't"))
                {
                    //update string, which will go out to config file to help user find the error entry
                    stringsToParse.set(i, string + "     <-- can't understand this entry, see BukkitDev documentation");

                    //warn about invalid material in log
                    GriefPrevention.AddLogEntry(String.format("ERROR: Invalid material %s.  Please update your config.yml.", string));
                }
            }

            //otherwise material is valid, add it
            else
            {
                materials.add(material);
            }
        }

        return materials;
    }

    public int getSeaLevel(World world)
    {
        Integer overrideValue = this.config_seaLevelOverride.get(world.getName());
        if (overrideValue == null || overrideValue == -1)
        {
            return world.getSeaLevel();
        }
        else
        {
            return overrideValue;
        }
    }

    public boolean pvpRulesApply(World world)
    {
        Boolean configSetting = this.config_pvp_specifiedWorlds.get(world);
        if (configSetting != null) return configSetting;
        return world.getPVP();
    }

    public ItemStack getItemInHand(Player player, EquipmentSlot hand)
    {
        if (hand == EquipmentSlot.OFF_HAND) return player.getInventory().getItemInOffHand();
        return player.getInventory().getItemInMainHand();
    }

    public boolean claimIsPvPSafeZone(Claim claim)
    {
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

    public void startRescueTask(Player player, Location location)
    {
        //Schedule task to reset player's portal cooldown after 30 seconds (Maximum timeout time for client, in case their network is slow and taking forever to load chunks)
        BukkitTask task = new CheckForPortalTrapTask(player, this, location).runTaskLater(GriefPrevention.instance, 600L);

        //Cancel existing rescue task
        if (portalReturnTaskMap.containsKey(player.getUniqueId()))
            portalReturnTaskMap.put(player.getUniqueId(), task).cancel();
        else
            portalReturnTaskMap.put(player.getUniqueId(), task);
    }

    public EconomyHandler getEconomyHandler()
    {
        return this.economyHandler;
    }

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
                    if (playerID == null) continue;
                    long lastSeen = player.getLastPlayed();

                    //if the player has been seen in the last 90 days, cache his name/UUID pair
                    long diff = now - lastSeen;
                    long daysDiff = diff / millisecondsPerDay;
                    if (daysDiff <= config_advanced_offlineplayer_cache_days)
                    {
                        String playerName = player.getName();
                        if (playerName == null) continue;
                        this.playerNameToIDMap.put(playerName, playerID);
                        this.playerNameToIDMap.put(playerName.toLowerCase(), playerID);
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
