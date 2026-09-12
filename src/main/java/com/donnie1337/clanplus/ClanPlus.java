package com.donnie1337.clanplus;

import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;

public final class ClanPlus extends JavaPlugin {
    private ClanManager clanManager;
    private Map<String, String> messages;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("messages.yml", false);
        loadMessages();
        clanManager = new ClanManager(this);
        clanManager.load();

        ClanCommand clanCommand = new ClanCommand(this);
        PluginCommand clan = getCommand("clan");
        if (clan != null) { clan.setExecutor(clanCommand); clan.setTabCompleter(clanCommand); }
        PluginCommand clans = getCommand("clans");
        if (clans != null) clans.setExecutor(new ClansCommand(this));
        PluginCommand admin = getCommand("clanadmin");
        if (admin != null) { ClanAdminCommand adminCommand = new ClanAdminCommand(this); admin.setExecutor(adminCommand); admin.setTabCompleter(adminCommand); }
        getServer().getPluginManager().registerEvents(new ClanListener(this), this);
        getServer().getPluginManager().registerEvents(new ClanFeatureListener(this), this);
        getLogger().info("ClanPlus habilitado com " + clanManager.all().size() + " clan(s).");
    }

    @Override
    public void onDisable() { if (clanManager != null) clanManager.save(); }

    public void loadMessages() {
        var cfg = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(new java.io.File(getDataFolder(), "messages.yml"));
        messages = new java.util.HashMap<>();
        for (String key : cfg.getKeys(false)) messages.put(key, cfg.getString(key, ""));
    }

    public String msg(String key, String... replacements) {
        String value = messages.getOrDefault(key, key);
        for (int i = 0; i + 1 < replacements.length; i += 2) value = value.replace(replacements[i], replacements[i + 1]);
        return ChatColor.translateAlternateColorCodes('&', messages.getOrDefault("prefix", "") + value);
    }

    public String raw(String key, String... replacements) {
        String value = messages.getOrDefault(key, key);
        for (int i = 0; i + 1 < replacements.length; i += 2) value = value.replace(replacements[i], replacements[i + 1]);
        return ChatColor.translateAlternateColorCodes('&', value);
    }

    public String getPlayerTag(UUID playerId) {
        if (playerId == null || clanManager == null) return "";
        Clan clan = clanManager.byPlayer(playerId);
        return clan == null ? "" : clan.tag();
    }

    public String getPlayerTag(Player player) {
        return player == null ? "" : getPlayerTag(player.getUniqueId());
    }

    public ClanManager clans() { return clanManager; }
}
