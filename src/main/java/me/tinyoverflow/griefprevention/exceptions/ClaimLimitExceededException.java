package me.tinyoverflow.griefprevention.exceptions;

import lombok.Getter;
import me.tinyoverflow.griefprevention.data.models.UserModel;

@Getter
public class ClaimLimitExceededException extends Exception
{
    private final UserModel player;
    private final int maximumClaims;

    public ClaimLimitExceededException(UserModel player, int maximumClaims)
    {
        this.player = player;
        this.maximumClaims = maximumClaims;
    }
}
