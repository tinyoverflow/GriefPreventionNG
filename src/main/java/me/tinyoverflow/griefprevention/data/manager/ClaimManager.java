package me.tinyoverflow.griefprevention.data.manager;

import me.tinyoverflow.griefprevention.data.models.ClaimModel;
import me.tinyoverflow.griefprevention.data.repositories.ClaimRepository;

import java.util.ArrayList;
import java.util.List;

public class ClaimManager
{
    private final ClaimRepository repository;
    private final List<ClaimModel> claims;

    public ClaimManager(ClaimRepository repository)
    {
        this.repository = repository;
        claims = new ArrayList<>();
    }

    public List<ClaimModel> getClaims()
    {
        return claims;
    }
}
