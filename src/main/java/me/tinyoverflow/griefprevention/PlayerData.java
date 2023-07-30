/*
    GriefPrevention Server Plugin for Minecraft
    Copyright (C) 2011 Ryan Hamshire

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

import com.griefprevention.visualization.BoundaryVisualization;
import me.tinyoverflow.griefprevention.datastore.DataStore;
import me.tinyoverflow.griefprevention.logger.ActivityType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.util.Calendar;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

//holds all of GriefPrevention's player-tied data
public class PlayerData
{
    //the player's ID
    public UUID playerID;
    //where this player was the last time we checked on him for earning claim blocks
    public Location lastAfkCheckLocation = null;
    //what "mode" the shovel is in determines what it will do when it's used
    public ShovelMode shovelMode = ShovelMode.Basic;
    //radius for restore nature fill mode
    public int fillRadius = 0;
    //last place the player used the shovel, useful in creating and resizing claims,
    //because the player must use the shovel twice in those instances
    public Location lastShovelLocation = null;
    //the claim this player is currently resizing
    public Claim claimResizing = null;
    //the claim this player is currently subdividing
    public Claim claimSubdividing = null;
    //whether or not the player has a pending /trapped rescue
    public boolean pendingTrapped = false;
    //whether this player was recently warned about building outside land claims
    public boolean warnedAboutBuildingOutsideClaims = false;
    //timestamp when last siege ended (where this player was the defender)
    public long lastSiegeEndTimeStamp = 0;
    //whether the player was kicked (set and used during logout)
    public boolean wasKicked = false;
    /**
     * @deprecated Use {@link #getVisibleBoundaries} and {@link #setVisibleBoundaries(BoundaryVisualization)}
     */
    @Deprecated(forRemoval = true, since = "16.18")
    public Visualization currentVisualization = null;
    //anti-camping pvp protection
    public boolean pvpImmune = false;
    public long lastSpawn = 0;
    //ignore claims mode
    public boolean ignoreClaims = false;
    //the last claim this player was in, that we know of
    public Claim lastClaim = null;
    //siege
    public SiegeData siegeData = null;
    //pvp
    public long lastPvpTimestamp = 0;
    public String lastPvpPlayer = "";
    //safety confirmation for deleting multi-subdivision claims
    public boolean warnedAboutMajorDeletion = false;
    public InetAddress ipAddress;
    //whether or not this player has received a message about unlocking death drops since his last death
    public boolean receivedDropUnlockAdvertisement = false;
    //whether or not this player's dropped items (on death) are unlocked for other players to pick up
    public boolean dropsAreUnlocked = false;
    //message to send to player after he respawns
    public String messageOnRespawn = null;
    //player which a pet will be given to when it's right-clicked
    public OfflinePlayer petGiveawayRecipient = null;
    //timestamp for last "you're building outside your land claims" message
    public Long buildWarningTimestamp = null;
    //spot where a player can't talk, used to mute new players until they've moved a little
    //this is an anti-bot strategy.
    public Location noChatLocation = null;
    //ignore list
    //true means invisible (admin-forced ignore), false means player-created ignore
    public ConcurrentHashMap<UUID, Boolean> ignoredPlayers = new ConcurrentHashMap<>();
    public boolean ignoreListChanged = false;
    //profanity warning, once per play session
    boolean profanityWarned = false;
    //the player's claims
    private Vector<Claim> claims = null;
    //how many claim blocks the player has earned via play time
    private Integer accruedClaimBlocks = null;
    //temporary holding area to avoid opening data files too early
    private int newlyAccruedClaimBlocks = 0;
    //how many claim blocks the player has been gifted by admins, or purchased via economy integration
    private Integer bonusClaimBlocks = null;
    //visualization
    private transient @Nullable BoundaryVisualization visibleBoundaries = null;
    //for addons to set per-player claim limits. Any negative value will use config's value
    private int AccruedClaimBlocksLimit = -1;

    //whether or not this player is "in" pvp combat
    public boolean inPvpCombat()
    {
        if (lastPvpTimestamp == 0) return false;

        long now = Calendar.getInstance().getTimeInMillis();

        long elapsed = now - lastPvpTimestamp;

        if (elapsed >
            GriefPrevention.instance.getPluginConfig().getPvpConfiguration().getCombatTimeout() * 1000L) //X seconds
        {
            lastPvpTimestamp = 0;
            return false;
        }

        return true;
    }

    //the number of claim blocks a player has available for claiming land
    public int getRemainingClaimBlocks()
    {
        int remainingBlocks = getAccruedClaimBlocks() + getBonusClaimBlocks() +
                              GriefPrevention.instance.dataStore.getGroupBonusBlocks(playerID);
        for (int i = 0; i < getClaims().size(); i++)
        {
            Claim claim = getClaims().get(i);
            remainingBlocks -= claim.getArea();
        }

        return remainingBlocks;
    }

    //don't load data from secondary storage until it's needed
    public synchronized int getAccruedClaimBlocks()
    {
        if (accruedClaimBlocks == null) loadDataFromSecondaryStorage();

        //update claim blocks with any he has accrued during his current play session
        if (newlyAccruedClaimBlocks > 0)
        {
            int accruedLimit = getAccruedClaimBlocksLimit();

            //if over the limit before adding blocks, leave it as-is, because the limit may have changed AFTER he accrued the blocks
            if (accruedClaimBlocks < accruedLimit)
            {
                //move any in the holding area
                int newTotal = accruedClaimBlocks + newlyAccruedClaimBlocks;

                //respect limits
                accruedClaimBlocks = Math.min(newTotal, accruedLimit);
            }

            newlyAccruedClaimBlocks = 0;
            return accruedClaimBlocks;
        }

        return accruedClaimBlocks;
    }

    public void setAccruedClaimBlocks(Integer accruedClaimBlocks)
    {
        this.accruedClaimBlocks = accruedClaimBlocks;
        newlyAccruedClaimBlocks = 0;
    }

    public int getBonusClaimBlocks()
    {
        if (bonusClaimBlocks == null) loadDataFromSecondaryStorage();
        return bonusClaimBlocks;
    }

    public void setBonusClaimBlocks(Integer bonusClaimBlocks)
    {
        this.bonusClaimBlocks = bonusClaimBlocks;
    }

    private void loadDataFromSecondaryStorage()
    {
        //reach out to secondary storage to get any data there
        PlayerData storageData = GriefPrevention.instance.dataStore.getPlayerDataFromStorage(playerID);

        if (accruedClaimBlocks == null)
        {
            if (storageData.accruedClaimBlocks != null)
            {
                accruedClaimBlocks = storageData.accruedClaimBlocks;

                //ensure at least minimum accrued are accrued (in case of settings changes to increase initial amount)
                if (GriefPrevention.instance.config_advanced_fixNegativeClaimblockAmounts && (accruedClaimBlocks <
                                                                                              GriefPrevention.instance.getPluginConfig().getClaimConfiguration().getClaimBlocksConfiguration().initial))
                {
                    accruedClaimBlocks = GriefPrevention.instance.getPluginConfig().getClaimConfiguration().getClaimBlocksConfiguration().initial;
                }
            }
            else
            {
                accruedClaimBlocks = GriefPrevention.instance.getPluginConfig().getClaimConfiguration().getClaimBlocksConfiguration().initial;
            }
        }

        if (bonusClaimBlocks == null)
        {
            if (storageData.bonusClaimBlocks != null)
            {
                bonusClaimBlocks = storageData.bonusClaimBlocks;
            }
            else
            {
                bonusClaimBlocks = 0;
            }
        }
    }

    public Vector<Claim> getClaims()
    {
        if (claims == null)
        {
            claims = new Vector<>();

            //find all the claims belonging to this player and note them for future reference
            DataStore dataStore = GriefPrevention.instance.dataStore;
            int totalClaimsArea = 0;
            for (int i = 0; i < dataStore.claims.size(); i++)
            {
                Claim claim = dataStore.claims.get(i);
                if (!claim.inDataStore)
                {
                    dataStore.claims.remove(i--);
                    continue;
                }
                if (playerID.equals(claim.ownerID))
                {
                    claims.add(claim);
                    totalClaimsArea += claim.getArea();
                }
            }

            //ensure player has claim blocks for his claims, and at least the minimum accrued
            loadDataFromSecondaryStorage();

            //if total claimed area is more than total blocks available
            int totalBlocks = accruedClaimBlocks + getBonusClaimBlocks() +
                              GriefPrevention.instance.dataStore.getGroupBonusBlocks(playerID);
            if (GriefPrevention.instance.config_advanced_fixNegativeClaimblockAmounts && totalBlocks < totalClaimsArea)
            {
                OfflinePlayer player = GriefPrevention.instance.getServer().getOfflinePlayer(playerID);
                GriefPrevention.AddLogEntry(
                        player.getName() + " has more claimed land than blocks available.  Adding blocks to fix.",
                        ActivityType.DEBUG,
                        true
                );
                GriefPrevention.AddLogEntry(
                        player.getName() + " Accrued blocks: " + getAccruedClaimBlocks() + " Bonus blocks: " +
                        getBonusClaimBlocks(),
                        ActivityType.DEBUG,
                        true
                );
                GriefPrevention.AddLogEntry(
                        "Total blocks: " + totalBlocks + " Total claimed area: " + totalClaimsArea,
                        ActivityType.DEBUG,
                        true
                );
                for (Claim claim : claims)
                {
                    if (!claim.inDataStore) continue;
                    GriefPrevention.AddLogEntry(
                            GriefPrevention.getFriendlyLocationString(claim.getLesserBoundaryCorner()) + " // "
                            + GriefPrevention.getFriendlyLocationString(claim.getGreaterBoundaryCorner()) + " = "
                            + claim.getArea()
                            , ActivityType.DEBUG, true);
                }

                //try to fix it by adding to accrued blocks
                accruedClaimBlocks = totalClaimsArea; //Set accrued blocks to equal total claims
                int accruedLimit = getAccruedClaimBlocksLimit();
                accruedClaimBlocks = Math.min(
                        accruedLimit,
                        accruedClaimBlocks
                ); //set accrued blocks to maximum limit, if it's smaller
                GriefPrevention.AddLogEntry("New accrued blocks: " + accruedClaimBlocks, ActivityType.DEBUG, true);

                //Recalculate total blocks (accrued + bonus + permission group bonus)
                totalBlocks = accruedClaimBlocks + getBonusClaimBlocks() +
                              GriefPrevention.instance.dataStore.getGroupBonusBlocks(playerID);
                GriefPrevention.AddLogEntry("New total blocks: " + totalBlocks, ActivityType.DEBUG, true);

                //if that didn't fix it, then make up the difference with bonus blocks
                if (totalBlocks < totalClaimsArea)
                {
                    int bonusBlocksToAdd = totalClaimsArea - totalBlocks;
                    bonusClaimBlocks += bonusBlocksToAdd;
                    GriefPrevention.AddLogEntry(
                            "Accrued blocks weren't enough. Adding " + bonusBlocksToAdd + " bonus blocks.",
                            ActivityType.DEBUG,
                            true
                    );
                }
                GriefPrevention.AddLogEntry(
                        player.getName() + " Accrued blocks: " + getAccruedClaimBlocks() + " Bonus blocks: " +
                        getBonusClaimBlocks() + " Group Bonus Blocks: " +
                        GriefPrevention.instance.dataStore.getGroupBonusBlocks(playerID),
                        ActivityType.DEBUG,
                        true
                );
                //Recalculate total blocks (accrued + bonus + permission group bonus)
                totalBlocks = accruedClaimBlocks + getBonusClaimBlocks() +
                              GriefPrevention.instance.dataStore.getGroupBonusBlocks(playerID);
                GriefPrevention.AddLogEntry(
                        "Total blocks: " + totalBlocks + " Total claimed area: " + totalClaimsArea,
                        ActivityType.DEBUG,
                        true
                );
                GriefPrevention.AddLogEntry(
                        "Remaining claim blocks to use: " + getRemainingClaimBlocks() + " (should be 0)",
                        ActivityType.DEBUG,
                        true
                );
            }
        }

        for (int i = 0; i < claims.size(); i++)
        {
            if (!claims.get(i).inDataStore)
            {
                claims.remove(i--);
            }
        }

        return claims;
    }

    //Limit can be changed by addons
    public int getAccruedClaimBlocksLimit()
    {
        if (AccruedClaimBlocksLimit < 0)
        {
            return GriefPrevention.instance.getPluginConfig().getClaimConfiguration().getClaimBlocksConfiguration().accrued.limit;
        }
        return AccruedClaimBlocksLimit;
    }

    public void setAccruedClaimBlocksLimit(int limit)
    {
        AccruedClaimBlocksLimit = limit;
    }

    public void accrueBlocks(int howMany)
    {
        newlyAccruedClaimBlocks += howMany;
    }

    public @Nullable BoundaryVisualization getVisibleBoundaries()
    {
        return visibleBoundaries;
    }

    public void setVisibleBoundaries(@Nullable BoundaryVisualization visibleBoundaries)
    {
        if (this.visibleBoundaries != null)
        {
            this.visibleBoundaries.revert(Bukkit.getPlayer(playerID));
        }

        this.visibleBoundaries = visibleBoundaries;
    }
}