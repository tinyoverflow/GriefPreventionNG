package me.tinyoverflow.griefprevention.exceptions;

import lombok.Getter;
import me.tinyoverflow.griefprevention.data.models.UserModel;

@Getter
public class InsufficientClaimBlocksException extends Exception
{
    private final UserModel userModel;
    private final int requiredBlocks;
    private final int availableBlocks;

    public InsufficientClaimBlocksException(UserModel userModel, int requiredBlocks, int availableBlocks)
    {
        this.userModel = userModel;
        this.requiredBlocks = requiredBlocks;
        this.availableBlocks = availableBlocks;
    }
}
