package me.tinyoverflow.griefprevention.datastore;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import me.tinyoverflow.griefprevention.Claim;
import me.tinyoverflow.griefprevention.GriefPrevention;
import me.tinyoverflow.griefprevention.PlayerData;
import me.tinyoverflow.griefprevention.logger.ActivityType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

//manages data stored in the file system
public class FlatFileDataStore extends DataStore
{
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
        File playerDataFolder = new File(playerDataFolderPath);
        File claimDataFolder = new File(claimDataFolderPath);

        if (!playerDataFolder.exists() || !claimDataFolder.exists()) {
            playerDataFolder.mkdirs();
            claimDataFolder.mkdirs();
        }

        //load group data into memory
        File[] files = playerDataFolder.listFiles();
        for (File file : files) {
            if (!file.isFile()) continue;  //avoids folders

            //all group data files start with a dollar sign.  ignoring the rest, which are player data files.
            if (!file.getName().startsWith("$")) continue;

            String groupName = file.getName().substring(1);
            if (groupName == null || groupName.isEmpty()) continue;  //defensive coding, avoid unlikely cases

            BufferedReader inStream = null;
            try {
                inStream = new BufferedReader(new FileReader(file.getAbsolutePath()));
                String line = inStream.readLine();

                int groupBonusBlocks = Integer.parseInt(line);

                permissionToBonusBlocksMap.put(groupName, groupBonusBlocks);
            }
            catch (Exception e) {
                StringWriter errors = new StringWriter();
                e.printStackTrace(new PrintWriter(errors));
                GriefPrevention.AddLogEntry(errors.toString(), ActivityType.EXCEPTION);
            }

            try {
                if (inStream != null) inStream.close();
            }
            catch (IOException exception) {
            }
        }

        //load next claim number from file
        File nextClaimIdFile = new File(nextClaimIdFilePath);
        if (nextClaimIdFile.exists()) {
            BufferedReader inStream = null;
            try {
                inStream = new BufferedReader(new FileReader(nextClaimIdFile.getAbsolutePath()));

                //read the id
                String line = inStream.readLine();

                //try to parse into a long value
                nextClaimID = Long.parseLong(line);
            }
            catch (Exception e) {
            }

            try {
                if (inStream != null) inStream.close();
            }
            catch (IOException exception) {
            }
        }

        //load claims data into memory
        //get a list of all the files in the claims data folder
        files = claimDataFolder.listFiles();
        if (files != null) {
            loadClaimData(files);
        }

        super.initialize();
    }

    void loadClaimData(File[] files)
    {
        ConcurrentHashMap<Claim, Long> orphans = new ConcurrentHashMap<>();
        for (int i = 0; i < files.length; i++) {
            if (files[i].isFile())  //avoids folders
            {
                //skip any file starting with an underscore, to avoid special files not representing land claims
                if (files[i].getName().startsWith("_")) continue;

                //delete any which don't end in .yml
                if (!files[i].getName().endsWith(".yml")) {
                    files[i].delete();
                    continue;
                }

                //the filename is the claim ID.  try to parse it
                long claimID;

                try {
                    claimID = Long.parseLong(files[i].getName().split("\\.")[0]);
                }

                //because some older versions used a different file name pattern before claim IDs were introduced,
                //those files need to be "converted" by renaming them to a unique ID
                catch (Exception e) {
                    claimID = nextClaimID;
                    incrementNextClaimID();
                    File newFile = new File(claimDataFolderPath + File.separator + nextClaimID + ".yml");
                    files[i].renameTo(newFile);
                    files[i] = newFile;
                }

                try {
                    ArrayList<Long> out_parentID = new ArrayList<>();  //hacky output parameter
                    Claim claim = loadClaim(files[i], out_parentID, claimID);
                    if (out_parentID.isEmpty() || out_parentID.get(0) == -1) {
                        addClaim(claim, false);
                    }
                    else {
                        orphans.put(claim, out_parentID.get(0));
                    }
                }

                //if there's any problem with the file's content, log an error message and skip it
                catch (Exception e) {
                    if (e.getMessage() != null && e.getMessage().contains("World not found")) {
                        GriefPrevention.AddLogEntry("Failed to load a claim (ID:" + claimID +
                                                    ") because its world isn't loaded (yet?).  If this is not expected, delete this claim.");
                    }
                    else {
                        StringWriter errors = new StringWriter();
                        e.printStackTrace(new PrintWriter(errors));
                        GriefPrevention.AddLogEntry(files[i].getName() + " " + errors, ActivityType.EXCEPTION);
                    }
                }
            }
        }

        //link children to parents
        for (Claim child : orphans.keySet()) {
            Claim parent = getClaim(orphans.get(child));
            if (parent != null) {
                child.parent = parent;
                addClaim(child, false);
            }
        }
    }

    Claim loadClaim(File file, ArrayList<Long> out_parentID, long claimID) throws Exception
    {
        List<String> lines = Files.readLines(file, StandardCharsets.UTF_8);
        StringBuilder builder = new StringBuilder();
        for (String line : lines) {
            builder.append(line).append('\n');
        }

        return loadClaim(
                builder.toString(),
                out_parentID,
                file.lastModified(),
                claimID,
                Bukkit.getServer().getWorlds()
        );
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
        Claim claim = new Claim(
                lesserBoundaryCorner,
                greaterBoundaryCorner,
                ownerID,
                builders,
                containers,
                accessors,
                managers,
                inheritNothing,
                claimID
        );
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
        if (claim.parent != null) {
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

        try {
            //open the claim's file
            File claimFile = new File(claimDataFolderPath + File.separator + claimID + ".yml");
            claimFile.createNewFile();
            Files.write(yaml.getBytes(StandardCharsets.UTF_8), claimFile);
        }

        //if any problem, log it
        catch (Exception e) {
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            GriefPrevention.AddLogEntry(claimID + " " + errors, ActivityType.EXCEPTION);
        }
    }

    //deletes a claim from the file system
    @Override
    synchronized void deleteClaimFromSecondaryStorage(Claim claim)
    {
        String claimID = String.valueOf(claim.id);

        //remove from disk
        File claimFile = new File(claimDataFolderPath + File.separator + claimID + ".yml");
        if (claimFile.exists() && !claimFile.delete()) {
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
        if (playerFile.exists()) {
            try {
                YamlConfiguration yaml = new YamlConfiguration();
                yaml.load(playerFile);

                playerData.setAccruedClaimBlocks(yaml.getInt("accrued.blocks"));
                playerData.setAccruedClaimBlocksLimit(yaml.getInt("accrued.limit"));
                playerData.setBonusClaimBlocks(yaml.getInt("bonus.blocks"));
            }

            //if there's any problem with the file's content, retry up to 5 times with 5 milliseconds between
            catch (Exception e) {
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

        try {
            File playerDataYamlFile = new File(playerDataFolderPath + File.separator + playerID + ".yml");
            Files.write(yaml.saveToString().getBytes(Charsets.UTF_8), playerDataYamlFile);
        }
        catch (Exception e) {
            GriefPrevention.AddLogEntry(
                    "GriefPrevention: Unexpected exception saving data for player \"" + playerID + "\": " +
                    e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    synchronized void incrementNextClaimID()
    {
        //increment in memory
        nextClaimID++;

        BufferedWriter outStream = null;

        try {
            //open the file and write the new value
            File nextClaimIdFile = new File(nextClaimIdFilePath);
            nextClaimIdFile.createNewFile();
            outStream = new BufferedWriter(new FileWriter(nextClaimIdFile));

            outStream.write(String.valueOf(nextClaimID));
        }

        //if any problem, log it
        catch (Exception e) {
            GriefPrevention.AddLogEntry("Unexpected exception saving next claim ID: " + e.getMessage());
            e.printStackTrace();
        }

        //close the file
        try {
            if (outStream != null) outStream.close();
        }
        catch (IOException exception) {
        }
    }

    @Override
    public synchronized void close()
    {
    }
}
