package com.donnie1337.clanplus;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

import java.util.Arrays;

public final class ClanListener implements Listener {
    private final ClanPlus plugin;
    public ClanListener(ClanPlus plugin) { this.plugin = plugin; }

    private String clean(String s) { return ChatColor.stripColor(s); }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player victim = e.getEntity();
        plugin.clans().recordDeath(victim.getUniqueId());
        Player killer = victim.getKiller();
        if (killer != null && !killer.getUniqueId().equals(victim.getUniqueId())) plugin.clans().recordKill(killer.getUniqueId());
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        String title = clean(e.getView().getTitle());
        if (title.startsWith("Baú da clan:")) return;

        if (title.equals("ᴄʟᴀɴ")) {
            e.setCancelled(true);
            switch (e.getRawSlot()) {
                case 10 -> { p.closeInventory(); if (plugin.clans().byPlayer(p.getUniqueId()) == null) p.sendMessage(plugin.msg("create-from-gui")); else p.performCommand("clan menu"); }
                case 12 -> new ClanGui(plugin).openInvites(p);
                case 14 -> new ClanGui(plugin).openTop(p);
                case 16 -> new ClanGui(plugin).openAll(p);
                case 22 -> new ClanGui(plugin).openKdr(p);
                case 26 -> p.closeInventory();
                default -> { }
            }
            return;
        }

        if (title.equals("Convites recebidos")) {
            e.setCancelled(true);
            if (e.getRawSlot() == 49) { new ClanGui(plugin).openMain(p); return; }
            if (e.getRawSlot() < 0 || e.getRawSlot() >= e.getInventory().getSize()) return;
            if (e.getCurrentItem() == null || !e.getCurrentItem().hasItemMeta() || e.getCurrentItem().getItemMeta().getLore() == null) return;
            for (String line : e.getCurrentItem().getItemMeta().getLore()) {
                String clean = ChatColor.stripColor(line);
                if (!clean.startsWith("ID: ")) continue;
                try {
                    int id = Integer.parseInt(clean.substring(4).trim());
                    p.closeInventory();
                    p.performCommand(e.isShiftClick() ? "clan recusar " + id : "clan aceitar " + id);
                } catch (NumberFormatException ignored) { }
                return;
            }
            return;
        }

        if (title.equals("Clans mais top") || title.equals("Clans do servidor") || title.equals("Ranking de KDR")) {
            e.setCancelled(true);
            if (e.getRawSlot() == 49) new ClanGui(plugin).openMain(p);
            return;
        }

        if (title.startsWith("Clan de ")) {
            e.setCancelled(true);
            Clan c = plugin.clans().byPlayer(p.getUniqueId());
            if (c == null) { p.closeInventory(); return; }
            switch (e.getRawSlot()) {
                case 10 -> { p.closeInventory(); p.sendMessage(plugin.msg("prefix") + ChatColor.WHITE + "Informações:"); p.sendMessage(ChatColor.GRAY + "Nome: " + ChatColor.WHITE + c.name()); p.sendMessage(ChatColor.GRAY + "Tag: " + ChatColor.GOLD + c.tag()); p.sendMessage(ChatColor.GRAY + "Membros: " + ChatColor.WHITE + c.members().size()); p.sendMessage(ChatColor.GRAY + "KDR médio: " + ChatColor.YELLOW + String.format(java.util.Locale.US, "%.2f", plugin.clans().clanKdr(c))); }
                case 14 -> { p.closeInventory(); p.performCommand("clan bau"); }
                case 16 -> { p.closeInventory(); p.performCommand("clan home"); }
                case 22 -> { p.closeInventory(); p.sendMessage(ChatColor.YELLOW + "Use /clan tag <TAG> para alterar a tag."); }
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
