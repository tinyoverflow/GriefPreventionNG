package me.tinyoverflow.griefprevention.configuration;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.List;

@ConfigSerializable
public class ClaimCommandTrustLimitsConfiguration
{
    @Setting
    @Comment("Require access trust to be able to execute these commands inside a claim.")
    public List<String> accessTrust = List.of("/sethome");
}
