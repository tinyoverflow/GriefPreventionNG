package me.tinyoverflow.griefprevention.configurations;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@ConfigSerializable
public class ClaimExpirationConfiguration
{
    @Setting
    @Comment("The number of days before a claim created by a chest will be removed.")
    public int chestClaimDays = 7;

    @Setting
    @Comment("The number of days before an unused claim (nothing was build) will be removed.")
    public int unusedClaimDays = 14;

    @Setting
    @Comment("The number of days before claims of an inactive user will be removed.")
    public int allClaimsDays = 60;

    @Setting
    @Comment("Whether to restore nature when a claim gets removed.")
    public boolean restoreNature = false;

    @Setting
    @Comment("Do not automatically remove claims if at least one of the exceptions apply.")
    public ClaimExpirationExceptionConfiguration exceptions = new ClaimExpirationExceptionConfiguration();
}

