package me.tinyoverflow.griefprevention.data.models;

import lombok.Data;

import java.util.UUID;

@Data
public class UserModel implements Model<UUID>
{
    private final UUID id;

    private int accruedBlocks = 0;
    private int accruedBlockLimit = 0;
    private int bonusBlocks = 0;

    public UserModel(UUID id)
    {
        this.id = id;
    }
}
