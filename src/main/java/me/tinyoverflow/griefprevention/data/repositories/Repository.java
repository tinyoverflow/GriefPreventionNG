package me.tinyoverflow.griefprevention.data.repositories;

import java.util.Optional;

public interface Repository<K, T>
{
    /**
     * Loads all objects from the data store.
     */
    void load();

    /**
     * Return the given item from the data store.
     *
     * @param key Identifier of the item to load.
     */
    Optional<K> get(T key);

    /**
     * Saves a {@link K} object to the data store.
     *
     * @param model The {@link K} instance to save.
     * @return Whether the saving was successful.
     */
    boolean save(K model);

    /**
     * Saves all objects to the data store.
     */
    void save();
}
