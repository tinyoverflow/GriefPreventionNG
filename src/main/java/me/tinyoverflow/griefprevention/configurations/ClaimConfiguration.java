package me.tinyoverflow.griefprevention.configurations;

import lombok.Data;
import me.tinyoverflow.griefprevention.ClaimsMode;
import org.bukkit.World;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.Map;

@ConfigSerializable
@Data
public class ClaimConfiguration
{
    @Setting
    @Comment("How the plugin should work in different worlds (Disabled, Survival, SurvivalRequiringClaims, Creative).")
    private final Map<String, ClaimsMode> worldModes = Map.of(
            "world", ClaimsMode.Survival,
            "world_nether", ClaimsMode.Disabled,
            "world_the_end", ClaimsMode.Disabled
    );

    @Setting
    @Comment("Whether the /trapped command can be used inside admin claims.")
    private final boolean allowTrappedInAdminClaims = false;

    @Setting("mobs")
    private final ClaimMobsConfiguration mobsConfiguration = new ClaimMobsConfiguration();

    @Setting("claim-blocks")
    private final ClaimBlocksConfiguration claimBlocksConfiguration = new ClaimBlocksConfiguration();

    @Setting("tools")
    private final ClaimToolsConfiguration toolsConfiguration = new ClaimToolsConfiguration();

    @Setting("expiration")
    private final ClaimExpirationConfiguration expirationConfiguration = new ClaimExpirationConfiguration();

    @Setting("command-trust-limits")
    private final ClaimCommandTrustLimitsConfiguration commandTrustLimitsConfiguration = new ClaimCommandTrustLimitsConfiguration();

    @Setting("protection")
    private final ClaimProtectionConfiguration protectionConfiguration = new ClaimProtectionConfiguration();

    @Setting("creation")
    private final ClaimCreationConfiguration creationConfiguration = new ClaimCreationConfiguration();

    @Setting("manual")
    private final ClaimManualConfiguration manualConfiguration = new ClaimManualConfiguration();

    @Setting("restoration")
    private final ClaimRestorationConfiguration restorationConfiguration = new ClaimRestorationConfiguration();

    /**
     * Determines whether protection rules apply to the specified world.
     *
     * @param world The world to check for.
     * @return True if protection is not disabled, false otherwise.
     */
    public boolean isWorldEnabled(World world)
    {
        return worldModes.containsKey(world.getName()) &&
                worldModes.get(world.getName()) != ClaimsMode.Disabled;
    }

    public ClaimsMode getWorldMode(World world)
    {
        String worldName = world.getName();
        if (worldModes.containsKey(worldName))
        {
            return worldModes.get(worldName);
        }

        return ClaimsMode.Disabled;
    }
}

