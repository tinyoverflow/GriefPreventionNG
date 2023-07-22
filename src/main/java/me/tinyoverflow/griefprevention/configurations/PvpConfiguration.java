package me.tinyoverflow.griefprevention.configurations;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.List;

@ConfigSerializable
public class PvpConfiguration
{
    @Setting
    @Comment("Prevent spawn-trapping by protecting freshly spawned players.")
    private boolean protectFreshSpawns = true;

    @Setting
    @Comment("Whether to punish logouts in combat by instantly killing the player who logged out.")
    private boolean punishLogouts = true;

    @Setting
    @Comment("How many seconds a player will be in combat mode after the last attack.")
    private int combatTimeout = 15;

    @Setting
    @Comment("Allow players to drop items while in combat mode.")
    private boolean allowCombatItemDrop = false;

    @Setting
    @Comment("List of commands that cannot be used while in combat.")
    private List<String> restrictedCommands = List.of("/home", "/vanish", "/spawn", "/tpa");

    @Setting
    @Comment("Whether to protect players from PvP in user claims.")
    private boolean protectInPlayerClaims = false;

    @Setting
    @Comment("Whether to protect players from PvP in admin claims.")
    private boolean protectInAdminClaims = false;

    @Setting
    @Comment("Whether to protect players from PvP in admin subdivisions.")
    private boolean protectInAdminSubdivisions = false;
}
