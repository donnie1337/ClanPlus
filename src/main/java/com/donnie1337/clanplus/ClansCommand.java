package com.donnie1337.clanplus;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class ClansCommand implements CommandExecutor {
    private final ClanPlus plugin;
    public ClansCommand(ClanPlus plugin) { this.plugin = plugin; }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("Apenas jogadores podem usar este comando."); return true; }
        List<Clan> clans = new ArrayList<>(plugin.clans().all());
        int size = Math.min(54, Math.max(9, ((clans.size() + 8) / 9) * 9));
        Inventory inv = Bukkit.createInventory(null, size, ChatColor.translateAlternateColorCodes('&', plugin.raw("locator-title")));
        int slot = 0;
        for (Clan c : clans) {
            if (slot >= size) break;
            ItemStack item = new ItemStack(Material.NAME_TAG);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.GOLD + c.name() + ChatColor.DARK_GRAY + " [" + ChatColor.GOLD + c.tag() + ChatColor.DARK_GRAY + "]");
            meta.setLore(List.of(ChatColor.GRAY + "Membros: " + ChatColor.WHITE + c.members().size(), ChatColor.GRAY + "Online: " + ChatColor.WHITE + c.onlineCount(), ChatColor.GRAY + "Líder: " + ChatColor.WHITE + String.valueOf(Bukkit.getOfflinePlayer(c.owner()).getName())));
            item.setItemMeta(meta); inv.setItem(slot++, item);
        }
        p.openInventory(inv);
        return true;
    }
}
