package com.donnie1337.clanplus;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class ClanFeatureListener implements Listener {
    private static final String DASHBOARD = "§8ᴄʟᴀɴ • ᴘᴀɪɴᴇʟ";
    private static final String PROFILE = "§8ᴘᴇʀғɪʟ • ";
    private static final String UPGRADES = "§8ᴄʟᴀɴ • ᴜᴘɢʀᴀᴅᴇs";
    private static final String RANKING = "§8ᴄʟᴀɴ • ʀᴀɴᴋɪɴɢ";
    private static final String NOTIFICATIONS = "§8ᴄʟᴀɴ • ɴᴏᴛɪғɪᴄᴀᴄ̧ᴏ̃ᴇs";
    private final ClanPlus plugin;

    public ClanFeatureListener(ClanPlus plugin) { this.plugin = plugin; }

    private String color(String text) { return ChatColor.translateAlternateColorCodes('&', text); }
    private String clean(String text) { return ChatColor.stripColor(text); }
    private String kdr(double value) { return String.format(Locale.US, "%.2f", value); }

    private ItemStack item(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(color(name).replace("§l", ""));
        if (lore.length > 0) meta.setLore(java.util.Arrays.stream(lore).map(this::color).toList());
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack head(OfflinePlayer player, String name, String... lore) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwningPlayer(player);
        meta.setDisplayName(color(name).replace("§l", ""));
        meta.setLore(java.util.Arrays.stream(lore).map(this::color).toList());
        item.setItemMeta(meta);
        return item;
    }

    private boolean authenticated(Player p) { return LoginPlusHook.requireAuthentication(plugin, p); }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player p = event.getPlayer();
        String raw = event.getMessage().trim();
        if (!raw.regionMatches(true, 0, "/clan", 0, 5)) return;
        if (raw.length() > 5 && !Character.isWhitespace(raw.charAt(5))) return;
        String rest = raw.length() <= 5 ? "" : raw.substring(5).trim();
        if (!authenticated(p)) { event.setCancelled(true); return; }

        if (rest.isEmpty() || rest.equalsIgnoreCase("menu")) {
            event.setCancelled(true);
            openDashboard(p);
            return;
        }
        String[] parts = rest.split("\\s+");
        switch (parts[0].toLowerCase(Locale.ROOT)) {
            case "ranking" -> { event.setCancelled(true); openRanking(p); }
            case "perfil" -> { event.setCancelled(true); openProfile(p, parts.length > 1 ? parts[1] : p.getName()); }
            case "upgrades", "melhorias" -> { event.setCancelled(true); openUpgrades(p); }
            case "notificacoes", "notificações" -> { event.setCancelled(true); openNotifications(p); }
            default -> { }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!p.isOnline() || !LoginPlusHook.isAuthenticated(p)) return;
            int pending = pendingInvites(p.getUniqueId());
            if (pending > 0) p.sendMessage(plugin.msg("notification-invites", "%count%", String.valueOf(pending)));
            Clan clan = plugin.clans().byPlayer(p.getUniqueId());
            if (clan != null) {
                int rank = plugin.clans().rank(clan);
                p.sendMessage(plugin.msg("notification-clan", "%name%", clan.name(), "%level%", String.valueOf(clan.level()), "%rank%", String.valueOf(rank)));
            }
        }, 20L);
    }

    private int pendingInvites(UUID player) {
        int count = 0;
        for (Clan clan : plugin.clans().all()) for (Clan.Invite invite : clan.invites().values()) if (invite.player().equals(player)) count++;
        return count;
    }

    public void openDashboard(Player p) {
        Inventory inv = Bukkit.createInventory(null, 36, DASHBOARD);
        Clan clan = plugin.clans().byPlayer(p.getUniqueId());
        int kills = plugin.clans().kills(p.getUniqueId());
        int deaths = plugin.clans().deaths(p.getUniqueId());
        String clanName = clan == null ? "Nenhuma" : clan.name();
        String role = clan == null ? "Nenhum" : roleName(clan.role(p.getUniqueId()));

        inv.setItem(10, head(p, "&3" + p.getName(),
                "&7Seu perfil individual", "", "&7Clan: &f" + clanName,
                "&7Cargo: &f" + role, "&7KDR: &e" + kdr(plugin.clans().kdr(p.getUniqueId())),
                "&7Abates: &a" + kills, "&7Mortes: &c" + deaths, "", "&3Clique para abrir o perfil"));
        inv.setItem(12, item(clan == null ? Material.PAPER : Material.CHEST, clan == null ? "&aCriar Clan" : "&bMeu Clan",
                clan == null ? "&7Você ainda não pertence a um clan." : "&7Nível: &e" + clan.level(),
                clan == null ? "&7Use a opção existente para criar seu clan." : "&7XP: &f" + clan.xp(),
                clan == null ? "" : "&7KDR médio: &e" + kdr(plugin.clans().clanKdr(clan)),
                "", clan == null ? "&aClique para criar" : "&bClique para abrir o menu"));
        inv.setItem(14, item(Material.NETHER_STAR, "&6Ranking",
                "&7Veja a classificação dos clans.", "&7Critérios: nível, KDR e XP.", "", "&6Clique para abrir"));
        inv.setItem(16, item(Material.ANVIL, "&eUpgrades do Clan",
                "&7Evolua os benefícios do seu clan.", clan == null ? "&cVocê precisa estar em um clan." : "&7XP disponível: &f" + clan.xp(), "", "&eClique para abrir"));
        inv.setItem(19, item(Material.WRITABLE_BOOK, "&fPerfil individual",
                "&7KDR, kills, mortes, cargo e progresso.", "", "&fClique para abrir"));
        inv.setItem(21, item(Material.BELL, "&dNotificações",
                "&7Convites e atualizações do seu clan.", "&7Pendentes: &f" + pendingInvites(p.getUniqueId()), "", "&dClique para abrir"));
        if (clan != null) {
            int rank = plugin.clans().rank(clan);
            inv.setItem(23, item(Material.GOLD_INGOT, "&6Status do Clan",
                    "&7Nome: &f" + clan.name(), "&7TAG: &f" + clan.tag(),
                    "&7Nível: &e" + clan.level(), "&7XP: &f" + clan.xp() + " / " + (clan.xp() + clan.xpForNextLevel() - clan.xpIntoLevel()),
                    "&7Ranking: &6#" + rank, "&7Membros: &f" + clan.members().size() + "/" + clan.memberLimit(plugin.getConfig().getInt("clan.max-members", 30)),
                    "&7Online: &a" + clan.onlineCount()));
        }
        inv.setItem(31, item(Material.ARROW, "&7Voltar", "&8Fechar painel"));
        p.openInventory(inv);
    }

    public void openProfile(Player viewer, String targetName) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        Clan clan = plugin.clans().byPlayer(target.getUniqueId());
        Inventory inv = Bukkit.createInventory(null, 27, PROFILE + target.getName());
        String clanName = clan == null ? "Nenhuma" : clan.name();
        String role = clan == null ? "Nenhum" : roleName(clan.role(target.getUniqueId()));
        inv.setItem(10, head(target, "&3" + target.getName(),
                "&7Clan: &f" + clanName, "&7Cargo: &f" + role,
                "&7KDR: &e" + kdr(plugin.clans().kdr(target.getUniqueId())),
                "&7Abates: &a" + plugin.clans().kills(target.getUniqueId()),
                "&7Mortes: &c" + plugin.clans().deaths(target.getUniqueId())));
        if (clan != null) {
            int rank = plugin.clans().rank(clan);
            inv.setItem(13, item(Material.NETHER_STAR, "&6Clan",
                    "&7Nome: &f" + clan.name(), "&7TAG: &f" + clan.tag(),
                    "&7Nível: &e" + clan.level(), "&7Ranking: &6#" + rank,
                    "&7KDR médio: &e" + kdr(plugin.clans().clanKdr(clan))));
        }
        inv.setItem(16, item(Material.BOOK, "&fProgresso",
                "&7Participação no clan baseada em", "&7KDR, abates e presença.", "", "&7Membros online: &a" + (clan == null ? 0 : clan.onlineCount())));
        inv.setItem(22, item(Material.ARROW, "&7Voltar", "&8Voltar ao painel"));
        viewer.openInventory(inv);
    }

    public void openRanking(Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, RANKING);
        List<Clan> clans = new ArrayList<>(plugin.clans().ranking());
        for (int i = 0; i < Math.min(45, clans.size()); i++) {
            Clan clan = clans.get(i);
            Material material = i == 0 ? Material.GOLD_BLOCK : i == 1 ? Material.IRON_BLOCK : i == 2 ? Material.COPPER_BLOCK : Material.NAME_TAG;
            inv.setItem(i, item(material, "&f#" + (i + 1) + " &b" + clan.name(),
                    "&7TAG: &f" + clan.tag(), "&7Nível: &e" + clan.level(), "&7XP: &f" + clan.xp(),
                    "&7KDR médio: &e" + kdr(plugin.clans().clanKdr(clan)), "&7Membros: &f" + clan.members().size(),
                    "&7Online: &a" + clan.onlineCount()));
        }
        inv.setItem(49, item(Material.ARROW, "&7Voltar", "&8Voltar ao painel"));
        p.openInventory(inv);
    }

    public void openUpgrades(Player p) {
        Inventory inv = Bukkit.createInventory(null, 27, UPGRADES);
        Clan clan = plugin.clans().byPlayer(p.getUniqueId());
        if (clan == null) {
            inv.setItem(13, item(Material.BARRIER, "&cNenhum Clan", "&7Você precisa estar em um clan."));
        } else {
            boolean manager = clan.role(p.getUniqueId()) != null && clan.role(p.getUniqueId()).canManage();
            inv.setItem(11, item(Material.CHEST, "&bLimite de membros",
                    "&7Nível: &f" + clan.upgrade("member_limit"),
                    "&7Capacidade: &f" + clan.memberLimit(plugin.getConfig().getInt("clan.max-members", 30)),
                    "&7Próximo custo: &e" + plugin.clans().upgradeCost(clan, "member_limit") + " XP",
                    "", manager ? "&aClique para comprar" : "&cSomente líderes e moderadores"));
            inv.setItem(15, item(Material.EXPERIENCE_BOTTLE, "&dBônus de XP",
                    "&7Nível: &f" + clan.upgrade("xp_boost"),
                    "&7Bônus atual: &e+" + (int)((clan.xpMultiplier() - 1.0D) * 100) + "%",
                    "&7Próximo custo: &e" + plugin.clans().upgradeCost(clan, "xp_boost") + " XP",
                    "", manager ? "&aClique para comprar" : "&cSomente líderes e moderadores"));
            inv.setItem(13, item(Material.EXPERIENCE_BOTTLE, "&eXP do Clan", "&7Disponível: &f" + clan.xp(), "&7Nível atual: &e" + clan.level(), "&7Próximo nível: &f" + (clan.xp() + clan.xpForNextLevel() - clan.xpIntoLevel())));
        }
        inv.setItem(22, item(Material.ARROW, "&7Voltar", "&8Voltar ao painel"));
        p.openInventory(inv);
    }

    public void openNotifications(Player p) {
        Inventory inv = Bukkit.createInventory(null, 27, NOTIFICATIONS);
        int pending = pendingInvites(p.getUniqueId());
        Clan clan = plugin.clans().byPlayer(p.getUniqueId());
        inv.setItem(10, item(Material.WRITABLE_BOOK, "&eConvites pendentes", "&7Você possui: &f" + pending, "", pending > 0 ? "&aUse o botão de convites no painel." : "&7Nenhum convite pendente."));
        if (clan != null) {
            inv.setItem(13, item(Material.NETHER_STAR, "&6Atualização do Clan",
                    "&7Clan: &f" + clan.name(), "&7Nível: &e" + clan.level(), "&7Ranking: &6#" + plugin.clans().rank(clan),
                    "&7XP: &f" + clan.xp()));
        }
        inv.setItem(16, item(Material.BELL, "&dStatus", "&7As notificações importantes são", "&7mostradas automaticamente ao entrar."));
        inv.setItem(22, item(Material.ARROW, "&7Voltar", "&8Voltar ao painel"));
        p.openInventory(inv);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player p)) return;
        String title = clean(event.getView().getTitle());
        if (!isFeatureGui(title)) return;
        event.setCancelled(true);
        if (!authenticated(p)) { p.closeInventory(); return; }

        if (title.equals(clean(DASHBOARD))) {
            switch (event.getRawSlot()) {
                case 10, 19 -> openProfile(p, p.getName());
                case 12 -> { Clan clan = plugin.clans().byPlayer(p.getUniqueId()); p.closeInventory(); if (clan == null) p.performCommand("clan criar"); else new ClanCommand(plugin).openMenu(p, clan); }
                case 14 -> openRanking(p);
                case 16 -> openUpgrades(p);
                case 21 -> openNotifications(p);
                case 31 -> p.closeInventory();
                default -> { }
            }
            return;
        }
        if (title.equals(clean(RANKING))) { if (event.getRawSlot() == 49) openDashboard(p); return; }
        if (title.equals(clean(PROFILE + p.getName())) || title.startsWith(clean(PROFILE))) { if (event.getRawSlot() == 22) openDashboard(p); return; }
        if (title.equals(clean(NOTIFICATIONS))) { if (event.getRawSlot() == 22) openDashboard(p); return; }
        if (title.equals(clean(UPGRADES))) {
            if (event.getRawSlot() == 22) { openDashboard(p); return; }
            Clan clan = plugin.clans().byPlayer(p.getUniqueId());
            if (clan == null || clan.role(p.getUniqueId()) == null || !clan.role(p.getUniqueId()).canManage()) return;
            String key = event.getRawSlot() == 11 ? "member_limit" : event.getRawSlot() == 15 ? "xp_boost" : null;
            if (key == null) return;
            int cost = plugin.clans().upgradeCost(clan, key);
            if (plugin.clans().buyUpgrade(clan, key)) {
                p.sendMessage(plugin.msg("upgrade-bought", "%level%", String.valueOf(clan.upgrade(key)), "%cost%", String.valueOf(cost)));
                p.sendMessage(plugin.msg("notification-upgrade", "%name%", clan.name(), "%upgrade%", key));
            } else p.sendMessage(plugin.msg("upgrade-no-xp", "%cost%", String.valueOf(cost)));
            openUpgrades(p);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (isFeatureGui(clean(event.getView().getTitle()))) event.setCancelled(true);
    }

    private boolean isFeatureGui(String title) {
        return title.equals(clean(DASHBOARD)) || title.equals(clean(RANKING)) || title.equals(clean(UPGRADES)) || title.equals(clean(NOTIFICATIONS)) || title.startsWith(clean(PROFILE));
    }

    private String roleName(ClanRole role) {
        if (role == null) return "Nenhum";
        return switch (role) {
            case LEADER -> "Líder";
            case MODERATOR -> "Moderador";
            case MEMBER -> "Membro";
        };
    }
}
