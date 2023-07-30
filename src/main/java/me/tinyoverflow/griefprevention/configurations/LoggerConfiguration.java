package me.tinyoverflow.griefprevention.configurations;

import lombok.Data;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@ConfigSerializable
@Data
public class LoggerConfiguration
{
    @Setting
    @Comment("The amount of days to retain logs for. Set to 0 to keep all logs.")
    private final int daysToKeep = 7;

    @Setting
    @Comment("Whether social interactions should be logged.")
    private final boolean socialEnabled = true;

    @Setting
    @Comment("Whether suspicious interactions should be logged.")
    private final boolean suspiciousEnabled = true;

    @Setting
    @Comment("Whether admin interactions should be logged.")
    private final boolean adminEnabled = true;

    @Setting
    @Comment("Whether debug information should be logged. Exceptions will always be logged.")
    private final boolean debugEnabled = false;

    /**
     * Determines whether the log pruning feature is enabled.
     *
     * @return {@code true} if enabled, {@code false} if not.
     */
    public boolean isLogPruningEnabled()
    {
        //noinspection ConstantValue
        return daysToKeep > 0;
    }
}
