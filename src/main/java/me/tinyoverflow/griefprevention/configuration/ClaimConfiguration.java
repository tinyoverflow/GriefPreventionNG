package me.tinyoverflow.griefprevention.configuration;

import org.bukkit.Material;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ConfigSerializable
public class ClaimConfiguration
{
    public Map<String, String> mode = new HashMap<>();

    public boolean preventGlobalMonsterEgg = true;
    public boolean preventTheft = true;
    public boolean preventButtonSwitches = true;
    public boolean lockWoodenDoors = false;
    public boolean lockTrapDoors = false;
    public boolean lockFenceGates = true;
    public boolean enderPearlsRequireAccessTrust = true;
    public boolean raidTriggersRequireBuildTrust = true;
    public boolean protectCreatures = true;
    public boolean protectHorses = true;
    public boolean protectDonkeys = true;
    public boolean protectLlamas = true;
    public int initialBlocks = 100;
    public int accruedPerHour = 100;
    public int maxAccruedBlocks = 80000;
    public int accruedIdleThreshold = 0;
    public int accruedIdlePercent = 0;
    public float accruedReturnRatio = 1.0f;
    public float abandonReturnRatio = 1.0f;
    public int automaticNewPlayerClaimsRadius = 4;
    public int automaticNewPlayerClaimsRadiusMinimum = 0;
    public int extendIntoGroundDistance = 5;
    public int minimumWidth = 5;
    public int minimumArea = 100;
    public int maximumDepth = Integer.MIN_VALUE;
    public Material investigationTool = Material.STICK;
    public Material modificationTool = Material.GOLDEN_SHOVEL;
    public ClaimExpirationConfiguration expiration = new ClaimExpirationConfiguration();
    public boolean allowTrappedInAdminClaims = false;
    public int maximumNumberOfClaimsPerPlayer = 0;
    public boolean creationRequiredWorldGuardPermission = true;
    public List<String> commandsRequiringAccessTrust = List.of("/sethome");
    public boolean deliverManuals = true;
    public int manualDeliveryDelaySeconds = 30;
    public boolean ravagersBreakBlocks = true;
    public boolean fireSpeadInClaims = false;
    public boolean fireDamagesInClaims = false;
    public boolean lecternReadingRequiresAccessTrust = true;
}
