package me.tinyoverflow.griefprevention.data.repositories;

public interface Repository<K, T>
{
    /**
     * Loads a {@link K} object from the data store.
     *
     * @param id The {@link T} to load the data for.
     * @return The hydrated {@link K} instance.
     */
    K load(T id);

    /**
     * Saves a {@link K} object to the data store.
     *
     * @param model The {@link K} instance to save.
     * @return Whether the saving was successful.
     */
    boolean save(K model);
}
