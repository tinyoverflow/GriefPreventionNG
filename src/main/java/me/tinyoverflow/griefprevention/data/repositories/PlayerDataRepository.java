package me.tinyoverflow.griefprevention.data.repositories;

import me.tinyoverflow.griefprevention.data.PlayerData;

import java.util.UUID;

public class PlayerDataRepository implements Repository<UUID, PlayerData>
{
    @Override
    public PlayerData load(UUID player)
    {
        return null;
    }

    @Override
    public boolean save(PlayerData playerData)
    {
        return false;
    }
}
