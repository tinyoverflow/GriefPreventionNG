package me.tinyoverflow.griefprevention.commands;

import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.executors.CommandArguments;
import dev.jorel.commandapi.executors.PlayerCommandExecutor;
import me.tinyoverflow.tolker.Tolker;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

public class BookDemoCommand implements PlayerCommandExecutor
{
    private final Tolker tolker;

    public BookDemoCommand(Tolker tolker)
    {
        this.tolker = tolker;
    }

    @Override
    public void run(Player player, CommandArguments commandArguments) throws WrapperCommandSyntaxException
    {
        ItemFactory factory = Bukkit.getItemFactory();
        BookMeta bookMeta = (BookMeta) factory.getItemMeta(Material.WRITTEN_BOOK);
        bookMeta.setTitle("Demo Book");
        bookMeta.setAuthor("Demo Author");
        bookMeta.addPages(Component.text("Demo Book Content"));

        ItemStack book = new ItemStack(Material.WRITTEN_BOOK, 1);
        book.setItemMeta(bookMeta);

        player.openBook(book);
    }
}
