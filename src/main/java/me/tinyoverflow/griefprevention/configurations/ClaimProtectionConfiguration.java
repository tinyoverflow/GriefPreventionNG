package me.tinyoverflow.griefprevention.configurations;

import lombok.Data;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@ConfigSerializable
@Data
public class ClaimProtectionConfiguration
{
    @Setting("protect-vehicles")
    @Comment("Requires access trust to break vehicles.")
    private final boolean protectingVehicles = true;

    @Setting("lock-containers")
    @Comment("Requires access trust to access all kinds of containers.")
    private final boolean lockContainers = true;

    @Setting("lock-wooden-doors")
    @Comment("Requires access trust to interact with wooden doors.")
    public final boolean lockingWoodenDoors = true;

    @Setting("lock-fence-gates")
    @Comment("Requires access trust to interact with fence gates.")
    private final boolean lockingFenceGates = true;

    @Setting("lock-trap-doors")
    @Comment("Requires access trust to interact with trap doors.")
    private final boolean lockingTrapDoors = true;

    @Setting("lock-switches")
    @Comment("Requires access trust to interact with buttons, levers and pressure plates.")
    private final boolean lockingSwitches = true;

    @Setting("lock-lecterns")
    @Comment("Requires access trust to interact with a lectern.")
    private final boolean lockingLecterns = true;

    @Setting("prevent-monster-eggs")
    @Comment("Allow placing monster eggs regardless of trust.")
    private final boolean preventingMonsterEggs = true;

    @Setting("prevent-ender-pearls")
    @Comment("Requires access trust to use an ender pearl to teleport into a claim.")
    private final boolean preventingEnderPearls = true;

    @Setting("prevent-raid-triggers")
    @Comment("Requires build trust to be able to trigger a raid.")
    private final boolean preventingRaidTriggers = true;

    @Setting("prevent-ravager-damage")
    @Comment("Whether or not to prevent ravagers from damaging blocks inside a claim.")
    private final boolean preventingRavagerDamage = false;

    @Setting("prevent-fire-spread")
    @Comment("Whether fire will spread inside claims.")
    private final boolean preventingFireSpread = true;

    @Setting("prevent-fire-damage")
    @Comment("Whether fire will do damage inside claims.")
    private final boolean preventingFireDamage = true;

    @Setting("prevent-non-player-portals")
    @Comment("Prevent creation of portals inside claims when the creating player is unknown.")
    private final boolean preventingNonPlayerPortals = true;

    @Setting("prevent-villager-trades")
    @Comment("Require access trust to be able to trade with claimed villagers.")
    private final boolean preventingVillagerTrades = true;
}

