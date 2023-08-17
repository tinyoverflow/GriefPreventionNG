package me.tinyoverflow.griefprevention.exceptions;

import lombok.Getter;
import me.tinyoverflow.griefprevention.data.models.ClaimModel;

@Getter
public class ClaimOverlapException extends Exception
{
    private final ClaimModel otherClaim;

    public ClaimOverlapException(ClaimModel otherClaim)
    {
        this.otherClaim = otherClaim;
    }
}
