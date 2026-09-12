package com.donnie1337.clanplus;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class ClansCommand implements CommandExecutor {
    private final ClanPlus plugin;
    public ClansCommand(ClanPlus plugin) { this.plugin = plugin; }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("Apenas jogadores podem usar este comando."); return true; }
        if (!LoginPlusHook.requireAuthentication(plugin, p)) return true;
        new ClanFeatureListener(plugin).openDashboard(p);
        return true;
    }
}
