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

package me.tinyoverflow.griefprevention.tasks;

import me.tinyoverflow.griefprevention.Claim;
import me.tinyoverflow.griefprevention.GriefPrevention;
import me.tinyoverflow.griefprevention.PlayerData;
import me.tinyoverflow.griefprevention.events.ClaimExpirationEvent;
import me.tinyoverflow.griefprevention.logger.ActivityType;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.Calendar;
import java.util.Date;
import java.util.Vector;

class CleanupUnusedClaimTask implements Runnable
{
    Claim claim;
    PlayerData ownerData;
    OfflinePlayer ownerInfo;

    CleanupUnusedClaimTask(Claim claim, PlayerData ownerData, OfflinePlayer ownerInfo)
    {
        this.claim = claim;
        this.ownerData = ownerData;
        this.ownerInfo = ownerInfo;
    }

    @Override
    public void run()
    {

        //determine area of the default chest claim
        int areaOfDefaultClaim = 0;
        if (GriefPrevention.instance.getPluginConfig().getClaimConfiguration().getCreationConfiguration().automaticPreferredRadius >=
            0)
        {
            areaOfDefaultClaim = (int) Math.pow(
                    GriefPrevention.instance.getPluginConfig().getClaimConfiguration().getCreationConfiguration().automaticPreferredRadius *
                    2 + 1,
                    2
            );
        }

        //if this claim is a chest claim and those are set to expire
        if (ownerData.getClaims().size() == 1 && claim.getArea() <= areaOfDefaultClaim &&
            GriefPrevention.instance.getPluginConfig().getClaimConfiguration().getExpirationConfiguration().chestClaimDays >
            0)
        {
            //if the owner has been gone at least a week, and if he has ONLY the new player claim, it will be removed
            Calendar sevenDaysAgo = Calendar.getInstance();
            sevenDaysAgo.add(
                    Calendar.DATE,
                    -GriefPrevention.instance.getPluginConfig().getClaimConfiguration().getExpirationConfiguration().chestClaimDays
            );
            if (sevenDaysAgo.getTime().after(new Date(ownerInfo.getLastPlayed())))
            {
                if (expireEventCanceled())
                {
                    return;
                }
                claim.removeSurfaceFluids(null);
                GriefPrevention.instance.dataStore.deleteClaim(claim, true, true);

                //if configured to do so, restore the land to natural
                if (GriefPrevention.instance.creativeRulesApply(claim.getLesserBoundaryCorner()) ||
                    GriefPrevention.instance.getPluginConfig().getClaimConfiguration().getRestorationConfiguration().isEnabled())
                {
                    GriefPrevention.instance.restoreClaim(claim, 0);
                }

                GriefPrevention.AddLogEntry(
                        " " + claim.getOwnerName() + "'s new player claim expired.",
                        ActivityType.ADMIN
                );
            }
        }

        //if configured to always remove claims after some inactivity period without exceptions...
        else if (
                GriefPrevention.instance.getPluginConfig().getClaimConfiguration().getExpirationConfiguration().allClaimsDays >
                0)
        {
            Calendar earliestPermissibleLastLogin = Calendar.getInstance();
            earliestPermissibleLastLogin.add(
                    Calendar.DATE,
                    -GriefPrevention.instance.getPluginConfig().getClaimConfiguration().getExpirationConfiguration().allClaimsDays
            );

            if (earliestPermissibleLastLogin.getTime().after(new Date(ownerInfo.getLastPlayed())))
            {
                if (expireEventCanceled())
                {
                    return;
                }
                //make a copy of this player's claim list
                Vector<Claim> claims = new Vector<>(ownerData.getClaims());

                //delete them
                GriefPrevention.instance.dataStore.deleteClaimsForPlayer(claim.ownerID, true);
                GriefPrevention.AddLogEntry(
                        " All of " + claim.getOwnerName() + "'s claims have expired.",
                        ActivityType.ADMIN
                );
                GriefPrevention.AddLogEntry(
                        "earliestPermissibleLastLogin#getTime: " + earliestPermissibleLastLogin.getTime(),
                        ActivityType.DEBUG,
                        true
                );
                GriefPrevention.AddLogEntry(
                        "ownerInfo#getLastPlayed: " + ownerInfo.getLastPlayed(),
                        ActivityType.DEBUG,
                        true
                );

                for (Claim claim : claims)
                {
                    //if configured to do so, restore the land to natural
                    if (GriefPrevention.instance.creativeRulesApply(claim.getLesserBoundaryCorner()) ||
                        GriefPrevention.instance.getPluginConfig().getClaimConfiguration().getRestorationConfiguration().isEnabled())
                    {
                        GriefPrevention.instance.restoreClaim(claim, 0);
                    }
                }
            }
        }
        else if (
                GriefPrevention.instance.getPluginConfig().getClaimConfiguration().getExpirationConfiguration().unusedClaimDays >
                0 && GriefPrevention.instance.creativeRulesApply(claim.getLesserBoundaryCorner()))
        {
            //avoid scanning large claims and administrative claims
            if (claim.isAdminClaim() || claim.getWidth() > 25 || claim.getHeight() > 25) return;

            //otherwise scan the claim content
            int minInvestment = 400;

            long investmentScore = claim.getPlayerInvestmentScore();

            if (investmentScore < minInvestment)
            {
                //if the owner has been gone at least a week, and if he has ONLY the new player claim, it will be removed
                Calendar sevenDaysAgo = Calendar.getInstance();
                sevenDaysAgo.add(
                        Calendar.DATE,
                        -GriefPrevention.instance.getPluginConfig().getClaimConfiguration().getExpirationConfiguration().unusedClaimDays
                );
                boolean claimExpired = sevenDaysAgo.getTime().after(new Date(ownerInfo.getLastPlayed()));
                if (claimExpired)
                {
                    if (expireEventCanceled())
                    {
                        return;
                    }
                    GriefPrevention.instance.dataStore.deleteClaim(claim, true, true);
                    GriefPrevention.AddLogEntry(
                            "Removed " + claim.getOwnerName() + "'s unused claim @ " +
                            GriefPrevention.getFriendlyLocationString(claim.getLesserBoundaryCorner()),
                            ActivityType.ADMIN
                    );

                    //restore the claim area to natural state
                    GriefPrevention.instance.restoreClaim(claim, 0);
                }
            }
        }
    }

    public boolean expireEventCanceled()
    {
        //see if any other plugins don't want this claim deleted
        ClaimExpirationEvent event = new ClaimExpirationEvent(claim);
        Bukkit.getPluginManager().callEvent(event);
        return event.isCancelled();
    }
}
