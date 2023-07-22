package me.tinyoverflow.griefprevention.configurations;

import me.tinyoverflow.griefprevention.ClaimsMode;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.Map;

@ConfigSerializable
public class ClaimConfiguration
{
    @Setting
    @Comment("How the plugin should work in different worlds (Disabled, Survival, SurvivalRequiringClaims, Creative).")
    private Map<String, ClaimsMode> mode = Map.of(
            "world", ClaimsMode.Survival,
            "world_nether", ClaimsMode.Disabled,
            "world_the_end", ClaimsMode.Disabled
    );

    @Setting
    @Comment("Whether the /trapped command can be used inside admin claims.")
    private boolean allowTrappedInAdminClaims = false;

    @Setting("mobs")
    private ClaimMobsConfiguration mobs = new ClaimMobsConfiguration();

    @Setting("claim-blocks")
    private ClaimBlocksConfiguration claimBlocks = new ClaimBlocksConfiguration();

    @Setting("tools")
    private ClaimToolsConfiguration tools = new ClaimToolsConfiguration();

    @Setting("expiration")
    private ClaimExpirationConfiguration expiration = new ClaimExpirationConfiguration();

    @Setting("command-trust-limits")
    private ClaimCommandTrustLimitsConfiguration commandTrustLimits = new ClaimCommandTrustLimitsConfiguration();

    @Setting("protection")
    private ClaimProtectionConfiguration protection = new ClaimProtectionConfiguration();

    @Setting("creation")
    private ClaimCreationConfiguration creation = new ClaimCreationConfiguration();

    @Setting("manual")
    private ClaimManualConfiguration manual = new ClaimManualConfiguration();

    public Map<String, String> getWorldModes()
    {
        return this.mode;
    }

    public boolean isTrappedInAdminClaimsAllowed()
    {
        return this.allowTrappedInAdminClaims;
    }

    public ClaimMobsConfiguration getMobs()
    {
        return this.mobs;
    }

    public ClaimBlocksConfiguration getClaimBlocks()
    {
        return this.claimBlocks;
    }

    public ClaimToolsConfiguration getTools()
    {
        return this.tools;
    }

    public ClaimExpirationConfiguration getExpiration()
    {
        return this.expiration;
    }

    public ClaimCommandTrustLimitsConfiguration getCommandTrustLimits()
    {
        return this.commandTrustLimits;
    }

    public ClaimProtectionConfiguration getProtection()
    {
        return this.protection;
    }

    public ClaimCreationConfiguration getCreation()
    {
        return this.creation;
    }

    public ClaimManualConfiguration getManual()
    {
        return this.manual;
    }
}

