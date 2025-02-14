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

package me.tinyoverflow.griefprevention.datastore;

import com.google.common.io.Files;
import com.griefprevention.visualization.BoundaryVisualization;
import com.griefprevention.visualization.VisualizationType;
import me.tinyoverflow.griefprevention.*;
import me.tinyoverflow.griefprevention.events.*;
import me.tinyoverflow.griefprevention.logger.ActivityType;
import me.tinyoverflow.griefprevention.tasks.SecureClaimTask;
import me.tinyoverflow.griefprevention.tasks.SiegeCheckupTask;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

//singleton class which manages all GriefPrevention data (except for config options)
public abstract class DataStore
{

    //path information, for where stuff stored on disk is well...  stored
    public final static String dataLayerFolderPath = "plugins" + File.separator + "GriefPrevention";
    public final static String configFilePath = dataLayerFolderPath + File.separator + "config.yml";
    //video links
    public static final String SURVIVAL_VIDEO_URL =
            String.valueOf(ChatColor.DARK_AQUA) + ChatColor.UNDERLINE + "bit.ly/mcgpuser" + ChatColor.RESET;
    public static final String CREATIVE_VIDEO_URL =
            String.valueOf(ChatColor.DARK_AQUA) + ChatColor.UNDERLINE + "bit.ly/mcgpcrea" + ChatColor.RESET;
    public static final String SUBDIVISION_VIDEO_URL =
            String.valueOf(ChatColor.DARK_AQUA) + ChatColor.UNDERLINE + "bit.ly/mcgpsub" + ChatColor.RESET;
    //the latest version of the data schema implemented here
    final static String messagesFilePath = dataLayerFolderPath + File.separator + "messages.yml";
    static String playerDataFolderPath = dataLayerFolderPath + File.separator + "PlayerData";
    //turns a location into a string, useful in data storage
    private final String locationStringDelimiter = ";";
    //timestamp for each siege cooldown to end
    private final HashMap<String, Long> siegeCooldownRemaining = new HashMap<>();
    //in-memory cache for claim data
    public ArrayList<Claim> claims = new ArrayList<>();
    public ConcurrentHashMap<Long, ArrayList<Claim>> chunksToClaimsMap = new ConcurrentHashMap<>();
    //in-memory cache for player data
    protected ConcurrentHashMap<UUID, PlayerData> playerNameToPlayerDataMap = new ConcurrentHashMap<>();
    //in-memory cache for group (permission-based) data
    protected ConcurrentHashMap<String, Integer> permissionToBonusBlocksMap = new ConcurrentHashMap<>();
    //next claim ID
    Long nextClaimID = (long) 0;
    //in-memory cache for messages
    private String[] messages;

    //#region Chunk Hashes
    //gets an almost-unique, persistent identifier for a chunk
    public static Long getChunkHash(long chunkx, long chunkz)
    {
        return (chunkz ^ (chunkx << 32));
    }

    //gets an almost-unique, persistent identifier for a chunk
    public static Long getChunkHash(Location location)
    {
        return getChunkHash(location.getBlockX() >> 4, location.getBlockZ() >> 4);
    }

    public static ArrayList<Long> getChunkHashes(Claim claim)
    {
        return getChunkHashes(claim.getLesserBoundaryCorner(), claim.getGreaterBoundaryCorner());
    }

    public static ArrayList<Long> getChunkHashes(Location min, Location max)
    {
        ArrayList<Long> hashes = new ArrayList<>();
        int smallX = min.getBlockX() >> 4;
        int smallZ = min.getBlockZ() >> 4;
        int largeX = max.getBlockX() >> 4;
        int largeZ = max.getBlockZ() >> 4;

        for (int x = smallX; x <= largeX; x++) {
            for (int z = smallZ; z <= largeZ; z++) {
                hashes.add(getChunkHash(x, z));
            }
        }

        return hashes;
    }
    //#endregion

    //initialization!
    void initialize() throws Exception
    {
        GriefPrevention.AddLogEntry(claims.size() + " total claims loaded.");

        //RoboMWM: ensure the nextClaimID is greater than any other claim ID. If not, data corruption occurred (out of storage space, usually).
        for (Claim claim : claims) {
            if (claim.id >= nextClaimID) {
                GriefPrevention.instance.getLogger().severe(
                        "nextClaimID was lesser or equal to an already-existing claim ID!\n" +
                        "This usually happens if you ran out of storage space.");
                GriefPrevention.AddLogEntry(
                        "Changing nextClaimID from " + nextClaimID + " to " + claim.id,
                        ActivityType.DEBUG,
                        false
                );
                nextClaimID = claim.id + 1;
            }
        }

        //ensure data folders exist
        File playerDataFolder = new File(playerDataFolderPath);
        if (!playerDataFolder.exists()) {
            playerDataFolder.mkdirs();
        }

        //load up all the messages from messages.yml
        loadMessages();
        GriefPrevention.AddLogEntry("Customizable messages loaded.");
    }

    //removes cached player data from memory
    public synchronized void clearCachedPlayerData(UUID playerID)
    {
        playerNameToPlayerDataMap.remove(playerID);
    }

    //gets the number of bonus blocks a player has from his permissions
    //Bukkit doesn't allow for checking permissions of an offline player.
    //this will return 0 when he's offline, and the correct number when online.
    synchronized public int getGroupBonusBlocks(UUID playerID)
    {
        Player player = GriefPrevention.instance.getServer().getPlayer(playerID);

        if (player == null) return 0;

        int bonusBlocks = 0;

        for (Map.Entry<String, Integer> groupEntry : permissionToBonusBlocksMap.entrySet()) {
            if (player.hasPermission(groupEntry.getKey())) {
                bonusBlocks += groupEntry.getValue();
            }
        }

        return bonusBlocks;
    }

    synchronized public void changeClaimOwner(Claim claim, UUID newOwnerID)
    {
        //if it's a subdivision, throw an exception
        if (claim.parent != null) {
            throw new NoTransferException("Subdivisions can't be transferred.  Only top-level claims may change owners.");
        }

        //otherwise update information

        //determine current claim owner
        PlayerData ownerData = null;
        if (!claim.isAdminClaim()) {
            ownerData = getPlayerData(claim.ownerID);
        }

        //call event
        ClaimTransferEvent event = new ClaimTransferEvent(claim, newOwnerID);
        Bukkit.getPluginManager().callEvent(event);

        //return if event is cancelled
        if (event.isCancelled()) return;

        //determine new owner
        PlayerData newOwnerData = null;

        if (event.getNewOwner() != null) {
            newOwnerData = getPlayerData(event.getNewOwner());
        }

        //transfer
        claim.ownerID = event.getNewOwner();
        saveClaim(claim);

        //adjust blocks and other records
        if (ownerData != null) {
            ownerData.getClaims().remove(claim);
        }

        if (newOwnerData != null) {
            newOwnerData.getClaims().add(claim);
        }
    }

    //adds a claim to the datastore, making it an effective claim
    synchronized void addClaim(Claim newClaim, boolean writeToStorage)
    {
        //subdivisions are added under their parent, not directly to the hash map for direct search
        if (newClaim.parent != null) {
            if (!newClaim.parent.children.contains(newClaim)) {
                newClaim.parent.children.add(newClaim);
            }
            newClaim.inDataStore = true;
            if (writeToStorage) {
                saveClaim(newClaim);
            }
            return;
        }

        //add it and mark it as added
        claims.add(newClaim);
        addToChunkClaimMap(newClaim);

        newClaim.inDataStore = true;

        //except for administrative claims (which have no owner), update the owner's playerData with the new claim
        if (!newClaim.isAdminClaim() && writeToStorage) {
            PlayerData ownerData = getPlayerData(newClaim.ownerID);
            ownerData.getClaims().add(newClaim);
        }

        //make sure the claim is saved to disk
        if (writeToStorage) {
            saveClaim(newClaim);
        }
    }

    private void addToChunkClaimMap(Claim claim)
    {
        // Subclaims should not be added to chunk claim map.
        if (claim.parent != null) return;

        ArrayList<Long> chunkHashes = claim.getChunkHashes();
        for (Long chunkHash : chunkHashes) {
            ArrayList<Claim> claimsInChunk = chunksToClaimsMap.get(chunkHash);
            if (claimsInChunk == null) {
                chunksToClaimsMap.put(chunkHash, claimsInChunk = new ArrayList<>());
            }

            claimsInChunk.add(claim);
        }
    }

    private void removeFromChunkClaimMap(Claim claim)
    {
        ArrayList<Long> chunkHashes = claim.getChunkHashes();
        for (Long chunkHash : chunkHashes) {
            ArrayList<Claim> claimsInChunk = chunksToClaimsMap.get(chunkHash);
            if (claimsInChunk != null) {
                for (Iterator<Claim> it = claimsInChunk.iterator(); it.hasNext(); ) {
                    Claim c = it.next();
                    if (c.id.equals(claim.id)) {
                        it.remove();
                        break;
                    }
                }
                if (claimsInChunk.isEmpty()) { // if nothing's left, remove this chunk's cache
                    chunksToClaimsMap.remove(chunkHash);
                }
            }
        }
    }

    String locationToString(Location location)
    {
        return location.getWorld().getName() + locationStringDelimiter +
               location.getBlockX() +
               locationStringDelimiter +
               location.getBlockY() +
               locationStringDelimiter +
               location.getBlockZ();
    }

    //turns a location string back into a location
    Location locationFromString(String string, List<World> validWorlds) throws Exception
    {
        //split the input string on the space
        String[] elements = string.split(locationStringDelimiter);

        //expect four elements - world name, X, Y, and Z, respectively
        if (elements.length < 4) {
            throw new Exception("Expected four distinct parts to the location string: \"" + string + "\"");
        }

        String worldName = elements[0];
        String xString = elements[1];
        String yString = elements[2];
        String zString = elements[3];

        //identify world the claim is in
        World world = null;
        for (World w : validWorlds) {
            if (w.getName().equalsIgnoreCase(worldName)) {
                world = w;
                break;
            }
        }

        if (world == null) {
            throw new Exception("World not found: \"" + worldName + "\"");
        }

        //convert those numerical strings to integer values
        int x = Integer.parseInt(xString);
        int y = Integer.parseInt(yString);
        int z = Integer.parseInt(zString);

        return new Location(world, x, y, z);
    }

    //saves any changes to a claim to secondary storage
    synchronized public void saveClaim(Claim claim)
    {
        assignClaimID(claim);

        writeClaimToStorage(claim);
    }

    private void assignClaimID(Claim claim)
    {
        //ensure a unique identifier for the claim which will be used to name the file on disk
        if (claim.id == null || claim.id == -1) {
            claim.id = nextClaimID;
            incrementNextClaimID();
        }
    }

    abstract void writeClaimToStorage(Claim claim);

    //increments the claim ID and updates secondary storage to be sure it's saved
    abstract void incrementNextClaimID();

    //deletes a claim or subdivision
    synchronized public void deleteClaim(Claim claim, boolean releasePets)
    {
        deleteClaim(claim, true, releasePets);
    }

    public synchronized void deleteClaim(Claim claim, boolean fireEvent, boolean releasePets)
    {
        //delete any children
        for (int j = 1; (j - 1) < claim.children.size(); j++) {
            deleteClaim(claim.children.get(j - 1), true);
        }

        //subdivisions must also be removed from the parent claim child list
        if (claim.parent != null) {
            Claim parentClaim = claim.parent;
            parentClaim.children.remove(claim);
        }

        //mark as deleted so any references elsewhere can be ignored
        claim.inDataStore = false;

        //remove from memory
        for (int i = 0; i < claims.size(); i++) {
            if (claims.get(i).id.equals(claim.id)) {
                claims.remove(i);
                break;
            }
        }

        removeFromChunkClaimMap(claim);

        //remove from secondary storage
        deleteClaimFromSecondaryStorage(claim);

        //update player data
        if (claim.ownerID != null) {
            PlayerData ownerData = getPlayerData(claim.ownerID);
            for (int i = 0; i < ownerData.getClaims().size(); i++) {
                if (ownerData.getClaims().get(i).id.equals(claim.id)) {
                    ownerData.getClaims().remove(i);
                    break;
                }
            }
            savePlayerData(claim.ownerID, ownerData);
        }

        if (fireEvent) {
            ClaimDeletedEvent ev = new ClaimDeletedEvent(claim);
            Bukkit.getPluginManager().callEvent(ev);
        }

        //optionally set any pets free which belong to the claim owner
        if (releasePets && claim.ownerID != null && claim.parent == null) {
            for (Chunk chunk : claim.getChunks()) {
                Entity[] entities = chunk.getEntities();
                for (Entity entity : entities) {
                    if (entity instanceof Tameable pet) {
                        if (pet.isTamed()) {
                            AnimalTamer owner = pet.getOwner();
                            if (owner != null) {
                                UUID ownerID = owner.getUniqueId();
                                if (ownerID.equals(claim.ownerID)) {
                                    pet.setTamed(false);
                                    pet.setOwner(null);
                                    if (pet instanceof InventoryHolder holder) {
                                        holder.getInventory().clear();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    abstract void deleteClaimFromSecondaryStorage(Claim claim);

    //gets the claim at a specific location
    //ignoreHeight = TRUE means that a location UNDER an existing claim will return the claim
    //cachedClaim can be NULL, but will help performance if you have a reasonable guess about which claim the location is in
    synchronized public Claim getClaimAt(Location location, boolean ignoreHeight, Claim cachedClaim)
    {
        return getClaimAt(location, ignoreHeight, false, cachedClaim);
    }

    /**
     * Get the claim at a specific location.
     *
     * <p>The cached claim may be null, but will increase performance if you have a reasonable idea
     * of which claim is correct.
     *
     * @param location        the location
     * @param ignoreHeight    whether to check containment vertically
     * @param ignoreSubclaims whether subclaims should be returned over claims
     * @param cachedClaim     the cached claim, if any
     * @return the claim containing the location or null if no claim exists there
     */
    synchronized public Claim getClaimAt(Location location, boolean ignoreHeight, boolean ignoreSubclaims, Claim cachedClaim)
    {
        //check cachedClaim guess first.  if it's in the datastore and the location is inside it, we're done
        if (cachedClaim != null && cachedClaim.inDataStore &&
            cachedClaim.contains(location, ignoreHeight, !ignoreSubclaims))
        {
            return cachedClaim;
        }

        //find a top level claim
        Long chunkID = getChunkHash(location);
        ArrayList<Claim> claimsInChunk = chunksToClaimsMap.get(chunkID);
        if (claimsInChunk == null) return null;

        for (Claim claim : claimsInChunk) {
            if (claim.inDataStore && claim.contains(location, ignoreHeight, false)) {
                // If ignoring subclaims, claim is a match.
                if (ignoreSubclaims) return claim;

                //when we find a top level claim, if the location is in one of its subdivisions,
                //return the SUBDIVISION, not the top level claim
                for (int j = 0; j < claim.children.size(); j++) {
                    Claim subdivision = claim.children.get(j);
                    if (subdivision.inDataStore && subdivision.contains(location, ignoreHeight, false)) {
                        return subdivision;
                    }
                }

                return claim;
            }
        }

        //if no claim found, return null
        return null;
    }

    //finds a claim by ID
    public synchronized Claim getClaim(long id)
    {
        for (Claim claim : claims) {
            if (claim.inDataStore) {
                if (claim.getID() == id) {
                    return claim;
                }
                for (Claim subClaim : claim.children) {
                    if (subClaim.getID() == id) {
                        return subClaim;
                    }
                }
            }
        }

        return null;
    }

    //returns a read-only access point for the list of all land claims
    //if you need to make changes, use provided methods like .deleteClaim() and .createClaim().
    //this will ensure primary memory (RAM) and secondary memory (disk, database) stay in sync
    public Collection<Claim> getClaims()
    {
        return Collections.unmodifiableCollection(claims);
    }

    public Collection<Claim> getClaims(int chunkx, int chunkz)
    {
        ArrayList<Claim> chunkClaims = chunksToClaimsMap.get(getChunkHash(chunkx, chunkz));
        return Collections.unmodifiableCollection(Objects.requireNonNullElseGet(chunkClaims, ArrayList::new));
    }

    /*
     * Creates a claim and flags it as being new....throwing a create claim event;
     */
    synchronized public CreateClaimResult createClaim(World world, int x1, int x2, int y1, int y2, int z1, int z2, UUID ownerID, Claim parent, Long id, Player creatingPlayer)
    {
        return createClaim(world, x1, x2, y1, y2, z1, z2, ownerID, parent, id, creatingPlayer, false);
    }

    //creates a claim.
    //if the new claim would overlap an existing claim, returns a failure along with a reference to the existing claim
    //if the new claim would overlap a WorldGuard region where the player doesn't have permission to build, returns a failure with NULL for claim
    //otherwise, returns a success along with a reference to the new claim
    //use ownerName == "" for administrative claims
    //for top level claims, pass parent == NULL
    //DOES adjust claim blocks available on success (players can go into negative quantity available)
    //DOES check for world guard regions where the player doesn't have permission
    //does NOT check a player has permission to create a claim, or enough claim blocks.
    //does NOT check minimum claim size constraints
    //does NOT visualize the new claim for any players
    synchronized public CreateClaimResult createClaim(World world, int x1, int x2, int y1, int y2, int z1, int z2, UUID ownerID, Claim parent, Long id, Player creatingPlayer, boolean dryRun)
    {
        CreateClaimResult result = new CreateClaimResult();

        int smallx, bigx, smally, bigy, smallz, bigz;

        int worldMinY = world.getMinHeight();
        y1 = Math.max(
                worldMinY,
                Math.max(
                        GriefPrevention.instance.getPluginConfig()
                                                .getClaimConfiguration()
                                                .getCreationConfiguration().maximumDepth,
                        y1
                )
        );
        y2 = Math.max(
                worldMinY,
                Math.max(
                        GriefPrevention.instance.getPluginConfig()
                                                .getClaimConfiguration()
                                                .getCreationConfiguration().maximumDepth,
                        y2
                )
        );

        //determine small versus big inputs
        if (x1 < x2) {
            smallx = x1;
            bigx = x2;
        }
        else {
            smallx = x2;
            bigx = x1;
        }

        if (y1 < y2) {
            smally = y1;
            bigy = y2;
        }
        else {
            smally = y2;
            bigy = y1;
        }

        if (z1 < z2) {
            smallz = z1;
            bigz = z2;
        }
        else {
            smallz = z2;
            bigz = z1;
        }

        if (parent != null) {
            Location lesser = parent.getLesserBoundaryCorner();
            Location greater = parent.getGreaterBoundaryCorner();
            if (smallx < lesser.getX() || smallz < lesser.getZ() || bigx > greater.getX() || bigz > greater.getZ()) {
                result.succeeded = false;
                result.claim = parent;
                return result;
            }
            smally = sanitizeClaimDepth(parent, smally);
        }

        //creative mode claims always go to bedrock
        if (GriefPrevention.instance.getPluginConfig().getClaimConfiguration().getWorldMode(world) ==
            ClaimsMode.Creative)
        {
            smally = world.getMinHeight();
        }

        //create a new claim instance (but don't save it, yet)
        Claim newClaim = new Claim(
                new Location(world, smallx, smally, smallz),
                new Location(world, bigx, bigy, bigz),
                ownerID,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                id
        );

        newClaim.parent = parent;

        //ensure this new claim won't overlap any existing claims
        ArrayList<Claim> claimsToCheck;
        if (newClaim.parent != null) {
            claimsToCheck = newClaim.parent.children;
        }
        else {
            claimsToCheck = claims;
        }

        for (Claim otherClaim : claimsToCheck) {
            //if we find an existing claim which will be overlapped
            if (!Objects.equals(otherClaim.id, newClaim.id) && otherClaim.inDataStore &&
                otherClaim.overlaps(newClaim))
            {
                //result = fail, return conflicting claim
                result.succeeded = false;
                result.claim = otherClaim;
                return result;
            }
        }

        if (dryRun) {
            // since this is a dry run, just return the unsaved claim as is.
            result.succeeded = true;
            result.claim = newClaim;
            return result;
        }

        assignClaimID(newClaim); // assign a claim ID before calling event, in case a plugin wants to know the ID.
        ClaimCreatedEvent event = new ClaimCreatedEvent(newClaim, creatingPlayer);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            result.succeeded = false;
            result.claim = null;
            return result;
        }
        //otherwise add this new claim to the data store to make it effective
        addClaim(newClaim, true);

        //then return success along with reference to new claim
        result.succeeded = true;
        result.claim = newClaim;
        return result;
    }

    //#region Loading/Saving Player Data
    //retrieves player data from memory or secondary storage, as necessary
    //if the player has never been on the server before, this will return a fresh player data with default values
    synchronized public PlayerData getPlayerData(UUID playerID)
    {
        //first, look in memory
        PlayerData playerData = playerNameToPlayerDataMap.get(playerID);

        //if not there, build a fresh instance with some blanks for what may be in secondary storage
        if (playerData == null) {
            playerData = new PlayerData();
            playerData.playerID = playerID;

            //shove that new player data into the hash map cache
            playerNameToPlayerDataMap.put(playerID, playerData);
        }

        return playerData;
    }

    synchronized public PlayerData getPlayerData(OfflinePlayer player)
    {
        return getPlayerData(player.getUniqueId());
    }

    public abstract PlayerData getPlayerDataFromStorage(UUID playerID);

    //saves changes to player data to secondary storage.  MUST be called after you're done making changes, otherwise a reload will lose them
    public void savePlayerDataSync(UUID playerID, PlayerData playerData)
    {
        //ensure player data is already read from file before trying to save
        playerData.getAccruedClaimBlocks();
        playerData.getClaims();

        asyncSavePlayerData(playerID, playerData);
    }

    //saves changes to player data to secondary storage.  MUST be called after you're done making changes, otherwise a reload will lose them
    public void savePlayerData(UUID playerID, PlayerData playerData)
    {
        new SavePlayerDataThread(playerID, playerData).start();
    }

    public void asyncSavePlayerData(UUID playerID, PlayerData playerData)
    {
        //save everything except the ignore list
        overrideSavePlayerData(playerID, playerData);

        //save the ignore list
        if (playerData.ignoreListChanged) {
            StringBuilder fileContent = new StringBuilder();
            try {
                for (UUID uuidKey : playerData.ignoredPlayers.keySet()) {
                    Boolean value = playerData.ignoredPlayers.get(uuidKey);
                    if (value == null) continue;

                    //admin-enforced ignores begin with an asterisk
                    if (value) {
                        fileContent.append("*");
                    }

                    fileContent.append(uuidKey);
                    fileContent.append("\n");
                }

                //write data to file
                File playerDataFile = new File(playerDataFolderPath + File.separator + playerID + ".ignore");
                Files.write(fileContent.toString().trim().getBytes(StandardCharsets.UTF_8), playerDataFile);
            }

            //if any problem, log it
            catch (Exception e) {
                GriefPrevention.AddLogEntry(
                        "GriefPrevention: Unexpected exception saving data for player \"" + playerID.toString() +
                        "\": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    abstract void overrideSavePlayerData(UUID playerID, PlayerData playerData);
    //#endregion

    //extends a claim to a new depth
    //respects the max depth config variable
    synchronized public void extendClaim(Claim claim, int newDepth)
    {
        if (claim.parent != null) claim = claim.parent;

        newDepth = sanitizeClaimDepth(claim, newDepth);

        //call event and return if event got cancelled
        ClaimExtendEvent event = new ClaimExtendEvent(claim, newDepth);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return;

        //adjust to new depth
        setNewDepth(claim, event.getNewDepth());
    }

    /**
     * Helper method for sanitizing claim depth to find the minimum expected value.
     *
     * @param claim    the claim
     * @param newDepth the new depth
     * @return the sanitized new depth
     */
    private int sanitizeClaimDepth(Claim claim, int newDepth)
    {
        if (claim.parent != null) claim = claim.parent;

        // Get the old depth including the depth of the lowest subdivision.
        int oldDepth = Math.min(
                claim.getLesserBoundaryCorner().getBlockY(),
                claim.children.stream().mapToInt(child -> child.getLesserBoundaryCorner().getBlockY())
                                       .min().orElse(Integer.MAX_VALUE)
        );

        // Use the lowest of the old and new depths.
        newDepth = Math.min(newDepth, oldDepth);
        // Cap depth to maximum depth allowed by the configuration.
        newDepth = Math.max(
                newDepth,
                GriefPrevention.instance.getPluginConfig()
                                        .getClaimConfiguration()
                                        .getCreationConfiguration().maximumDepth
        );
        // Cap the depth to the world's minimum height.
        World world = Objects.requireNonNull(claim.getLesserBoundaryCorner().getWorld());
        newDepth = Math.max(newDepth, world.getMinHeight());

        return newDepth;
    }

    /**
     * Helper method for sanitizing and setting claim depth. Saves affected claims.
     *
     * @param claim    the claim
     * @param newDepth the new depth
     */
    private void setNewDepth(Claim claim, int newDepth)
    {
        if (claim.parent != null) claim = claim.parent;

        final int depth = sanitizeClaimDepth(claim, newDepth);

        Stream.concat(Stream.of(claim), claim.children.stream()).forEach(localClaim ->
        {
            localClaim.lesserBoundaryCorner.setY(depth);
            localClaim.greaterBoundaryCorner.setY(Math.max(localClaim.greaterBoundaryCorner.getBlockY(), depth));
            saveClaim(localClaim);
        });
    }

    //#region Siege
    //starts a siege on a claim
    //does NOT check siege cooldowns, see onCooldown() below
    synchronized public void startSiege(Player attacker, Player defender, Claim defenderClaim)
    {
        //fill-in the necessary SiegeData instance
        SiegeData siegeData = new SiegeData(attacker, defender, defenderClaim);
        PlayerData attackerData = getPlayerData(attacker.getUniqueId());
        PlayerData defenderData = getPlayerData(defender.getUniqueId());
        attackerData.siegeData = siegeData;
        defenderData.siegeData = siegeData;
        defenderClaim.siegeData = siegeData;

        //start a task to monitor the siege
        //why isn't this a "repeating" task?
        //because depending on the status of the siege at the time the task runs, there may or may not be a reason to run the task again
        SiegeCheckupTask task = new SiegeCheckupTask(siegeData);
        siegeData.checkupTaskID = GriefPrevention.instance.getServer().getScheduler().scheduleSyncDelayedTask(
                GriefPrevention.instance,
                task,
                20L * 30
        );
    }

    //ends a siege
    //either winnerName or loserName can be null, but not both
    synchronized public void endSiege(SiegeData siegeData, String winnerName, String loserName, List<ItemStack> drops)
    {
        boolean grantAccess = false;

        //determine winner and loser
        if (winnerName == null && loserName != null) {
            if (siegeData.attacker.getName().equals(loserName)) {
                winnerName = siegeData.defender.getName();
            }
            else {
                winnerName = siegeData.attacker.getName();
            }
        }
        else if (winnerName != null && loserName == null) {
            if (siegeData.attacker.getName().equals(winnerName)) {
                loserName = siegeData.defender.getName();
            }
            else {
                loserName = siegeData.attacker.getName();
            }
        }

        //if the attacker won, plan to open the doors for looting
        if (siegeData.attacker.getName().equals(winnerName)) {
            grantAccess = true;
        }

        PlayerData attackerData = getPlayerData(siegeData.attacker.getUniqueId());
        attackerData.siegeData = null;

        PlayerData defenderData = getPlayerData(siegeData.defender.getUniqueId());
        defenderData.siegeData = null;
        defenderData.lastSiegeEndTimeStamp = System.currentTimeMillis();

        //start a cooldown for this attacker/defender pair
        long now = Calendar.getInstance().getTimeInMillis();
        long cooldownEnd = now + 1000L * 60 *
                                 GriefPrevention.instance.getPluginConfig()
                                                         .getSiegeConfiguration()
                                                         .getCooldownEnd();  //one hour from now
        siegeCooldownRemaining.put(siegeData.attacker.getName() + "_" + siegeData.defender.getName(), cooldownEnd);

        //start cooldowns for every attacker/involved claim pair
        for (int i = 0; i < siegeData.claims.size(); i++) {
            Claim claim = siegeData.claims.get(i);
            claim.siegeData = null;
            siegeCooldownRemaining.put(siegeData.attacker.getName() + "_" + claim.getOwnerName(), cooldownEnd);

            //if doors should be opened for looting, do that now
            if (grantAccess) {
                claim.doorsOpen = true;
            }
        }

        //cancel the siege checkup task
        GriefPrevention.instance.getServer().getScheduler().cancelTask(siegeData.checkupTaskID);

        //notify everyone who won and lost
        if (winnerName != null && loserName != null) {
            GriefPrevention.instance.getServer().broadcastMessage(
                    winnerName + " defeated " + loserName + " in siege warfare!");
        }

        //if the claim should be opened to looting
        if (grantAccess) {

            Player winner = GriefPrevention.instance.getServer().getPlayer(winnerName);
            if (winner != null) {
                //notify the winner
                GriefPrevention.sendMessage(winner, TextMode.SUCCESS, Messages.SiegeWinDoorsOpen);

                //schedule a task to secure the claims in about 5 minutes
                SecureClaimTask task = new SecureClaimTask(siegeData);

                GriefPrevention.instance.getServer().getScheduler().scheduleSyncDelayedTask(
                        GriefPrevention.instance,
                        task,
                        20L * GriefPrevention.instance.getPluginConfig().getSiegeConfiguration().getDoorsOpenDelay()
                );
            }
        }

        //if the siege ended due to death, transfer inventory to winner
        if (drops != null) {
            Player winner = GriefPrevention.instance.getServer().getPlayer(winnerName);
            Player loser = GriefPrevention.instance.getServer().getPlayer(loserName);

            if (winner != null && loser != null) {
                //try to add any drops to the winner's inventory
                for (ItemStack stack : drops) {
                    if (stack == null || stack.getType() == Material.AIR || stack.getAmount() == 0) continue;

                    HashMap<Integer, ItemStack> wontFitItems = winner.getInventory().addItem(stack);

                    //drop any remainder on the ground at his feet
                    Location winnerLocation = winner.getLocation();
                    for (Map.Entry<Integer, ItemStack> wontFitItem : wontFitItems.entrySet()) {
                        winner.getWorld().dropItemNaturally(winnerLocation, wontFitItem.getValue());
                    }
                }

                drops.clear();
            }
        }
    }

    /**
     * whether a sieger can siege a particular victim or claim, considering only cooldowns
     */
    synchronized public boolean onCooldown(Player attacker, Player defender, Claim defenderClaim)
    {
        Long cooldownEnd = null;

        //look for an attacker/defender cooldown
        if (siegeCooldownRemaining.get(attacker.getName() + "_" + defender.getName()) != null) {
            cooldownEnd = siegeCooldownRemaining.get(attacker.getName() + "_" + defender.getName());

            if (Calendar.getInstance().getTimeInMillis() < cooldownEnd) {
                return true;
            }

            //if found but expired, remove it
            siegeCooldownRemaining.remove(attacker.getName() + "_" + defender.getName());
        }

        //look for genderal defender cooldown
        PlayerData defenderData = getPlayerData(defender.getUniqueId());
        if (defenderData.lastSiegeEndTimeStamp > 0) {
            long now = System.currentTimeMillis();
            if (now - defenderData.lastSiegeEndTimeStamp > 1000 * 60 * 15) //15 minutes in milliseconds
            {
                return true;
            }
        }

        //look for an attacker/claim cooldown
        if (cooldownEnd == null &&
            siegeCooldownRemaining.get(attacker.getName() + "_" + defenderClaim.getOwnerName()) != null)
        {
            cooldownEnd = siegeCooldownRemaining.get(attacker.getName() + "_" + defenderClaim.getOwnerName());

            if (Calendar.getInstance().getTimeInMillis() < cooldownEnd) {
                return true;
            }

            //if found but expired, remove it
            siegeCooldownRemaining.remove(attacker.getName() + "_" + defenderClaim.getOwnerName());
        }

        return false;
    }

    //extend a siege, if it's possible to do so
    public synchronized void tryExtendSiege(Player player, Claim claim)
    {
        PlayerData playerData = getPlayerData(player.getUniqueId());

        //player must be sieged
        if (playerData.siegeData == null) return;

        //claim isn't already under the same siege
        if (playerData.siegeData.claims.contains(claim)) return;

        //admin claims can't be sieged
        if (claim.isAdminClaim()) return;

        //player must have some level of permission to be sieged in a claim
        Claim currentClaim = claim;
        while (!currentClaim.hasExplicitPermission(player, ClaimPermission.Access)) {
            if (currentClaim.parent == null) return;
            currentClaim = currentClaim.parent;
        }

        //otherwise extend the siege
        playerData.siegeData.claims.add(claim);
        claim.siegeData = playerData.siegeData;
    }
    //#endregion

    //deletes all claims owned by a player
    synchronized public void deleteClaimsForPlayer(UUID playerID, boolean releasePets)
    {
        //make a list of the player's claims
        ArrayList<Claim> claimsToDelete = new ArrayList<>();
        for (Claim claim : claims) {
            if ((Objects.equals(playerID, claim.ownerID))) {
                claimsToDelete.add(claim);
            }
        }

        //delete them one by one
        for (Claim claim : claimsToDelete) {
            claim.removeSurfaceFluids(null);

            deleteClaim(claim, releasePets);

            //if in a creative mode world, delete the claim
            if (GriefPrevention.instance.creativeRulesApply(claim.getLesserBoundaryCorner())) {
                GriefPrevention.instance.restoreClaim(claim, 0);
            }
        }
    }

    //tries to resize a claim
    //see CreateClaim() for details on return value
    synchronized public CreateClaimResult resizeClaim(Claim claim, int newx1, int newx2, int newy1, int newy2, int newz1, int newz2, Player resizingPlayer)
    {
        //try to create this new claim, ignoring the original when checking for overlap
        CreateClaimResult result = createClaim(
                claim.getLesserBoundaryCorner().getWorld(),
                newx1,
                newx2,
                newy1,
                newy2,
                newz1,
                newz2,
                claim.ownerID,
                claim.parent,
                claim.id,
                resizingPlayer,
                true
        );

        //if succeeded
        if (result.succeeded) {
            removeFromChunkClaimMap(claim); // remove the old boundary from the chunk cache
            // copy the boundary from the claim created in the dry run of createClaim() to our existing claim
            claim.lesserBoundaryCorner = result.claim.lesserBoundaryCorner;
            claim.greaterBoundaryCorner = result.claim.greaterBoundaryCorner;
            // Sanitize claim depth, expanding parent down to the lowest subdivision and subdivisions down to parent.
            // Also saves affected claims.
            setNewDepth(claim, claim.getLesserBoundaryCorner().getBlockY());
            result.claim = claim;
            addToChunkClaimMap(claim); // add the new boundary to the chunk cache
        }

        return result;
    }

    public void resizeClaimWithChecks(Player player, PlayerData playerData, int newx1, int newx2, int newy1, int newy2, int newz1, int newz2)
    {
        //for top level claims, apply size rules and claim blocks requirement
        if (playerData.claimResizing.parent == null) {
            //measure new claim, apply size rules
            int newWidth = (Math.abs(newx1 - newx2) + 1);
            int newHeight = (Math.abs(newz1 - newz2) + 1);
            boolean smaller =
                    newWidth < playerData.claimResizing.getWidth() || newHeight < playerData.claimResizing.getHeight();

            if (!player.hasPermission("griefprevention.adminclaims") && !playerData.claimResizing.isAdminClaim() &&
                smaller)
            {
                if (newWidth <
                    GriefPrevention.instance.getPluginConfig()
                                            .getClaimConfiguration()
                                            .getCreationConfiguration().minimumWidth ||
                    newHeight <
                    GriefPrevention.instance.getPluginConfig()
                                            .getClaimConfiguration()
                                            .getCreationConfiguration().minimumWidth)
                {
                    GriefPrevention.sendMessage(
                            player,
                            TextMode.ERROR,
                            Messages.ResizeClaimTooNarrow,
                            String.valueOf(GriefPrevention.instance.getPluginConfig()
                                                                   .getClaimConfiguration()
                                                                   .getCreationConfiguration().minimumWidth)
                    );
                    return;
                }

                int newArea = newWidth * newHeight;
                if (newArea <
                    GriefPrevention.instance.getPluginConfig()
                                            .getClaimConfiguration()
                                            .getCreationConfiguration().minimumArea)
                {
                    GriefPrevention.sendMessage(
                            player,
                            TextMode.ERROR,
                            Messages.ResizeClaimInsufficientArea,
                            String.valueOf(GriefPrevention.instance.getPluginConfig()
                                                                   .getClaimConfiguration()
                                                                   .getCreationConfiguration().minimumArea)
                    );
                    return;
                }
            }

            //make sure player has enough blocks to make up the difference
            if (!playerData.claimResizing.isAdminClaim() &&
                player.getName().equals(playerData.claimResizing.getOwnerName()))
            {
                int newArea = newWidth * newHeight;
                int blocksRemainingAfter =
                        playerData.getRemainingClaimBlocks() + playerData.claimResizing.getArea() - newArea;

                if (blocksRemainingAfter < 0) {
                    GriefPrevention.sendMessage(
                            player,
                            TextMode.ERROR,
                            Messages.ResizeNeedMoreBlocks,
                            String.valueOf(Math.abs(blocksRemainingAfter))
                    );
                    tryAdvertiseAdminAlternatives(player);
                    return;
                }
            }
        }

        Claim oldClaim = playerData.claimResizing;
        Claim newClaim = new Claim(oldClaim);
        World world = newClaim.getLesserBoundaryCorner().getWorld();
        newClaim.lesserBoundaryCorner = new Location(world, newx1, newy1, newz1);
        newClaim.greaterBoundaryCorner = new Location(world, newx2, newy2, newz2);

        //call event here to check if it has been cancelled
        ClaimResizeEvent event = new ClaimModifiedEvent(
                oldClaim,
                newClaim,
                player
        ); // Swap to ClaimResizeEvent when ClaimModifiedEvent is removed
        Bukkit.getPluginManager().callEvent(event);

        //return here if event is cancelled
        if (event.isCancelled()) return;

        //special rule for making a top-level claim smaller.  to check this, verifying the old claim's corners are inside the new claim's boundaries.
        //rule: in any mode, shrinking a claim removes any surface fluids
        boolean smaller = false;
        if (oldClaim.parent == null) {
            //if the new claim is smaller
            if (!newClaim.contains(oldClaim.getLesserBoundaryCorner(), true, false) ||
                !newClaim.contains(oldClaim.getGreaterBoundaryCorner(), true, false))
            {
                smaller = true;

                //remove surface fluids about to be unclaimed
                oldClaim.removeSurfaceFluids(newClaim);
            }
        }

        //ask the datastore to try and resize the claim, this checks for conflicts with other claims
        CreateClaimResult result = GriefPrevention.instance.dataStore.resizeClaim(
                playerData.claimResizing,
                newClaim.getLesserBoundaryCorner().getBlockX(),
                newClaim.getGreaterBoundaryCorner().getBlockX(),
                newClaim.getLesserBoundaryCorner().getBlockY(),
                newClaim.getGreaterBoundaryCorner().getBlockY(),
                newClaim.getLesserBoundaryCorner().getBlockZ(),
                newClaim.getGreaterBoundaryCorner().getBlockZ(),
                player
        );

        if (result.succeeded && result.claim != null) {
            //decide how many claim blocks are available for more resizing
            int claimBlocksRemaining = 0;
            if (!playerData.claimResizing.isAdminClaim()) {
                UUID ownerID = playerData.claimResizing.ownerID;
                if (playerData.claimResizing.parent != null) {
                    ownerID = playerData.claimResizing.parent.ownerID;
                }
                if (ownerID == player.getUniqueId()) {
                    claimBlocksRemaining = playerData.getRemainingClaimBlocks();
                }
                else {
                    PlayerData ownerData = getPlayerData(ownerID);
                    claimBlocksRemaining = ownerData.getRemainingClaimBlocks();
                    OfflinePlayer owner = GriefPrevention.instance.getServer().getOfflinePlayer(ownerID);
                    if (!owner.isOnline()) {
                        clearCachedPlayerData(ownerID);
                    }
                }
            }

            //inform about success, visualize, communicate remaining blocks available
            GriefPrevention.sendMessage(
                    player,
                    TextMode.SUCCESS,
                    Messages.ClaimResizeSuccess,
                    String.valueOf(claimBlocksRemaining)
            );
            BoundaryVisualization.visualizeClaim(player, result.claim, VisualizationType.CLAIM);

            //if resizing someone else's claim, make a log entry
            if (!player.getUniqueId().equals(playerData.claimResizing.ownerID) &&
                playerData.claimResizing.parent == null)
            {
                GriefPrevention.AddLogEntry(
                        player.getName() + " resized " + playerData.claimResizing.getOwnerName() + "'s claim at " +
                        GriefPrevention.getFriendlyLocationString(playerData.claimResizing.lesserBoundaryCorner) + ".");
            }

            //if increased to a sufficiently large size and no subdivisions yet, send subdivision instructions
            if (oldClaim.getArea() < 1000 && result.claim.getArea() >= 1000 && result.claim.children.isEmpty() &&
                !player.hasPermission("griefprevention.adminclaims"))
            {
                GriefPrevention.sendMessage(player, TextMode.INFO, Messages.BecomeMayor, 200L);
                GriefPrevention.sendMessage(
                        player,
                        TextMode.INSTRUCTION,
                        Messages.SubdivisionVideo2,
                        201L,
                        DataStore.SUBDIVISION_VIDEO_URL
                );
            }

            //if in a creative mode world and shrinking an existing claim, restore any unclaimed area
            if (smaller && GriefPrevention.instance.creativeRulesApply(oldClaim.getLesserBoundaryCorner())) {
                GriefPrevention.sendMessage(player, TextMode.WARNING, Messages.UnclaimCleanupWarning);
                GriefPrevention.instance.restoreClaim(oldClaim, 20L * 60 * 2);  //2 minutes
                GriefPrevention.AddLogEntry(player.getName() + " shrank a claim @ " +
                                            GriefPrevention.getFriendlyLocationString(playerData.claimResizing.getLesserBoundaryCorner()));
            }

            //clean up
            playerData.claimResizing = null;
            playerData.lastShovelLocation = null;
        }
        else {
            if (result.claim != null) {
                //inform player
                GriefPrevention.sendMessage(player, TextMode.ERROR, Messages.ResizeFailOverlap);

                //show the player the conflicting claim
                BoundaryVisualization.visualizeClaim(player, result.claim, VisualizationType.CONFLICT_ZONE);
            }
            else {
                GriefPrevention.sendMessage(player, TextMode.ERROR, Messages.ResizeFailOverlapRegion);
            }
        }
    }

    //educates a player about /adminclaims and /acb, if he can use them
    public void tryAdvertiseAdminAlternatives(Player player)
    {
        if (player.hasPermission("griefprevention.adminclaims") &&
            player.hasPermission("griefprevention.adjustclaimblocks"))
        {
            GriefPrevention.sendMessage(player, TextMode.INFO, Messages.AdvertiseACandACB);
        }
        else if (player.hasPermission("griefprevention.adminclaims")) {
            GriefPrevention.sendMessage(player, TextMode.INFO, Messages.AdvertiseAdminClaims);
        }
        else if (player.hasPermission("griefprevention.adjustclaimblocks")) {
            GriefPrevention.sendMessage(player, TextMode.INFO, Messages.AdvertiseACB);
        }
    }

    //#region Messaging
    public void loadMessages()
    {
        Messages[] messageIDs = Messages.values();
        messages = new String[Messages.values().length];

        HashMap<String, CustomizableMessage> defaults = new HashMap<>();

        //initialize defaults
        addDefault(defaults, Messages.RespectingClaims, "Now respecting claims.", null);
        addDefault(defaults, Messages.IgnoringClaims, "Now ignoring claims.", null);
        addDefault(
                defaults,
                Messages.NoCreativeUnClaim,
                "You can't unclaim this land.  You can only make this claim larger or create additional claims.",
                null
        );
        addDefault(
                defaults,
                Messages.SuccessfulAbandon,
                "Claims abandoned.  You now have {0} available claim blocks.",
                "0: remaining blocks"
        );
        addDefault(
                defaults,
                Messages.RestoreNatureActivate,
                "Ready to restore some nature!  Right click to restore nature, and use /BasicClaims to stop.",
                null
        );
        addDefault(
                defaults,
                Messages.RestoreNatureAggressiveActivate,
                "Aggressive mode activated.  Do NOT use this underneath anything you want to keep!  Right click to aggressively restore nature, and use /BasicClaims to stop.",
                null
        );
        addDefault(
                defaults,
                Messages.FillModeActive,
                "Fill mode activated with radius {0}.  Right click an area to fill.",
                "0: fill radius"
        );
        addDefault(
                defaults,
                Messages.TransferClaimPermission,
                "That command requires the administrative claims permission.",
                null
        );
        addDefault(
                defaults,
                Messages.TransferClaimMissing,
                "There's no claim here.  Stand in the administrative claim you want to transfer.",
                null
        );
        addDefault(
                defaults,
                Messages.TransferClaimAdminOnly,
                "Only administrative claims may be transferred to a player.",
                null
        );
        addDefault(defaults, Messages.PlayerNotFound2, "No player by that name has logged in recently.", null);
        addDefault(
                defaults,
                Messages.TransferTopLevel,
                "Only top level claims (not subdivisions) may be transferred.  Stand outside of the subdivision and try again.",
                null
        );
        addDefault(defaults, Messages.TransferSuccess, "Claim transferred.", null);
        addDefault(defaults, Messages.TrustListNoClaim, "Stand inside the claim you're curious about.", null);
        addDefault(defaults, Messages.ClearPermsOwnerOnly, "Only the claim owner can clear all permissions.", null);
        addDefault(
                defaults,
                Messages.UntrustIndividualAllClaims,
                "Revoked {0}'s access to ALL your claims.  To set permissions for a single claim, stand inside it.",
                "0: untrusted player"
        );
        addDefault(
                defaults,
                Messages.UntrustEveryoneAllClaims,
                "Cleared permissions in ALL your claims.  To set permissions for a single claim, stand inside it.",
                null
        );
        addDefault(
                defaults,
                Messages.NoPermissionTrust,
                "You don't have {0}'s permission to manage permissions here.",
                "0: claim owner's name"
        );
        addDefault(
                defaults,
                Messages.ClearPermissionsOneClaim,
                "Cleared permissions in this claim.  To set permission for ALL your claims, stand outside them.",
                null
        );
        addDefault(
                defaults,
                Messages.UntrustIndividualSingleClaim,
                "Revoked {0}'s access to this claim.  To set permissions for a ALL your claims, stand outside them.",
                "0: untrusted player"
        );
        addDefault(defaults, Messages.OnlySellBlocks, "Claim blocks may only be sold, not purchased.", null);
        addDefault(
                defaults,
                Messages.BlockPurchaseCost,
                "Each claim block costs {0}.  Your balance is {1}.",
                "0: cost of one block; 1: player's account balance"
        );
        addDefault(
                defaults,
                Messages.ClaimBlockLimit,
                "You've reached your claim block limit.  You can't purchase more.",
                null
        );
        addDefault(
                defaults,
                Messages.InsufficientFunds,
                "You don't have enough money.  You need {0}, but you only have {1}.",
                "0: total cost; 1: player's account balance"
        );
        addDefault(
                defaults,
                Messages.MaxBonusReached,
                "Can't purchase {0} more claim blocks. The server has a limit of {1} bonus claim blocks.",
                "0: block count; 1: bonus claims limit"
        );
        addDefault(
                defaults,
                Messages.PurchaseConfirmation,
                "Withdrew {0} from your account.  You now have {1} available claim blocks.",
                "0: total cost; 1: remaining blocks"
        );
        addDefault(defaults, Messages.OnlyPurchaseBlocks, "Claim blocks may only be purchased, not sold.", null);
        addDefault(
                defaults,
                Messages.BlockSaleValue,
                "Each claim block is worth {0}.  You have {1} available for sale.",
                "0: block value; 1: available blocks"
        );
        addDefault(
                defaults,
                Messages.NotEnoughBlocksForSale,
                "You don't have that many claim blocks available for sale.",
                null
        );
        addDefault(
                defaults,
                Messages.BlockSaleConfirmation,
                "Deposited {0} in your account.  You now have {1} available claim blocks.",
                "0: amount deposited; 1: remaining blocks"
        );
        addDefault(
                defaults,
                Messages.AdminClaimsMode,
                "Administrative claims mode active.  Any claims created will be free and editable by other administrators.",
                null
        );
        addDefault(defaults, Messages.BasicClaimsMode, "Returned to basic claim creation mode.", null);
        addDefault(
                defaults,
                Messages.SubdivisionMode,
                "Subdivision mode.  Use your shovel to create subdivisions in your existing claims.  Use /basicclaims to exit.",
                null
        );
        addDefault(defaults, Messages.SubdivisionVideo2, "Click for Subdivision Help: {0}", "0:video URL");
        addDefault(defaults, Messages.DeleteClaimMissing, "There's no claim here.", null);
        addDefault(
                defaults,
                Messages.DeletionSubdivisionWarning,
                "This claim includes subdivisions.  If you're sure you want to delete it, use /DeleteClaim again.",
                null
        );
        addDefault(defaults, Messages.DeleteSuccess, "Claim deleted.", null);
        addDefault(
                defaults,
                Messages.CantDeleteAdminClaim,
                "You don't have permission to delete administrative claims.",
                null
        );
        addDefault(defaults, Messages.DeleteAllSuccess, "Deleted all of {0}'s claims.", "0: owner's name");
        addDefault(defaults, Messages.NoDeletePermission, "You don't have permission to delete claims.", null);
        addDefault(defaults, Messages.AllAdminDeleted, "Deleted all administrative claims.", null);
        addDefault(
                defaults,
                Messages.AdjustBlocksSuccess,
                "Adjusted {0}'s bonus claim blocks by {1}.  New total bonus blocks: {2}.",
                "0: player; 1: adjustment; 2: new total"
        );
        addDefault(
                defaults,
                Messages.AdjustLimitSuccess,
                "Adjusted {0}'s max accrued claim block limit to {1}.",
                "0: player; 1: new total"
        );
        addDefault(
                defaults,
                Messages.AdjustBlocksAllSuccess,
                "Adjusted all online players' bonus claim blocks by {0}.",
                "0: adjustment amount"
        );
        addDefault(defaults, Messages.NotTrappedHere, "You can build here.  Save yourself.", null);
        addDefault(
                defaults,
                Messages.RescuePending,
                "If you stay put for 10 seconds, you'll be teleported out.  Please wait.",
                null
        );
        addDefault(defaults, Messages.NonSiegeWorld, "Siege is disabled here.", null);
        addDefault(defaults, Messages.AlreadySieging, "You're already involved in a siege.", null);
        addDefault(
                defaults,
                Messages.AlreadyUnderSiegePlayer,
                "{0} is already under siege.  Join the party!",
                "0: defending player"
        );
        addDefault(defaults, Messages.NotSiegableThere, "{0} isn't protected there.", "0: defending player");
        addDefault(defaults, Messages.SiegeTooFarAway, "You're too far away to siege.", null);
        addDefault(defaults, Messages.NoSiegeYourself, "You cannot siege yourself, don't be silly", null);
        addDefault(
                defaults,
                Messages.NoSiegeDefenseless,
                "That player is defenseless.  Go pick on somebody else.",
                null
        );
        addDefault(
                defaults,
                Messages.AlreadyUnderSiegeArea,
                "That area is already under siege.  Join the party!",
                null
        );
        addDefault(defaults, Messages.NoSiegeAdminClaim, "Siege is disabled in this area.", null);
        addDefault(
                defaults,
                Messages.SiegeOnCooldown,
                "You're still on siege cooldown for this defender or claim.  Find another victim.",
                null
        );
        addDefault(
                defaults,
                Messages.SiegeAlert,
                "You're under siege!  If you log out now, you will die.  You must defeat {0}, wait for him to give up, or escape.",
                "0: attacker name"
        );
        addDefault(
                defaults,
                Messages.SiegeConfirmed,
                "The siege has begun!  If you log out now, you will die.  You must defeat {0}, chase him away, or admit defeat and walk away.",
                "0: defender name"
        );
        addDefault(
                defaults,
                Messages.AbandonClaimMissing,
                "Stand in the claim you want to delete, or consider /AbandonAllClaims.",
                null
        );
        addDefault(defaults, Messages.NotYourClaim, "This isn't your claim.", null);
        addDefault(
                defaults,
                Messages.DeleteTopLevelClaim,
                "To delete a subdivision, stand inside it.  Otherwise, use /AbandonTopLevelClaim to delete this claim and all subdivisions.",
                null
        );
        addDefault(
                defaults,
                Messages.AbandonSuccess,
                "Claim abandoned.  You now have {0} available claim blocks.",
                "0: remaining claim blocks"
        );
        addDefault(
                defaults,
                Messages.ConfirmAbandonAllClaims,
                "Are you sure you want to abandon ALL of your claims?  Please confirm with /AbandonAllClaims confirm",
                null
        );
        addDefault(
                defaults,
                Messages.CantGrantThatPermission,
                "You can't grant a permission you don't have yourself.",
                null
        );
        addDefault(
                defaults,
                Messages.GrantPermissionNoClaim,
                "Stand inside the claim where you want to grant permission.",
                null
        );
        addDefault(
                defaults,
                Messages.GrantPermissionConfirmation,
                "Granted {0} permission to {1} {2}.",
                "0: target player; 1: permission description; 2: scope (changed claims)"
        );
        addDefault(
                defaults,
                Messages.ManageUniversalPermissionsInstruction,
                "To manage permissions for ALL your claims, stand outside them.",
                null
        );
        addDefault(
                defaults,
                Messages.ManageOneClaimPermissionsInstruction,
                "To manage permissions for a specific claim, stand inside it.",
                null
        );
        addDefault(defaults, Messages.CollectivePublic, "the public", "as in 'granted the public permission to...'");
        addDefault(defaults, Messages.BuildPermission, "build", null);
        addDefault(defaults, Messages.ContainersPermission, "access containers and animals", null);
        addDefault(defaults, Messages.AccessPermission, "use buttons and levers", null);
        addDefault(defaults, Messages.PermissionsPermission, "manage permissions", null);
        addDefault(defaults, Messages.LocationCurrentClaim, "in this claim", null);
        addDefault(defaults, Messages.LocationAllClaims, "in all your claims", null);
        addDefault(
                defaults,
                Messages.PvPImmunityStart,
                "You're protected from attack by other players as long as your inventory is empty.",
                null
        );
        addDefault(defaults, Messages.SiegeNoDrop, "You can't give away items while involved in a siege.", null);
        addDefault(
                defaults,
                Messages.DonateItemsInstruction,
                "To give away the item(s) in your hand, left-click the chest again.",
                null
        );
        addDefault(defaults, Messages.ChestFull, "This chest is full.", null);
        addDefault(defaults, Messages.DonationSuccess, "Item(s) transferred to chest!", null);
        addDefault(
                defaults,
                Messages.PlayerTooCloseForFire2,
                "You can't start a fire this close to another player.",
                null
        );
        addDefault(
                defaults,
                Messages.TooDeepToClaim,
                "This chest can't be protected because it's too deep underground.  Consider moving it.",
                null
        );
        addDefault(defaults, Messages.ChestClaimConfirmation, "This chest is protected.", null);
        addDefault(
                defaults,
                Messages.AutomaticClaimNotification,
                "This chest and nearby blocks are protected from breakage and theft.",
                null
        );
        addDefault(
                defaults,
                Messages.AutomaticClaimOtherClaimTooClose,
                "Cannot create a claim for your chest, there is another claim too close!",
                null
        );
        addDefault(
                defaults,
                Messages.UnprotectedChestWarning,
                "This chest is NOT protected.  Consider using a golden shovel to expand an existing claim or to create a new one.",
                null
        );
        addDefault(defaults, Messages.ThatPlayerPvPImmune, "You can't injure defenseless players.", null);
        addDefault(
                defaults,
                Messages.CantFightWhileImmune,
                "You can't fight someone while you're protected from PvP.",
                null
        );
        addDefault(defaults, Messages.NoDamageClaimedEntity, "That belongs to {0}.", "0: owner name");
        addDefault(defaults, Messages.ShovelBasicClaimMode, "Shovel returned to basic claims mode.", null);
        addDefault(defaults, Messages.RemainingBlocks, "You may claim up to {0} more blocks.", "0: remaining blocks");
        addDefault(defaults, Messages.CreativeBasicsVideo2, "Click for Land Claim Help: {0}", "{0}: video URL");
        addDefault(defaults, Messages.SurvivalBasicsVideo2, "Click for Land Claim Help: {0}", "{0}: video URL");
        addDefault(
                defaults,
                Messages.TrappedChatKeyword,
                "trapped;stuck",
                "When mentioned in chat, players get information about the /trapped command (multiple words can be separated with semi-colons)"
        );
        addDefault(
                defaults,
                Messages.TrappedInstructions,
                "Are you trapped in someone's land claim?  Try the /trapped command.",
                null
        );
        addDefault(defaults, Messages.PvPNoDrop, "You can't drop items while in PvP combat.", null);
        addDefault(defaults, Messages.SiegeNoTeleport, "You can't teleport out of a besieged area.", null);
        addDefault(defaults, Messages.BesiegedNoTeleport, "You can't teleport into a besieged area.", null);
        addDefault(
                defaults,
                Messages.SiegeNoContainers,
                "You can't access containers while involved in a siege.",
                null
        );
        addDefault(defaults, Messages.PvPNoContainers, "You can't access containers during PvP combat.", null);
        addDefault(defaults, Messages.PvPImmunityEnd, "Now you can fight with other players.", null);
        addDefault(
                defaults,
                Messages.NoBedPermission,
                "{0} hasn't given you permission to sleep here.",
                "0: claim owner"
        );
        addDefault(
                defaults,
                Messages.NoWildernessBuckets,
                "You may only dump buckets inside your claim(s) or underground.",
                null
        );
        addDefault(
                defaults,
                Messages.NoLavaNearOtherPlayer,
                "You can't place lava this close to {0}.",
                "0: nearby player"
        );
        addDefault(defaults, Messages.TooFarAway, "That's too far away.", null);
        addDefault(defaults, Messages.BlockNotClaimed, "No one has claimed this block.", null);
        addDefault(defaults, Messages.BlockClaimed, "That block has been claimed by {0}.", "0: claim owner");
        addDefault(defaults, Messages.SiegeNoShovel, "You can't use your shovel tool while involved in a siege.", null);
        addDefault(
                defaults,
                Messages.RestoreNaturePlayerInChunk,
                "Unable to restore.  {0} is in that chunk.",
                "0: nearby player"
        );
        addDefault(defaults, Messages.NoCreateClaimPermission, "You don't have permission to claim land.", null);
        addDefault(
                defaults,
                Messages.ResizeClaimTooNarrow,
                "This new size would be too small.  Claims must be at least {0} blocks wide.",
                "0: minimum claim width"
        );
        addDefault(
                defaults,
                Messages.ResizeNeedMoreBlocks,
                "You don't have enough blocks for this size.  You need {0} more.",
                "0: how many needed"
        );
        addDefault(
                defaults,
                Messages.ClaimResizeSuccess,
                "Claim resized.  {0} available claim blocks remaining.",
                "0: remaining blocks"
        );
        addDefault(
                defaults,
                Messages.ResizeFailOverlap,
                "Can't resize here because it would overlap another nearby claim.",
                null
        );
        addDefault(
                defaults,
                Messages.ResizeStart,
                "Resizing claim.  Use your shovel again at the new location for this corner.",
                null
        );
        addDefault(
                defaults,
                Messages.ResizeFailOverlapSubdivision,
                "You can't create a subdivision here because it would overlap another subdivision.  Consider /abandonclaim to delete it, or use your shovel at a corner to resize it.",
                null
        );
        addDefault(
                defaults,
                Messages.SubdivisionStart,
                "Subdivision corner set!  Use your shovel at the location for the opposite corner of this new subdivision.",
                null
        );
        addDefault(
                defaults,
                Messages.CreateSubdivisionOverlap,
                "Your selected area overlaps another subdivision.",
                null
        );
        addDefault(
                defaults,
                Messages.SubdivisionSuccess,
                "Subdivision created!  Use /trust to share it with friends.",
                null
        );
        addDefault(
                defaults,
                Messages.CreateClaimFailOverlap,
                "You can't create a claim here because it would overlap your other claim.  Use /abandonclaim to delete it, or use your shovel at a corner to resize it.",
                null
        );
        addDefault(
                defaults,
                Messages.CreateClaimFailOverlapOtherPlayer,
                "You can't create a claim here because it would overlap {0}'s claim.",
                "0: other claim owner"
        );
        addDefault(defaults, Messages.ClaimsDisabledWorld, "Land claims are disabled in this world.", null);
        addDefault(
                defaults,
                Messages.ClaimStart,
                "Claim corner set!  Use the shovel again at the opposite corner to claim a rectangle of land.  To cancel, put your shovel away.",
                null
        );
        addDefault(
                defaults,
                Messages.NewClaimTooNarrow,
                "This claim would be too small.  Any claim must be at least {0} blocks wide.",
                "0: minimum claim width"
        );
        addDefault(
                defaults,
                Messages.ResizeClaimInsufficientArea,
                "This claim would be too small.  Any claim must use at least {0} total claim blocks.",
                "0: minimum claim area"
        );
        addDefault(
                defaults,
                Messages.CreateClaimInsufficientBlocks,
                "You don't have enough blocks to claim that entire area.  You need {0} more blocks.",
                "0: additional blocks needed"
        );
        addDefault(
                defaults,
                Messages.AbandonClaimAdvertisement,
                "To delete another claim and free up some blocks, use /AbandonClaim.",
                null
        );
        addDefault(
                defaults,
                Messages.CreateClaimFailOverlapShort,
                "Your selected area overlaps an existing claim.",
                null
        );
        addDefault(defaults, Messages.CreateClaimSuccess, "Claim created!  Use /trust to share it with friends.", null);
        addDefault(
                defaults,
                Messages.SiegeWinDoorsOpen,
                "Congratulations!  Buttons and levers are temporarily unlocked.",
                null
        );
        addDefault(defaults, Messages.RescueAbortedMoved, "You moved!  Rescue cancelled.", null);
        addDefault(defaults, Messages.SiegeDoorsLockedEjection, "Looting time is up!  Ejected from the claim.", null);
        addDefault(defaults, Messages.NoModifyDuringSiege, "Claims can't be modified while under siege.", null);
        addDefault(defaults, Messages.OnlyOwnersModifyClaims, "Only {0} can modify this claim.", "0: owner name");
        addDefault(
                defaults,
                Messages.NoBuildUnderSiege,
                "This claim is under siege by {0}.  No one can build here.",
                "0: attacker name"
        );
        addDefault(defaults, Messages.NoBuildPvP, "You can't build in claims during PvP combat.", null);
        addDefault(
                defaults,
                Messages.NoBuildPermission,
                "You don't have {0}'s permission to build here.",
                "0: owner name"
        );
        addDefault(defaults, Messages.NonSiegeMaterial, "That material is too tough to break.", null);
        addDefault(defaults, Messages.NoOwnerBuildUnderSiege, "You can't make changes while under siege.", null);
        addDefault(
                defaults,
                Messages.NoAccessPermission,
                "You don't have {0}'s permission to use that.",
                "0: owner name.  access permission controls buttons, levers, and beds"
        );
        addDefault(
                defaults,
                Messages.NoContainersSiege,
                "This claim is under siege by {0}.  No one can access containers here right now.",
                "0: attacker name"
        );
        addDefault(
                defaults,
                Messages.NoContainersPermission,
                "You don't have {0}'s permission to use that.",
                "0: owner's name.  containers also include crafting blocks"
        );
        addDefault(
                defaults,
                Messages.OwnerNameForAdminClaims,
                "an administrator",
                "as in 'You don't have an administrator's permission to build here.'"
        );
        addDefault(
                defaults,
                Messages.ClaimTooSmallForEntities,
                "This claim isn't big enough for that.  Try enlarging it.",
                null
        );
        addDefault(
                defaults,
                Messages.TooManyEntitiesInClaim,
                "This claim has too many entities already.  Try enlarging the claim or removing some animals, monsters, paintings, or minecarts.",
                null
        );
        addDefault(defaults, Messages.YouHaveNoClaims, "You don't have any land claims.", null);
        addDefault(
                defaults,
                Messages.ConfirmFluidRemoval,
                "Abandoning this claim will remove lava inside the claim.  If you're sure, use /AbandonClaim again.",
                null
        );
        addDefault(
                defaults,
                Messages.AdjustGroupBlocksSuccess,
                "Adjusted bonus claim blocks for players with the {0} permission by {1}.  New total: {2}.",
                "0: permission; 1: adjustment amount; 2: new total bonus"
        );
        addDefault(
                defaults,
                Messages.InvalidPermissionID,
                "Please specify a player name, or a permission in [brackets].",
                null
        );
        addDefault(
                defaults,
                Messages.HowToClaimRegex,
                "(^|.*\\W)how\\W.*\\W(claim|protect|lock)(\\W.*|$)",
                "This is a Java Regular Expression.  Look it up before editing!  It's used to tell players about the demo video when they ask how to claim land."
        );
        addDefault(
                defaults,
                Messages.NoBuildOutsideClaims,
                "You can't build here unless you claim some land first.",
                null
        );
        addDefault(
                defaults,
                Messages.PlayerOfflineTime,
                "  Last login: {0} days ago.",
                "0: number of full days since last login"
        );
        addDefault(
                defaults,
                Messages.BuildingOutsideClaims,
                "Other players can build here, too.  Consider creating a land claim to protect your work!",
                null
        );
        addDefault(
                defaults,
                Messages.TrappedWontWorkHere,
                "Sorry, unable to find a safe location to teleport you to.  Contact an admin.",
                null
        );
        addDefault(defaults, Messages.CommandBannedInPvP, "You can't use that command while in PvP combat.", null);
        addDefault(
                defaults,
                Messages.UnclaimCleanupWarning,
                "The land you've unclaimed may be changed by other players or cleaned up by administrators.  If you've built something there you want to keep, you should reclaim it.",
                null
        );
        addDefault(
                defaults,
                Messages.BuySellNotConfigured,
                "Sorry, buying and selling claim blocks is disabled.",
                null
        );
        addDefault(defaults, Messages.NoTeleportPvPCombat, "You can't teleport while fighting another player.", null);
        addDefault(
                defaults,
                Messages.NoTNTDamageAboveSeaLevel,
                "Warning: TNT will not destroy blocks above sea level.",
                null
        );
        addDefault(defaults, Messages.NoTNTDamageClaims, "Warning: TNT will not destroy claimed blocks.", null);
        addDefault(defaults, Messages.IgnoreClaimsAdvertisement, "To override, use /IgnoreClaims.", null);
        addDefault(defaults, Messages.NoPermissionForCommand, "You don't have permission to do that.", null);
        addDefault(
                defaults,
                Messages.ClaimsListNoPermission,
                "You don't have permission to get information about another player's land claims.",
                null
        );
        addDefault(
                defaults,
                Messages.ExplosivesDisabled,
                "This claim is now protected from explosions.  Use /ClaimExplosions again to disable.",
                null
        );
        addDefault(
                defaults,
                Messages.ExplosivesEnabled,
                "This claim is now vulnerable to explosions.  Use /ClaimExplosions again to re-enable protections.",
                null
        );
        addDefault(
                defaults,
                Messages.ClaimExplosivesAdvertisement,
                "To allow explosives to destroy blocks in this land claim, use /ClaimExplosions.",
                null
        );
        addDefault(defaults, Messages.PlayerInPvPSafeZone, "That player is in a PvP safe zone.", null);
        addDefault(
                defaults,
                Messages.NoPistonsOutsideClaims,
                "Warning: Pistons won't move blocks outside land claims.",
                null
        );
        addDefault(
                defaults,
                Messages.DropUnlockAdvertisement,
                "Other players can't pick up your dropped items unless you /UnlockDrops first.",
                null
        );
        addDefault(
                defaults,
                Messages.PickupBlockedExplanation,
                "You can't pick this up unless {0} uses /UnlockDrops.",
                "0: The item stack's owner."
        );
        addDefault(
                defaults,
                Messages.DropUnlockConfirmation,
                "Unlocked your drops.  Other players may now pick them up (until you die again).",
                null
        );
        addDefault(
                defaults,
                Messages.DropUnlockOthersConfirmation,
                "Unlocked {0}'s drops.",
                "0: The owner of the unlocked drops."
        );
        addDefault(
                defaults,
                Messages.AdvertiseACandACB,
                "You may use /ACB to give yourself more claim blocks, or /AdminClaims to create a free administrative claim.",
                null
        );
        addDefault(
                defaults,
                Messages.AdvertiseAdminClaims,
                "You could create an administrative land claim instead using /AdminClaims, which you'd share with other administrators.",
                null
        );
        addDefault(defaults, Messages.AdvertiseACB, "You may use /ACB to give yourself more claim blocks.", null);
        addDefault(
                defaults,
                Messages.NotYourPet,
                "That belongs to {0} until it's given to you with /GivePet.",
                "0: owner name"
        );
        addDefault(defaults, Messages.PetGiveawayConfirmation, "Pet transferred.", null);
        addDefault(defaults, Messages.PetTransferCancellation, "Pet giveaway cancelled.", null);
        addDefault(
                defaults,
                Messages.ReadyToTransferPet,
                "Ready to transfer!  Right-click the pet you'd like to give away, or cancel with /GivePet cancel.",
                null
        );
        addDefault(
                defaults,
                Messages.AvoidGriefClaimLand,
                "Prevent grief!  If you claim your land, you will be grief-proof.",
                null
        );
        addDefault(defaults, Messages.BecomeMayor, "Subdivide your land claim and become a mayor!", null);
        addDefault(
                defaults,
                Messages.ClaimCreationFailedOverClaimCountLimit,
                "You've reached your limit on land claims.  Use /AbandonClaim to remove one before creating another.",
                null
        );
        addDefault(
                defaults,
                Messages.CreateClaimFailOverlapRegion,
                "You can't claim all of this because you're not allowed to build here.",
                null
        );
        addDefault(
                defaults,
                Messages.ResizeFailOverlapRegion,
                "You don't have permission to build there, so you can't claim that area.",
                null
        );
        addDefault(defaults, Messages.ShowNearbyClaims, "Found {0} land claims.", "0: Number of claims found.");
        addDefault(defaults, Messages.SiegeImmune, "That player is immune to /siege.", null);
        addDefault(defaults, Messages.SetClaimBlocksSuccess, "Updated accrued claim blocks.", null);
        addDefault(defaults, Messages.TrustListHeader, "Explicit permissions here:", "0: The claim's owner");
        addDefault(defaults, Messages.Manage, "Manage", null);
        addDefault(defaults, Messages.Build, "Build", null);
        addDefault(defaults, Messages.Containers, "Containers", null);
        addDefault(defaults, Messages.Access, "Access", null);
        addDefault(
                defaults,
                Messages.HasSubclaimRestriction,
                "This subclaim does not inherit permissions from the parent",
                null
        );
        addDefault(defaults, Messages.StartBlockMath, "{0} blocks from play + {1} bonus = {2} total.", null);
        addDefault(defaults, Messages.ClaimsListHeader, "Claims:", null);
        addDefault(defaults, Messages.ContinueBlockMath, " (-{0} blocks)", null);
        addDefault(defaults, Messages.EndBlockMath, " = {0} blocks left to spend", null);
        addDefault(defaults, Messages.NoClaimDuringPvP, "You can't claim lands during PvP combat.", null);
        addDefault(defaults, Messages.UntrustAllOwnerOnly, "Only the claim owner can clear all its permissions.", null);
        addDefault(defaults, Messages.ManagersDontUntrustManagers, "Only the claim owner can demote a manager.", null);
        addDefault(
                defaults,
                Messages.NoEnoughBlocksForChestClaim,
                "Because you don't have any claim blocks available, no automatic land claim was created for you.  You can use /ClaimsList to monitor your available claim block total.",
                null
        );
        addDefault(
                defaults,
                Messages.MustHoldModificationToolForThat,
                "You must be holding a golden shovel to do that.",
                null
        );
        addDefault(defaults, Messages.StandInClaimToResize, "Stand inside the land claim you want to resize.", null);
        addDefault(defaults, Messages.ClaimsExtendToSky, "Land claims always extend to max build height.", null);
        addDefault(
                defaults,
                Messages.ClaimsAutoExtendDownward,
                "Land claims auto-extend deeper into the ground when you place blocks under them.",
                null
        );
        addDefault(defaults, Messages.MinimumRadius, "Minimum radius is {0}.", "0: minimum radius");
        addDefault(
                defaults,
                Messages.RadiusRequiresGoldenShovel,
                "You must be holding a golden shovel when specifying a radius.",
                null
        );
        addDefault(
                defaults,
                Messages.ClaimTooSmallForActiveBlocks,
                "This claim isn't big enough to support any active block types (hoppers, spawners, beacons...).  Make the claim bigger first.",
                null
        );
        addDefault(
                defaults,
                Messages.TooManyActiveBlocksInClaim,
                "This claim is at its limit for active block types (hoppers, spawners, beacons...).  Either make it bigger, or remove other active blocks first.",
                null
        );

        addDefault(defaults, Messages.BookAuthor, "BigScary", null);
        addDefault(defaults, Messages.BookTitle, "How to Claim Land", null);
        addDefault(defaults, Messages.BookLink, "Click: {0}", "{0}: video URL");
        addDefault(
                defaults,
                Messages.BookIntro,
                "Claim land to protect your stuff!  Click the link above to learn land claims in 3 minutes or less.  :)",
                null
        );
        addDefault(
                defaults,
                Messages.BookTools,
                "Our claim tools are {0} and {1}.",
                "0: claim modification tool name; 1:claim information tool name"
        );
        addDefault(
                defaults,
                Messages.BookDisabledChestClaims,
                "  On this server, placing a chest will NOT claim land for you.",
                null
        );
        addDefault(defaults, Messages.BookUsefulCommands, "Useful Commands:", null);
        addDefault(
                defaults,
                Messages.ConsoleOnlyCommand,
                "That command may only be executed from the server console.",
                null
        );
        addDefault(defaults, Messages.WorldNotFound, "World not found.", null);

        addDefault(defaults, Messages.StandInSubclaim, "You need to be standing in a subclaim to restrict it", null);
        addDefault(
                defaults,
                Messages.SubclaimRestricted,
                "This subclaim's permissions will no longer inherit from the parent claim",
                null
        );
        addDefault(
                defaults,
                Messages.SubclaimUnrestricted,
                "This subclaim's permissions will now inherit from the parent claim",
                null
        );

        addDefault(
                defaults,
                Messages.NetherPortalTrapDetectionMessage,
                "It seems you might be stuck inside a nether portal. We will rescue you in a few seconds if that is the case!",
                "Sent to player on join, if they left while inside a nether portal."
        );

        //load the config file
        FileConfiguration config = YamlConfiguration.loadConfiguration(new File(messagesFilePath));

        //for each message ID
        for (Messages messageID : messageIDs) {
            //get default for this message
            CustomizableMessage messageData = defaults.get(messageID.name());

            //if default is missing, log an error and use some fake data for now so that the plugin can run
            if (messageData == null) {
                GriefPrevention.AddLogEntry(
                        "Missing message for " + messageID.name() + ".  Please contact the developer.");
                messageData = new CustomizableMessage(
                        messageID,
                        "Missing message!  ID: " + messageID.name() + ".  Please contact a server admin.",
                        null
                );
            }

            //read the message from the file, use default if necessary
            messages[messageID.ordinal()] = config.getString(
                    "Messages." + messageID.name() + ".Text",
                    messageData.text
            );
            config.set("Messages." + messageID.name() + ".Text", messages[messageID.ordinal()]);

            //support color codes
            if (messageID != Messages.HowToClaimRegex) {
                messages[messageID.ordinal()] = messages[messageID.ordinal()].replace('$', (char) 0x00A7);
            }

            if (messageData.notes != null) {
                messageData.notes = config.getString("Messages." + messageID.name() + ".Notes", messageData.notes);
                config.set("Messages." + messageID.name() + ".Notes", messageData.notes);
            }
        }

        //save any changes
        try {
            config.options().header(
                    "Use a YAML editor like NotepadPlusPlus to edit this file.  \nAfter editing, back up your changes before reloading the server in case you made a syntax error.  \nUse dollar signs ($) for formatting codes, which are documented here: https://minecraft.gamepedia.com/Formatting_codes");
            config.save(DataStore.messagesFilePath);
        }
        catch (IOException exception) {
            GriefPrevention.AddLogEntry(
                    "Unable to write to the configuration file at \"" + DataStore.messagesFilePath + "\"");
        }

        defaults.clear();
        System.gc();
    }

    private void addDefault(HashMap<String, CustomizableMessage> defaults,
                            Messages id, String text, String notes)
    {
        CustomizableMessage message = new CustomizableMessage(id, text, notes);
        defaults.put(id.name(), message);
    }

    synchronized public String getMessage(Messages messageID, String... args)
    {
        String message = messages[messageID.ordinal()];

        for (int i = 0; i < args.length; i++) {
            String param = args[i];
            message = message.replace("{" + i + "}", param);
        }

        return message;
    }
    //#endregion

    public abstract void close();

    //gets all the claims "near" a location
    public Set<Claim> getNearbyClaims(Location location)
    {
        Set<Claim> claims = new HashSet<>();

        Chunk lesserChunk = location.getWorld().getChunkAt(location.subtract(150, 0, 150));
        Chunk greaterChunk = location.getWorld().getChunkAt(location.add(300, 0, 300));

        for (int chunk_x = lesserChunk.getX(); chunk_x <= greaterChunk.getX(); chunk_x++) {
            for (int chunk_z = lesserChunk.getZ(); chunk_z <= greaterChunk.getZ(); chunk_z++) {
                Chunk chunk = location.getWorld().getChunkAt(chunk_x, chunk_z);
                Long chunkID = getChunkHash(chunk.getBlock(0, 0, 0).getLocation());
                ArrayList<Claim> claimsInChunk = chunksToClaimsMap.get(chunkID);
                if (claimsInChunk != null) {
                    for (Claim claim : claimsInChunk) {
                        if (claim.inDataStore &&
                            claim.getLesserBoundaryCorner().getWorld().equals(location.getWorld()))
                        {
                            claims.add(claim);
                        }
                    }
                }
            }
        }

        return claims;
    }

    //deletes all the land claims in a specified world
    public void deleteClaimsInWorld(World world, boolean deleteAdminClaims)
    {
        for (int i = 0; i < claims.size(); i++) {
            Claim claim = claims.get(i);
            if (claim.getLesserBoundaryCorner().getWorld().equals(world)) {
                if (!deleteAdminClaims && claim.isAdminClaim()) continue;
                deleteClaim(claim, false, false);
                i--;
            }
        }
    }

    //#region Exceptions
    public static class NoTransferException extends RuntimeException
    {
        private static final long serialVersionUID = 1L;

        NoTransferException(String message)
        {
            super(message);
        }
    }
    //#endregion

    //#region Tasks
    private class SavePlayerDataThread extends Thread
    {
        private final UUID playerID;
        private final PlayerData playerData;

        SavePlayerDataThread(UUID playerID, PlayerData playerData)
        {
            this.playerID = playerID;
            this.playerData = playerData;
        }

        public void run()
        {
            //ensure player data is already read from file before trying to save
            playerData.getAccruedClaimBlocks();
            playerData.getClaims();
            asyncSavePlayerData(playerID, playerData);
        }
    }
    //#endregion
}
