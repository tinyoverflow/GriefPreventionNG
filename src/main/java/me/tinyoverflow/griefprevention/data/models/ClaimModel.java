package me.tinyoverflow.griefprevention.data.models;

import lombok.Data;
import me.tinyoverflow.griefprevention.ClaimPermission;
import org.bukkit.OfflinePlayer;

import java.util.HashMap;
import java.util.UUID;

@Data
public class ClaimModel implements Model<UUID>
{
    private UUID id;
    private OfflinePlayer owner;
    private ClaimModel parentClaim;
    private ClaimBoundaries boundaries;
    private HashMap<OfflinePlayer, ClaimPermission> permissions;

    public ClaimModel()
    {
        id = UUID.randomUUID();
    }

    public ClaimModel(UUID id)
    {
        this.id = id;
    }
}
