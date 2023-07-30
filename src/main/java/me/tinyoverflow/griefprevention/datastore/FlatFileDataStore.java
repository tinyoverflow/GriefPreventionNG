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

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import me.tinyoverflow.griefprevention.Claim;
import me.tinyoverflow.griefprevention.GriefPrevention;
import me.tinyoverflow.griefprevention.PlayerData;
import me.tinyoverflow.griefprevention.UUIDFetcher;
import me.tinyoverflow.griefprevention.logger.LogType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;

//manages data stored in the file system
public class FlatFileDataStore extends DataStore
{
    private final static String schemaVersionFilePath = dataLayerFolderPath + File.separator + "_schemaVersion";
    private static String claimDataFolderPath = dataLayerFolderPath + File.separator + "ClaimData";
    private final static String nextClaimIdFilePath = claimDataFolderPath + File.separator + "_nextClaimID";

    //initialization!
    public FlatFileDataStore(File dataFolder) throws Exception
    {
        playerDataFolderPath = Paths.get(dataFolder.toString(), "players").toString();
        claimDataFolderPath = Paths.get(dataFolder.toString(), "claims").toString();

        initialize();
    }

    public static boolean hasData()
    {
        File claimsDataFolder = new File(claimDataFolderPath);

        return claimsDataFolder.exists();
    }

    @Override
    void initialize() throws Exception
    {
        //ensure data folders exist
        boolean newDataStore = false;
        File playerDataFolder = new File(playerDataFolderPath);
        File claimDataFolder = new File(claimDataFolderPath);
        if (!playerDataFolder.exists() || !claimDataFolder.exists())
        {
            newDataStore = true;
            playerDataFolder.mkdirs();
            claimDataFolder.mkdirs();
        }

        //if there's no data yet, then anything written will use the schema implemented by this code
        if (newDataStore)
        {
            setSchemaVersion(DataStore.latestSchemaVersion);
        }

        //load group data into memory
        File[] files = playerDataFolder.listFiles();
        for (File file : files)
        {
            if (!file.isFile()) continue;  //avoids folders

            //all group data files start with a dollar sign.  ignoring the rest, which are player data files.
            if (!file.getName().startsWith("$")) continue;

            String groupName = file.getName().substring(1);
            if (groupName == null || groupName.isEmpty()) continue;  //defensive coding, avoid unlikely cases

            BufferedReader inStream = null;
            try
            {
                inStream = new BufferedReader(new FileReader(file.getAbsolutePath()));
                String line = inStream.readLine();

                int groupBonusBlocks = Integer.parseInt(line);

                permissionToBonusBlocksMap.put(groupName, groupBonusBlocks);
            }
            catch (Exception e)
            {
                StringWriter errors = new StringWriter();
                e.printStackTrace(new PrintWriter(errors));
                GriefPrevention.AddLogEntry(errors.toString(), LogType.EXCEPTION);
            }

            try
            {
                if (inStream != null) inStream.close();
            }
            catch (IOException exception)
            {
            }
        }

        //load next claim number from file
        File nextClaimIdFile = new File(nextClaimIdFilePath);
        if (nextClaimIdFile.exists())
        {
            BufferedReader inStream = null;
            try
            {
                inStream = new BufferedReader(new FileReader(nextClaimIdFile.getAbsolutePath()));

                //read the id
                String line = inStream.readLine();

                //try to parse into a long value
                nextClaimID = Long.parseLong(line);
            }
            catch (Exception e)
            {
            }

            try
            {
                if (inStream != null) inStream.close();
            }
            catch (IOException exception)
            {
            }
        }

        //if converting up from schema version 0, rename player data files using UUIDs instead of player names
        //get a list of all the files in the claims data folder
        if (getSchemaVersion() == 0)
        {
            files = playerDataFolder.listFiles();
            ArrayList<String> namesToConvert = new ArrayList<>();
            for (File playerFile : files)
            {
                namesToConvert.add(playerFile.getName());
            }

            //resolve and cache as many as possible through various means
            try
            {
                UUIDFetcher fetcher = new UUIDFetcher(namesToConvert);
                fetcher.call();
            }
            catch (Exception e)
            {
                GriefPrevention.AddLogEntry("Failed to resolve a batch of names to UUIDs.  Details:" + e.getMessage());
                e.printStackTrace();
            }

            //rename files
            for (File playerFile : files)
            {
                String currentFilename = playerFile.getName();

                //if corrected casing and a record already exists using the correct casing, skip this one
                String correctedCasing = UUIDFetcher.correctedNames.get(currentFilename);
                if (correctedCasing != null && !currentFilename.equals(correctedCasing))
                {
                    File correctedCasingFile = new File(playerDataFolder.getPath() + File.separator + correctedCasing);
                    if (correctedCasingFile.exists())
                    {
                        continue;
                    }
                }

                //try to convert player name to UUID
                UUID playerID = null;
                try
                {
                    playerID = UUIDFetcher.getUUIDOf(currentFilename);

                    //if successful, rename the file using the UUID
                    if (playerID != null)
                    {
                        playerFile.renameTo(new File(playerDataFolder, playerID.toString()));
                    }
                }
                catch (Exception ex)
                {
                }
            }
        }

        //load claims data into memory
        //get a list of all the files in the claims data folder
        files = claimDataFolder.listFiles();

        if (getSchemaVersion() <= 1)
        {
            loadClaimData_Legacy(files);
        } else
        {
            loadClaimData(files);
        }

        super.initialize();
    }

    void loadClaimData_Legacy(File[] files) throws Exception
    {
        List<World> validWorlds = Bukkit.getServer().getWorlds();

        for (int i = 0; i < files.length; i++)
        {
            if (files[i].isFile())  //avoids folders
            {
                //skip any file starting with an underscore, to avoid special files not representing land claims
                if (files[i].getName().startsWith("_")) continue;

                //the filename is the claim ID.  try to parse it
                long claimID;

                try
                {
                    claimID = Long.parseLong(files[i].getName());
                }

                //because some older versions used a different file name pattern before claim IDs were introduced,
                //those files need to be "converted" by renaming them to a unique ID
                catch (Exception e)
                {
                    claimID = nextClaimID;
                    incrementNextClaimID();
                    File newFile = new File(claimDataFolderPath + File.separator + nextClaimID);
                    files[i].renameTo(newFile);
                    files[i] = newFile;
                }

                BufferedReader inStream = null;
                String lesserCornerString = "";
                try
                {
                    Claim topLevelClaim = null;

                    inStream = new BufferedReader(new FileReader(files[i].getAbsolutePath()));
                    String line = inStream.readLine();

                    while (line != null)
                    {
                        //skip any SUB:### lines from previous versions
                        if (line.toLowerCase().startsWith("sub:"))
                        {
                            line = inStream.readLine();
                        }

                        //skip any UUID lines from previous versions
                        Matcher match = uuidpattern.matcher(line.trim());
                        if (match.find())
                        {
                            line = inStream.readLine();
                        }

                        //first line is lesser boundary corner location
                        lesserCornerString = line;
                        Location lesserBoundaryCorner = locationFromString(lesserCornerString, validWorlds);

                        //second line is greater boundary corner location
                        line = inStream.readLine();
                        Location greaterBoundaryCorner = locationFromString(line, validWorlds);

                        //third line is owner name
                        line = inStream.readLine();
                        String ownerName = line;
                        UUID ownerID = null;
                        if (ownerName.isEmpty() || ownerName.startsWith("--"))
                        {
                            ownerID = null;  //administrative land claim or subdivision
                        } else if (getSchemaVersion() == 0)
                        {
                            try
                            {
                                ownerID = UUIDFetcher.getUUIDOf(ownerName);
                            }
                            catch (Exception ex)
                            {
                                GriefPrevention.AddLogEntry("Couldn't resolve this name to a UUID: " + ownerName + ".");
                                GriefPrevention.AddLogEntry("  Converted land claim to administrative @ " + lesserBoundaryCorner.toString());
                            }
                        } else
                        {
                            try
                            {
                                ownerID = UUID.fromString(ownerName);
                            }
                            catch (Exception ex)
                            {
                                GriefPrevention.AddLogEntry("Error - this is not a valid UUID: " + ownerName + ".");
                                GriefPrevention.AddLogEntry("  Converted land claim to administrative @ " + lesserBoundaryCorner.toString());
                            }
                        }

                        //fourth line is list of builders
                        line = inStream.readLine();
                        List<String> builderNames = Arrays.asList(line.split(";"));
                        builderNames = convertNameListToUUIDList(builderNames);

                        //fifth line is list of players who can access containers
                        line = inStream.readLine();
                        List<String> containerNames = Arrays.asList(line.split(";"));
                        containerNames = convertNameListToUUIDList(containerNames);

                        //sixth line is list of players who can use buttons and switches
                        line = inStream.readLine();
                        List<String> accessorNames = Arrays.asList(line.split(";"));
                        accessorNames = convertNameListToUUIDList(accessorNames);

                        //seventh line is list of players who can grant permissions
                        line = inStream.readLine();
                        if (line == null) line = "";
                        List<String> managerNames = Arrays.asList(line.split(";"));
                        managerNames = convertNameListToUUIDList(managerNames);

                        //skip any remaining extra lines, until the "===" string, indicating the end of this claim or subdivision
                        line = inStream.readLine();
                        while (line != null && !line.contains("==="))
                            line = inStream.readLine();

                        //build a claim instance from those data
                        //if this is the first claim loaded from this file, it's the top level claim
                        if (topLevelClaim == null)
                        {
                            //instantiate
                            topLevelClaim = new Claim(lesserBoundaryCorner, greaterBoundaryCorner, ownerID, builderNames, containerNames, accessorNames, managerNames, claimID);

                            topLevelClaim.modifiedDate = new Date(files[i].lastModified());
                            addClaim(topLevelClaim, false);
                        }

                        //otherwise there's already a top level claim, so this must be a subdivision of that top level claim
                        else
                        {
                            Claim subdivision = new Claim(lesserBoundaryCorner, greaterBoundaryCorner, null, builderNames, containerNames, accessorNames, managerNames, null);

                            subdivision.modifiedDate = new Date(files[i].lastModified());
                            subdivision.parent = topLevelClaim;
                            topLevelClaim.children.add(subdivision);
                            subdivision.inDataStore = true;
                        }

                        //move up to the first line in the next subdivision
                        line = inStream.readLine();
                    }

                    inStream.close();
                }

                //if there's any problem with the file's content, log an error message and skip it
                catch (Exception e)
                {
                    if (e.getMessage() != null && e.getMessage().contains("World not found"))
                    {
                        GriefPrevention.AddLogEntry("Failed to load a claim " + files[i].getName() + " because its world isn't loaded (yet?).  Please delete the claim file or contact the GriefPrevention developer with information about which plugin(s) you're using to load or create worlds.  " + lesserCornerString);
                        inStream.close();

                    } else
                    {
                        StringWriter errors = new StringWriter();
                        e.printStackTrace(new PrintWriter(errors));
                        GriefPrevention.AddLogEntry("Failed to load claim " + files[i].getName() + ". This usually occurs when your server runs out of storage space, causing any file saves to corrupt. Fix or delete the file found in GriefPrevention/ClaimData/" + files[i].getName(), LogType.DEBUG, false);
                        GriefPrevention.AddLogEntry(files[i].getName() + " " + errors, LogType.EXCEPTION);
                    }
                }

                try
                {
                    if (inStream != null) inStream.close();
                }
                catch (IOException exception)
                {
                }
            }
        }
    }

    void loadClaimData(File[] files) throws Exception
    {
        ConcurrentHashMap<Claim, Long> orphans = new ConcurrentHashMap<>();
        for (int i = 0; i < files.length; i++)
        {
            if (files[i].isFile())  //avoids folders
            {
                //skip any file starting with an underscore, to avoid special files not representing land claims
                if (files[i].getName().startsWith("_")) continue;

                //delete any which don't end in .yml
                if (!files[i].getName().endsWith(".yml"))
                {
                    files[i].delete();
                    continue;
                }

                //the filename is the claim ID.  try to parse it
                long claimID;

                try
                {
                    claimID = Long.parseLong(files[i].getName().split("\\.")[0]);
                }

                //because some older versions used a different file name pattern before claim IDs were introduced,
                //those files need to be "converted" by renaming them to a unique ID
                catch (Exception e)
                {
                    claimID = nextClaimID;
                    incrementNextClaimID();
                    File newFile = new File(claimDataFolderPath + File.separator + nextClaimID + ".yml");
                    files[i].renameTo(newFile);
                    files[i] = newFile;
                }

                try
                {
                    ArrayList<Long> out_parentID = new ArrayList<>();  //hacky output parameter
                    Claim claim = loadClaim(files[i], out_parentID, claimID);
                    if (out_parentID.size() == 0 || out_parentID.get(0) == -1)
                    {
                        addClaim(claim, false);
                    } else
                    {
                        orphans.put(claim, out_parentID.get(0));
                    }
                }

                //if there's any problem with the file's content, log an error message and skip it
                catch (Exception e)
                {
                    if (e.getMessage() != null && e.getMessage().contains("World not found"))
                    {
                        GriefPrevention.AddLogEntry("Failed to load a claim (ID:" + claimID + ") because its world isn't loaded (yet?).  If this is not expected, delete this claim.");
                    } else
                    {
                        StringWriter errors = new StringWriter();
                        e.printStackTrace(new PrintWriter(errors));
                        GriefPrevention.AddLogEntry(files[i].getName() + " " + errors, LogType.EXCEPTION);
                    }
                }
            }
        }

        //link children to parents
        for (Claim child : orphans.keySet())
        {
            Claim parent = getClaim(orphans.get(child));
            if (parent != null)
            {
                child.parent = parent;
                addClaim(child, false);
            }
        }
    }

    Claim loadClaim(File file, ArrayList<Long> out_parentID, long claimID) throws Exception
    {
        List<String> lines = Files.readLines(file, StandardCharsets.UTF_8);
        StringBuilder builder = new StringBuilder();
        for (String line : lines)
        {
            builder.append(line).append('\n');
        }

        return loadClaim(builder.toString(), out_parentID, file.lastModified(), claimID, Bukkit.getServer().getWorlds());
    }

    Claim loadClaim(String input, ArrayList<Long> out_parentID, long lastModifiedDate, long claimID, List<World> validWorlds) throws Exception
    {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.loadFromString(input);

        //boundaries
        Location lesserBoundaryCorner = locationFromString(yaml.getString("boundaries.lesser"), validWorlds);
        Location greaterBoundaryCorner = locationFromString(yaml.getString("boundaries.greater"), validWorlds);

        //owner
        String ownerIdentifier = yaml.getString("owner");
        UUID ownerID = ownerIdentifier == null || ownerIdentifier.isEmpty()
                ? null
                : UUID.fromString(ownerIdentifier);

        out_parentID.add(yaml.getLong("parent.claim-id", -1L));
        boolean inheritNothing = yaml.getBoolean("parent.inherit-nothing");

        List<String> builders = yaml.getStringList("permissions.builders");
        List<String> containers = yaml.getStringList("permissions.containers");
        List<String> accessors = yaml.getStringList("permissions.accessors");
        List<String> managers = yaml.getStringList("permissions.managers");

        //instantiate
        Claim claim = new Claim(lesserBoundaryCorner, greaterBoundaryCorner, ownerID, builders, containers, accessors, managers, inheritNothing, claimID);
        claim.modifiedDate = new Date(lastModifiedDate);
        claim.id = claimID;

        return claim;
    }

    String getYamlForClaim(Claim claim)
    {
        YamlConfiguration yaml = new YamlConfiguration();

        //owner
        yaml.set("owner", claim.ownerID != null ? claim.ownerID.toString() : null);

        //boundaries
        yaml.set("boundaries.lesser", locationToString(claim.lesserBoundaryCorner));
        yaml.set("boundaries.greater", locationToString(claim.greaterBoundaryCorner));

        // parent claim
        if (claim.parent != null)
        {
            yaml.set("parent.claim-id", claim.parent.id);
            yaml.set("parent.inherit-nothing", claim.getSubclaimRestrictions());
        }

        // access permissions
        ArrayList<String> builders = new ArrayList<>();
        ArrayList<String> containers = new ArrayList<>();
        ArrayList<String> accessors = new ArrayList<>();
        ArrayList<String> managers = new ArrayList<>();
        claim.getPermissions(builders, containers, accessors, managers);

        yaml.set("permissions.builders", builders);
        yaml.set("permissions.containers", containers);
        yaml.set("permissions.accessors", accessors);
        yaml.set("permissions.managers", managers);

        return yaml.saveToString();
    }

    @Override
    synchronized void writeClaimToStorage(Claim claim)
    {
        String claimID = String.valueOf(claim.id);

        String yaml = getYamlForClaim(claim);

        try
        {
            //open the claim's file
            File claimFile = new File(claimDataFolderPath + File.separator + claimID + ".yml");
            claimFile.createNewFile();
            Files.write(yaml.getBytes(StandardCharsets.UTF_8), claimFile);
        }

        //if any problem, log it
        catch (Exception e)
        {
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            GriefPrevention.AddLogEntry(claimID + " " + errors, LogType.EXCEPTION);
        }
    }

    //deletes a claim from the file system
    @Override
    synchronized void deleteClaimFromSecondaryStorage(Claim claim)
    {
        String claimID = String.valueOf(claim.id);

        //remove from disk
        File claimFile = new File(claimDataFolderPath + File.separator + claimID + ".yml");
        if (claimFile.exists() && !claimFile.delete())
        {
            GriefPrevention.AddLogEntry("Error: Unable to delete claim file \"" + claimFile.getAbsolutePath() + "\".");
        }
    }

    @Override
    public synchronized PlayerData getPlayerDataFromStorage(UUID playerID)
    {
        File playerFile = new File(playerDataFolderPath + File.separator + playerID.toString() + ".yml");

        PlayerData playerData = new PlayerData();
        playerData.playerID = playerID;

        //if it exists as a file, read the file
        if (playerFile.exists())
        {
            try
            {
                YamlConfiguration yaml = new YamlConfiguration();
                yaml.load(playerFile);

                playerData.setAccruedClaimBlocks(yaml.getInt("accrued.blocks"));
                playerData.setAccruedClaimBlocksLimit(yaml.getInt("accrued.limit"));
                playerData.setBonusClaimBlocks(yaml.getInt("bonus.blocks"));
            }

            //if there's any problem with the file's content, retry up to 5 times with 5 milliseconds between
            catch (Exception e)
            {
            }
        }

        return playerData;
    }

    //saves changes to player data.  MUST be called after you're done making changes, otherwise a reload will lose them
    @Override
    public void overrideSavePlayerData(UUID playerID, PlayerData playerData)
    {
        //never save data for the "administrative" account.  null for claim owner ID indicates administrative account
        if (playerID == null) return;

        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("accrued.blocks", playerData.getAccruedClaimBlocks());
        yaml.set("accrued.limit", playerData.getAccruedClaimBlocksLimit());
        yaml.set("bonus.blocks", playerData.getBonusClaimBlocks());

        try
        {
            File playerDataYamlFile = new File(playerDataFolderPath + File.separator + playerID + ".yml");
            Files.write(yaml.saveToString().getBytes(Charsets.UTF_8), playerDataYamlFile);
        }
        catch (Exception e)
        {
            GriefPrevention.AddLogEntry("GriefPrevention: Unexpected exception saving data for player \"" + playerID + "\": " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    synchronized void incrementNextClaimID()
    {
        //increment in memory
        nextClaimID++;

        BufferedWriter outStream = null;

        try
        {
            //open the file and write the new value
            File nextClaimIdFile = new File(nextClaimIdFilePath);
            nextClaimIdFile.createNewFile();
            outStream = new BufferedWriter(new FileWriter(nextClaimIdFile));

            outStream.write(String.valueOf(nextClaimID));
        }

        //if any problem, log it
        catch (Exception e)
        {
            GriefPrevention.AddLogEntry("Unexpected exception saving next claim ID: " + e.getMessage());
            e.printStackTrace();
        }

        //close the file
        try
        {
            if (outStream != null) outStream.close();
        }
        catch (IOException exception)
        {
        }
    }

    //grants a group (players with a specific permission) bonus claim blocks as long as they're still members of the group
    @Override
    synchronized void saveGroupBonusBlocks(String groupName, int currentValue)
    {
        //write changes to file to ensure they don't get lost
        BufferedWriter outStream = null;
        try
        {
            //open the group's file
            File groupDataFile = new File(playerDataFolderPath + File.separator + "$" + groupName);
            groupDataFile.createNewFile();
            outStream = new BufferedWriter(new FileWriter(groupDataFile));

            //first line is number of bonus blocks
            outStream.write(String.valueOf(currentValue));
            outStream.newLine();
        }

        //if any problem, log it
        catch (Exception e)
        {
            GriefPrevention.AddLogEntry("Unexpected exception saving data for group \"" + groupName + "\": " + e.getMessage());
        }

        try
        {
            //close the file
            if (outStream != null)
            {
                outStream.close();
            }
        }
        catch (IOException exception)
        {
        }
    }

    public synchronized void migrateData(DatabaseDataStore databaseStore)
    {
        //migrate claims
        for (Claim claim : claims)
        {
            databaseStore.addClaim(claim, true);
            for (Claim child : claim.children)
            {
                databaseStore.addClaim(child, true);
            }
        }

        //migrate groups
        for (Map.Entry<String, Integer> groupEntry : permissionToBonusBlocksMap.entrySet())
        {
            databaseStore.saveGroupBonusBlocks(groupEntry.getKey(), groupEntry.getValue());
        }

        //migrate players
        File playerDataFolder = new File(playerDataFolderPath);
        File[] files = playerDataFolder.listFiles();
        for (File file : files)
        {
            if (!file.isFile()) continue;  //avoids folders
            if (file.isHidden()) continue; //avoid hidden files, which are likely not created by GriefPrevention

            //all group data files start with a dollar sign.  ignoring those, already handled above
            if (file.getName().startsWith("$")) continue;

            //ignore special files
            if (file.getName().startsWith("_")) continue;
            if (file.getName().endsWith(".ignore")) continue;

            UUID playerID = UUID.fromString(file.getName());
            databaseStore.savePlayerData(playerID, getPlayerData(playerID));
            clearCachedPlayerData(playerID);
        }

        //migrate next claim ID
        if (nextClaimID > databaseStore.nextClaimID)
        {
            databaseStore.setNextClaimID(nextClaimID);
        }

        //rename player and claim data folders so the migration won't run again
        int i = 0;
        File claimsBackupFolder;
        File playersBackupFolder;
        do
        {
            String claimsFolderBackupPath = claimDataFolderPath;
            if (i > 0) claimsFolderBackupPath += String.valueOf(i);
            claimsBackupFolder = new File(claimsFolderBackupPath);

            String playersFolderBackupPath = playerDataFolderPath;
            if (i > 0) playersFolderBackupPath += String.valueOf(i);
            playersBackupFolder = new File(playersFolderBackupPath);
            i++;
        } while (claimsBackupFolder.exists() || playersBackupFolder.exists());

        File claimsFolder = new File(claimDataFolderPath);
        File playersFolder = new File(playerDataFolderPath);

        claimsFolder.renameTo(claimsBackupFolder);
        playersFolder.renameTo(playersBackupFolder);

        GriefPrevention.AddLogEntry("Backed your file system data up to " + claimsBackupFolder.getName() + " and " + playersBackupFolder.getName() + ".");
        GriefPrevention.AddLogEntry("If your migration encountered any problems, you can restore those data with a quick copy/paste.");
        GriefPrevention.AddLogEntry("When you're satisfied that all your data have been safely migrated, consider deleting those folders.");
    }

    @Override
    public synchronized void close()
    {
    }

    @Override
    int getSchemaVersionFromStorage()
    {
        File schemaVersionFile = new File(schemaVersionFilePath);
        if (schemaVersionFile.exists())
        {
            BufferedReader inStream = null;
            int schemaVersion = 0;
            try
            {
                inStream = new BufferedReader(new FileReader(schemaVersionFile.getAbsolutePath()));

                //read the version number
                String line = inStream.readLine();

                //try to parse into an int value
                schemaVersion = Integer.parseInt(line);
            }
            catch (Exception e)
            {
            }

            try
            {
                if (inStream != null) inStream.close();
            }
            catch (IOException exception)
            {
            }

            return schemaVersion;
        } else
        {
            updateSchemaVersionInStorage(0);
            return 0;
        }
    }

    @Override
    void updateSchemaVersionInStorage(int versionToSet)
    {
        BufferedWriter outStream = null;

        try
        {
            //open the file and write the new value
            File schemaVersionFile = new File(schemaVersionFilePath);
            schemaVersionFile.createNewFile();
            outStream = new BufferedWriter(new FileWriter(schemaVersionFile));

            outStream.write(String.valueOf(versionToSet));
        }

        //if any problem, log it
        catch (Exception e)
        {
            GriefPrevention.AddLogEntry("Unexpected exception saving schema version: " + e.getMessage());
        }

        //close the file
        try
        {
            if (outStream != null) outStream.close();
        }
        catch (IOException exception)
        {
        }

    }
}
