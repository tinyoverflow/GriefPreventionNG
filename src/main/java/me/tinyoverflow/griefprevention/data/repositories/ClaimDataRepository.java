package me.tinyoverflow.griefprevention.data.repositories;

import me.tinyoverflow.griefprevention.data.ClaimData;

import java.util.UUID;

public class ClaimDataRepository implements Repository<UUID, ClaimData>
{
    @Override
    public ClaimData load(UUID player)
    {
        return null;
    }

    @Override
    public boolean save(ClaimData playerData)
    {
        return false;
    }
}
