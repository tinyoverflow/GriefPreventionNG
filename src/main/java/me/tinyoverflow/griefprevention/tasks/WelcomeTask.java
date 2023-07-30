package me.tinyoverflow.griefprevention.tasks;

import me.tinyoverflow.griefprevention.GriefPrevention;
import me.tinyoverflow.griefprevention.Messages;
import me.tinyoverflow.griefprevention.TextMode;
import me.tinyoverflow.griefprevention.datastore.DataStore;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

public class WelcomeTask implements Runnable
{
    private final Player player;

    public WelcomeTask(Player player)
    {
        this.player = player;
    }

    @Override
    public void run()
    {
        //abort if player has logged out since this task was scheduled
        if (!player.isOnline()) return;

        //offer advice and a helpful link
        GriefPrevention.sendMessage(player, TextMode.INSTRUCTION, Messages.AvoidGriefClaimLand);
        GriefPrevention.sendMessage(
                player,
                TextMode.INSTRUCTION,
                Messages.SurvivalBasicsVideo2,
                DataStore.SURVIVAL_VIDEO_URL
        );

        //give the player a reference book for later
        if (GriefPrevention.instance.getPluginConfig().getClaimConfiguration().getManualConfiguration().enabled)
        {
            ItemFactory factory = Bukkit.getItemFactory();
            BookMeta meta = (BookMeta) factory.getItemMeta(Material.WRITTEN_BOOK);

            DataStore datastore = GriefPrevention.instance.dataStore;
            meta.setAuthor(datastore.getMessage(Messages.BookAuthor));
            meta.setTitle(datastore.getMessage(Messages.BookTitle));

            StringBuilder page1 = new StringBuilder();
            String URL = datastore.getMessage(Messages.BookLink, DataStore.SURVIVAL_VIDEO_URL);
            String intro = datastore.getMessage(Messages.BookIntro);

            page1.append(URL).append("\n\n");
            page1.append(intro).append("\n\n");
            String editToolName = GriefPrevention.instance.getPluginConfig().getClaimConfiguration().getToolsConfiguration().getModificationTool().name().replace(
                    '_',
                    ' '
            ).toLowerCase();
            String infoToolName = GriefPrevention.instance.getPluginConfig().getClaimConfiguration().getToolsConfiguration().getInvestigationTool().name().replace(
                    '_',
                    ' '
            ).toLowerCase();
            String configClaimTools = datastore.getMessage(Messages.BookTools, editToolName, infoToolName);
            page1.append(configClaimTools);
            if (GriefPrevention.instance.getPluginConfig().getClaimConfiguration().getCreationConfiguration().automaticPreferredRadius <
                0)
            {
                page1.append(datastore.getMessage(Messages.BookDisabledChestClaims));
            }

            String page2 = datastore.getMessage(Messages.BookUsefulCommands) + "\n\n" +
                           "/Trust /UnTrust /TrustList\n" +
                           "/ClaimsList\n" +
                           "/AbandonClaim\n\n" +
                           "/Claim /ExtendClaim\n" +
                           "/IgnorePlayer\n\n" +
                           "/SubdivideClaims\n" +
                           "/AccessTrust\n" +
                           "/ContainerTrust\n" +
                           "/PermissionTrust";

            meta.setPages(page1.toString(), page2);

            ItemStack item = new ItemStack(Material.WRITTEN_BOOK);
            item.setItemMeta(meta);
            player.getInventory().addItem(item);
        }
    }
}
