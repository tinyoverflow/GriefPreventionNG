package me.tinyoverflow.griefprevention.listeners.player;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.tinyoverflow.griefprevention.GriefPrevention;
import me.tinyoverflow.griefprevention.Messages;
import me.tinyoverflow.griefprevention.TextMode;
import me.tinyoverflow.griefprevention.datastore.DataStore;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.regex.Pattern;

public class PlayerChatListener implements Listener
{
    private final GriefPrevention plugin;
    private final DataStore dataStore;
    private final Pattern howToClaimPattern;

    public PlayerChatListener(GriefPrevention plugin, DataStore dataStore)
    {
        this.plugin = plugin;
        this.dataStore = dataStore;

        howToClaimPattern = Pattern.compile(dataStore.getMessage(Messages.HowToClaimRegex), Pattern.CASE_INSENSITIVE);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    synchronized void onPlayerChat(AsyncChatEvent event)
    {
        Player player = event.getPlayer();
        String message = event.originalMessage().toString();

        handleHowToClaimQuestion(player, message);
        handleTrappedQuestion(player, message);
    }

    private void handleTrappedQuestion(Player player, String message)
    {
        if (dataStore.getMessage(Messages.TrappedChatKeyword).isEmpty())
        {
            return;
        }

        String[] checkWords = dataStore.getMessage(Messages.TrappedChatKeyword).split(";");
        for (String checkWord : checkWords)
        {
            if (message.contains("/trapped") || !message.contains(checkWord))
            {
                continue;
            }

            GriefPrevention.sendMessage(player, TextMode.INFO, Messages.TrappedInstructions, 10L);
            break;
        }
    }

    private void handleHowToClaimQuestion(Player player, String message)
    {
        if (!howToClaimPattern.matcher(message).matches())
        {
            return;
        }

        String videoUrl = plugin.creativeRulesApply(player.getLocation())
                ? DataStore.CREATIVE_VIDEO_URL
                : DataStore.SURVIVAL_VIDEO_URL;

        GriefPrevention.sendMessage(player, TextMode.INFO, Messages.CreativeBasicsVideo2, 10L, videoUrl);
    }
}
