package me.tinyoverflow.griefprevention.data.repositories;

import me.tinyoverflow.griefprevention.data.models.ClaimModel;
import me.tinyoverflow.griefprevention.data.models.UserModel;

import java.util.List;
import java.util.UUID;

public interface ClaimRepository extends Repository<ClaimModel, UUID>
{
    List<ClaimModel> getByUser(UserModel userModel);

    List<ClaimModel> all();

    List<ClaimModel> childrenOf(ClaimModel parentClaim);
}
