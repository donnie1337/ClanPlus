package com.donnie1337.clanplus;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;

public final class ClanDashboardRedirectListener implements Listener {
    private final ClanPlus plugin;

    public ClanDashboardRedirectListener(ClanPlus plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        String title = ChatColor.stripColor(event.getView().getTitle());
        if (!"ᴄʟᴀɴ".equals(title)) return;

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) return;
            if (!"ᴄʟᴀɴ".equals(ChatColor.stripColor(player.getOpenInventory().getTitle()))) return;
            player.closeInventory();
            new ClanFeatureListener(plugin).openDashboard(player);
        });
    }
}
