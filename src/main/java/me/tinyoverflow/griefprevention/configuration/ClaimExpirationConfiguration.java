package me.tinyoverflow.griefprevention.configuration;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@ConfigSerializable
public class ClaimExpirationConfiguration
{
    public int chestClaimDays = 7;
    public int unusedClaimDays = 14;

    public int allClaimsDaysInactive = 60;
    public int allClaimsExceptWhenOwnerHasTotalClaimBlocks = 10000;
    public int allClaimsExceptWhenOwnerHasBonusClaimBlocks = 5000;
    public boolean automaticNatureRestorationInSurvivalWorlds = false;
}
