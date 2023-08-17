package me.tinyoverflow.griefprevention.data.repositories;

import me.tinyoverflow.griefprevention.data.models.UserModel;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

public interface UserRepository extends Repository<UserModel, UUID>
{
    UserModel get(OfflinePlayer player);
}
