package com.donnie1337.clanplus;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class ClanAdminCommand implements CommandExecutor, TabCompleter {
    private final ClanPlus plugin;
    public ClanAdminCommand(ClanPlus plugin) { this.plugin = plugin; }
    @Override public boolean onCommand(CommandSender s, Command c, String label, String[] a) {
        if (!s.hasPermission("clanplus.admin")) { s.sendMessage(ChatColor.RED + "Você não tem permissão para executar este comando."); return true; }
        if (a.length == 0) {
            s.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6&lᴄʟᴀɴ &8• &fPainel administrativo"));
            s.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e/clanadmin listar &8• &7Lista todas as clans"));
            s.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e/clanadmin info <clan> &8• &7Mostra informações"));
            s.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e/clanadmin excluir <clan> &8• &7Exclui uma clan"));
            s.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e/clanadmin reload &8• &7Recarrega configurações e dados"));
            return true;
        }
        switch (a[0].toLowerCase(Locale.ROOT)) {
            case "listar" -> plugin.clans().all().forEach(x -> s.sendMessage(ChatColor.GRAY + "• " + ChatColor.WHITE + x.name() + ChatColor.GRAY + " [" + ChatColor.GOLD + x.tag() + ChatColor.GRAY + "] " + ChatColor.DARK_GRAY + x.members().size() + " membros"));
            case "info" -> { if(a.length<2){s.sendMessage(ChatColor.YELLOW+"Uso: /clanadmin info <clan>");return true;} Clan x=plugin.clans().byName(a[1]); if(x==null){s.sendMessage(ChatColor.RED+"Clan não encontrada.");return true;} s.sendMessage(ChatColor.GOLD+x.name()+" ["+x.tag()+"] "+ChatColor.GRAY+"líder="+Bukkit.getOfflinePlayer(x.owner()).getName()+" membros="+x.members().size()); }
            case "excluir" -> { if(a.length<2){s.sendMessage(ChatColor.YELLOW+"Uso: /clanadmin excluir <clan>");return true;} Clan x=plugin.clans().byName(a[1]); if(x==null){s.sendMessage(ChatColor.RED+"Clan não encontrada.");return true;} plugin.clans().delete(x); s.sendMessage(ChatColor.GREEN+"Clan excluída: "+x.name()); }
            case "reload" -> { plugin.reloadConfig(); plugin.loadMessages(); plugin.clans().load(); s.sendMessage(ChatColor.GREEN+"ClanPlus recarregado."); }
            default -> s.sendMessage(ChatColor.YELLOW+"Use /clanadmin para ver o painel.");
        }
        return true;
    }
    @Override public List<String> onTabComplete(CommandSender s, Command c, String a, String[] args) { if(args.length==1)return Arrays.asList("listar","info","excluir","reload").stream().filter(x->x.startsWith(args[0].toLowerCase())).toList(); if(args.length==2&&List.of("info","excluir").contains(args[0].toLowerCase()))return plugin.clans().all().stream().map(Clan::name).filter(x->x.toLowerCase().startsWith(args[1].toLowerCase())).toList(); return List.of(); }
}
