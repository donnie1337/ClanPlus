package com.donnie1337.clanplus;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

import java.util.Arrays;

public final class ClanListener implements Listener {
    private final ClanPlus plugin;
    public ClanListener(ClanPlus plugin) { this.plugin = plugin; }

    private String clean(String s) { return ChatColor.stripColor(s); }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        String title = clean(e.getView().getTitle());
        if (title.startsWith("Baú da clan:")) return;
        if (title.startsWith("Clan de ")) {
            e.setCancelled(true);
            Clan c = plugin.clans().byPlayer(p.getUniqueId());
            if (c == null) { p.closeInventory(); return; }
            switch (e.getRawSlot()) {
                case 10 -> { p.closeInventory(); p.sendMessage(plugin.msg("prefix") + ChatColor.WHITE + "Informações:"); p.sendMessage(ChatColor.GRAY + "Nome: " + ChatColor.WHITE + c.name()); p.sendMessage(ChatColor.GRAY + "Tag: " + ChatColor.GOLD + c.tag()); p.sendMessage(ChatColor.GRAY + "Membros: " + ChatColor.WHITE + c.members().size()); }
                case 14 -> { p.closeInventory(); p.performCommand("clan bau"); }
                case 16 -> { p.closeInventory(); p.performCommand("clan home"); }
                case 22 -> { p.closeInventory(); p.sendMessage(ChatColor.YELLOW + "Use /clan tag <tag> para alterar a tag."); }
                case 24 -> { if (c.role(p.getUniqueId()).canManage()) plugin.clans().save(); p.closeInventory(); p.performCommand("clan config"); }
            }
        } else if (title.equals("Configuração da clan")) {
            e.setCancelled(true);
            Clan c = plugin.clans().byPlayer(p.getUniqueId());
            if (c == null || !c.role(p.getUniqueId()).canManage()) { p.closeInventory(); return; }
            if (e.getRawSlot() == 11) { c.setFriendlyFire(!c.friendlyFire()); plugin.clans().save(); p.closeInventory(); p.performCommand("clan config"); }
            if (e.getRawSlot() == 15) { p.closeInventory(); p.performCommand("clan menu"); }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        String title = clean(e.getView().getTitle());
        if (!title.startsWith("Baú da clan:")) return;
        if (!(e.getPlayer() instanceof Player p)) return;
        Clan c = plugin.clans().byPlayer(p.getUniqueId());
        if (c == null) return;
        c.setChest(Arrays.copyOf(e.getInventory().getContents(), 27));
        plugin.clans().save();
    }
}
