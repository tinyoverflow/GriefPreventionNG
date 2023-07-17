package me.tinyoverflow.griefprevention.configurations;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@ConfigSerializable
public class ClaimProtectionConfiguration
{
    @Setting
    @Comment("Requires access trust to access all kinds of containers.")
    public boolean lockContainers = true;

    @Setting
    @Comment("Requires access trust to interact with wooden doors.")
    public boolean lockWoodenDoors = true;

    @Setting
    @Comment("Requires access trust to interact with fence gates.")
    public boolean lockFenceGates = true;

    @Setting
    @Comment("Requires access trust to interact with trap doors.")
    public boolean lockTrapDoors = true;

    @Setting
    @Comment("Requires access trust to interact with buttons, levers and pressure plates.")
    public boolean lockSwitches = true;

    @Setting
    @Comment("Requires access trust to interact with a lectern.")
    public boolean lockLecterns = true;

    @Setting
    @Comment("Allow placing monster eggs regardless of trust.")
    public boolean preventMonsterEggs = true;

    @Setting
    @Comment("Requires access trust to use an ender pearl to teleport into a claim.")
    public boolean preventEnderPearls = true;

    @Setting
    @Comment("Requires build trust to be able to trigger a raid.")
    public boolean preventRaidTriggers = true;

    @Setting
    @Comment("Whether or not to prevent ravagers from damaging blocks inside a claim.")
    public boolean preventRavagerDamage = false;

    @Setting
    @Comment("Whether fire will spread inside claims.")
    public boolean preventFireSpread = true;

    @Setting
    @Comment("Whether fire will do damage inside claims.")
    public boolean preventFireDamage = true;
}

