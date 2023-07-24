package me.tinyoverflow.griefprevention.listeners.claim;

import me.tinyoverflow.griefprevention.Claim;
import me.tinyoverflow.griefprevention.ClaimPermission;
import me.tinyoverflow.griefprevention.GriefPrevention;
import me.tinyoverflow.griefprevention.Messages;
import me.tinyoverflow.griefprevention.events.ClaimPermissionCheckEvent;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class ClaimPermissionCheckListener implements Listener
{

    @EventHandler(priority = EventPriority.LOWEST)
    public void onClaimPermissionCheck(ClaimPermissionCheckEvent event)
    {
        if (event.getRequiredPermission() == ClaimPermission.Manage) return;

        Player player = event.getCheckedPlayer();

        // Player must be online to use siege features.
        if (player == null) return;

        Claim claim = event.getClaim();

        // Admin claims cannot be sieged.
        if (claim.isAdminClaim()) return;

        // Claim modification during siege is not allowed.
        if (event.getRequiredPermission() == ClaimPermission.Edit)
        {
            if (claim.siegeData != null)
                event.setDenialReason(() -> GriefPrevention.instance.dataStore.getMessage(Messages.NoModifyDuringSiege));
            return;
        }

        // Following a siege where the defender lost, the claim will allow everyone access for a time.
        if (event.getRequiredPermission() == ClaimPermission.Access)
        {
            if (claim.doorsOpen)
                event.setDenialReason(null);
            return;
        }

        // If under siege, nobody accesses containers.
        if (event.getRequiredPermission() == ClaimPermission.Inventory)
        {
            // Trying to access inventory in a claim may extend an existing siege to include this claim.
            GriefPrevention.instance.dataStore.tryExtendSiege(player, claim);

            if (claim.siegeData != null)
                event.setDenialReason(() -> GriefPrevention.instance.dataStore.getMessage(Messages.NoContainersSiege, claim.siegeData.attacker.getName()));

            return;
        }

        // When a player tries to build in a claim, if he's under siege, the siege may extend to include the new claim.
        GriefPrevention.instance.dataStore.tryExtendSiege(player, claim);

        // If claim is not under siege and doors are not open, use default behavior.
        if (claim.siegeData == null && !claim.doorsOpen)
            return;

        // If under siege, some blocks will be breakable.
        Material broken = null;
        if (event.getTriggeringEvent() instanceof BlockBreakEvent)
            broken = ((BlockBreakEvent) event.getTriggeringEvent()).getBlock().getType();
        else if (event.getTriggeringEvent() instanceof Claim.CompatBuildBreakEvent)
        {
            Claim.CompatBuildBreakEvent triggeringEvent = (Claim.CompatBuildBreakEvent) event.getTriggeringEvent();
            if (triggeringEvent.isBreak())
                broken = triggeringEvent.getMaterial();
        }
        else if (event.getTriggeringEvent() instanceof PlayerInteractEvent)
        {
            PlayerInteractEvent triggeringEvent = (PlayerInteractEvent) event.getTriggeringEvent();
            if (triggeringEvent.getAction() == Action.PHYSICAL && triggeringEvent.getClickedBlock() != null
                    && triggeringEvent.getClickedBlock().getType() == Material.TURTLE_EGG)
                broken = Material.TURTLE_EGG;
        }

        if (broken != null)
        {
            // Error messages for siege mode.
            if (!GriefPrevention.instance.getPluginConfig().getSiegeConfiguration().isBreakableBlock(broken))
                event.setDenialReason(() -> GriefPrevention.instance.dataStore.getMessage(Messages.NonSiegeMaterial));
            else if (player.getUniqueId().equals(claim.ownerID))
                event.setDenialReason(() -> GriefPrevention.instance.dataStore.getMessage(Messages.NoOwnerBuildUnderSiege));
            return;
        }

        // No building while under siege.
        if (claim.siegeData != null)
            event.setDenialReason(() -> GriefPrevention.instance.dataStore.getMessage(Messages.NoBuildUnderSiege, claim.siegeData.attacker.getName()));

    }

}
