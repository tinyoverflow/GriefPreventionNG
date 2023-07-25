package me.tinyoverflow.griefprevention.configurations;

import lombok.Data;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.List;

@ConfigSerializable
@Data
public class PvpConfiguration
{
    @Setting
    @Comment("Worlds that should be affected by the following rules.")
    private final List<String> worlds = List.of("world");

    @Setting
    @Comment("Prevent spawn-trapping by protecting freshly spawned players.")
    private final boolean protectFreshSpawns = true;

    @Setting
    @Comment("Whether to punish logouts in combat by instantly killing the player who logged out.")
    private final boolean punishLogouts = true;

    @Setting
    @Comment("How many seconds a player will be in combat mode after the last attack.")
    private final int combatTimeout = 15;

    @Setting
    @Comment("Allow players to drop items while in combat mode.")
    private final boolean allowCombatItemDrop = false;

    @Setting
    @Comment("List of commands that cannot be used while in combat.")
    private final List<String> restrictedCommands = List.of("/home", "/vanish", "/spawn", "/tpa");

    @Setting
    @Comment("Whether to protect players from PvP in user claims.")
    private final boolean protectInPlayerClaims = false;

    @Setting
    @Comment("Whether to protect players from PvP in admin claims.")
    private final boolean protectInAdminClaims = false;

    @Setting
    @Comment("Whether to protect players from PvP in admin subdivisions.")
    private final boolean protectInAdminSubdivisions = false;

    @Setting
    @Comment("Whether to allow placing lava near players in a PvP world.")
    private final boolean allowLavaNearPlayers = true;

    @Setting
    @Comment("Whether to allow placing lava near players in a Non-PvP world.")
    private final boolean allowLavaNearPlayersNonPvP = false;

    @Setting
    @Comment("Whether to allow igniting fire near players in a PvP world.")
    private final boolean allowFireNearPlayers = true;

    @Setting
    @Comment("Whether to allow igniting fire near players in a Non-PvP world.")
    private final boolean allowFireNearPlayersNonPvP = false;

    @Setting
    @Comment("Whether to allow damaging pets outside of claims in a PvP world.")
    private final boolean protectPets = false;
}
