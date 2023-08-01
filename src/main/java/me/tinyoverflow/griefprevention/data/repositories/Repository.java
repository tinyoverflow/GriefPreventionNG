package me.tinyoverflow.griefprevention.data.repositories;

import me.tinyoverflow.griefprevention.data.Saveable;

import java.util.UUID;

public interface Repository<K, T extends Saveable<? extends K>>
{
    T load(UUID player);

    boolean save(T playerData);
}
