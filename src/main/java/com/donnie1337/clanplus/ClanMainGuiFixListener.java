package com.donnie1337.clanplus;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public final class ClanMainGuiFixListener implements Listener {
    private final ClanPlus plugin;

    public ClanMainGuiFixListener(ClanPlus plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!ChatColor.stripColor(event.getView().getTitle()).equals("Convites recebidos")) return;
        if (event.getRawSlot() != 49) return;

        event.setCancelled(true);
        player.closeInventory();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) new ClanFeatureListener(plugin).openDashboard(player);
        });
    }
}
