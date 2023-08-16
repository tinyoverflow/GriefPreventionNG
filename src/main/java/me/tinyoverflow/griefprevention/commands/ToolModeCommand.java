package me.tinyoverflow.griefprevention.commands;

import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.executors.CommandArguments;
import dev.jorel.commandapi.executors.PlayerCommandExecutor;
import me.tinyoverflow.griefprevention.GriefPrevention;
import me.tinyoverflow.griefprevention.PlayerData;
import me.tinyoverflow.griefprevention.ToolMode;
import me.tinyoverflow.griefprevention.utils.CaseUtil;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ToolModeCommand implements PlayerCommandExecutor
{
    private final GriefPrevention plugin;

    public ToolModeCommand(GriefPrevention plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public void run(Player player, CommandArguments args) throws WrapperCommandSyntaxException
    {
        // Extract arguments from command.
        String toolModeName = (String) args.getOptional("mode").orElse("basic");
        int toolFillRadius = (int) args.getOptional("radius").orElse(2);

        // Configure player data according to the arguments.
        PlayerData playerData = plugin.getDataStore().getPlayerData(player.getUniqueId());
        playerData.toolMode = parseToolMode(toolModeName);
        playerData.fillRadius = toolFillRadius;

        // Send a success message to the player.
        plugin.getTolker()
                .from("tool-mode." + toolModeName + ".activated")
                .with("radius", toolFillRadius)
                .send(player);
    }

    public void help(Player player, CommandArguments ignoredCommandArguments)
    {
        PlayerData playerData = plugin.getDataStore().getPlayerData(player.getUniqueId());

        plugin.getTolker()
                .from("tool-mode.help.title").send(player);

        for (ToolMode toolMode : ToolMode.values()) {
            String modeName = CaseUtil.fromMacro(toolMode.name()).toKebab();

            if (!player.hasPermission("griefprevention.command.toolmode." + modeName)) {
                continue;
            }

            TextComponent component = (TextComponent) plugin.getTolker()
                    .from("tool-mode." + modeName + ".title")
                    .build();

            plugin.getTolker()
                    .from("tool-mode.help.mode-description")
                    .with("title", component)
                    .withBool("active", playerData.toolMode == toolMode)
                    .send(player);
        }
    }

    private ToolMode parseToolMode(@NotNull String input)
    {
        return ToolMode.valueOf(CaseUtil.fromKebab(input).toMacro());
    }
}
