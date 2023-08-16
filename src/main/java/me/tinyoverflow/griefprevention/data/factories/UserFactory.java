package me.tinyoverflow.griefprevention.data.factories;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.tinyoverflow.griefprevention.data.models.UserModel;

import java.util.UUID;

@Setter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UserFactory
{
    private static int defaultAccruedBlocks = 0;
    private static int defaultAccruedLimit = 0;
    private static int defaultBonusBlocks = 0;

    private UUID uuid;
    private int accruedBlocks;
    private int accruedLimit;
    private int bonusBlocks;

    private UserFactory(UUID uuid)
    {
        this.uuid = uuid;
    }

    public static void setDefaults(int accruedBlocks, int accruedLimit, int bonusBlocks)
    {
        defaultAccruedBlocks = accruedBlocks;
        defaultAccruedLimit = accruedLimit;
        defaultBonusBlocks = bonusBlocks;
    }

    public static UserFactory builder(UUID uuid)
    {
        return new UserFactory(uuid);
    }

    public UserFactory applyDefaults()
    {
        accruedBlocks = defaultAccruedBlocks;
        accruedLimit = defaultAccruedLimit;
        bonusBlocks = defaultBonusBlocks;

        return this;
    }

    public UserModel build()
    {
        UserModel userModel = new UserModel(uuid);
        userModel.setAccruedBlocks(accruedBlocks);
        userModel.setAccruedBlockLimit(accruedLimit);
        userModel.setBonusBlocks(bonusBlocks);

        return userModel;
    }
}
