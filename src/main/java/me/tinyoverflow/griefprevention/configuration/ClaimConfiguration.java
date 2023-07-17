package me.tinyoverflow.griefprevention.configuration;

import org.bukkit.Material;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ConfigSerializable
public class ClaimConfiguration
{
    @Setting
    @Comment("How the plugin should work in different worlds (Disabled, Survival, SurvivalRequiringClaims, Creative).")
    public Map<String, String> mode = Map.of("world", "Survival", "world_nether", "Disabled", "world_the_end", "Disabled");

    public ClaimMobsConfiguration mobs = new ClaimMobsConfiguration();
    public ClaimBlocksConfiguration claimBlocks = new ClaimBlocksConfiguration();
    public ClaimToolsConfiguration tools = new ClaimToolsConfiguration();
    public ClaimExpirationConfiguration expiration = new ClaimExpirationConfiguration();
    public ClaimCommandTrustLimitsConfiguration commandTrustLimits = new ClaimCommandTrustLimitsConfiguration();
    public ClaimProtectionConfiguration protection = new ClaimProtectionConfiguration();
    public ClaimCreationConfiguration creation = new ClaimCreationConfiguration();
    public ClaimManualConfiguration manual = new ClaimManualConfiguration();

    @Setting
    @Comment("Whether the /trapped command can be used inside admin claims.")
    public boolean allowTrappedInAdminClaims = false;
}
