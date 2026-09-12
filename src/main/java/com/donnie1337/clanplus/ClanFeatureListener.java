package com.donnie1337.clanplus;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
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
    private static final String UPGRADES = "§8ᴄʟᴀɴ • ᴜᴘɢʀᴀᴅᴇs";
    private static final String RANKING = "§8ᴄʟᴀɴ • ʀᴀɴᴋɪɴɢ";
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

    private ItemStack profileHead(Player p, Clan clan) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwningPlayer(p);
        meta.setDisplayName(color("&b&l" + p.getName()));
        List<String> lore = new ArrayList<>();
        lore.add(color("&8👤 &7Seu perfil no sistema de clans"));
        lore.add("");
        lore.add(color("&b🏠 &7Clan: &f" + (clan == null ? "Nenhum" : clan.name())));
        lore.add(color("&e⭐ &7Cargo: &f" + (clan == null ? "Nenhum" : roleName(clan.role(p.getUniqueId())))));
        lore.add(color("&6⚔ &7KDR: &e" + kdr(playerKdr(p))));
        lore.add("");
        lore.add(color("&8Este item é apenas informativo."));
        return applyLore(item, meta, lore);
    }

    private ItemStack applyLore(ItemStack item, ItemMeta meta, List<String> lore) {
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private double playerKdr(Player p) {
        Clan clan = plugin.clans().byPlayer(p.getUniqueId());
        if (clan == null) return 0.0D;
        return plugin.clans().clanKdr(clan);
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
            case "upgrades", "melhorias" -> { event.setCancelled(true); openUpgrades(p); }
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
        Inventory inv = Bukkit.createInventory(null, 27, DASHBOARD);
        Clan clan = plugin.clans().byPlayer(p.getUniqueId());
        String role = clan == null ? "Nenhum" : roleName(clan.role(p.getUniqueId()));
        int pending = pendingInvites(p.getUniqueId());

        inv.setItem(11, profileHead(p, clan));
        inv.setItem(12, item(clan == null ? Material.PAPER : Material.CHEST,
                clan == null ? "&a&lCriar Clan" : "&b&lMeu Clan",
                clan == null ? "&8📜 &7Crie sua própria clan e comece sua história." : "&8🏠 &7Gerencie sua clan e seus membros.",
                clan == null ? "&3✦ &7Escolha um nome e uma TAG exclusiva." : "&3✦ &7Clan: &f" + clan.name(),
                clan == null ? "&3✦ &7Convide jogadores e conquiste posições." : "&3✦ &7Seu cargo: &f" + role,
                "",
                clan == null ? "&a➜ Clique para começar" : "&b➜ Clique para abrir"));
        inv.setItem(14, item(Material.WRITABLE_BOOK, "&e&lConvites",
                "&8✉ &7Convites enviados para você por outras clans.",
                "&e✦ &7Convites pendentes: &f" + pending,
                "&7Aceite ou recuse convites recebidos.",
                "",
                "&e➜ Clique para visualizar"));
        inv.setItem(16, item(Material.NETHER_STAR, "&6&lRanking",
                "&8🏆 &7Veja as clans que mais se destacam.",
                "&6✦ &7Critérios: &fnível, KDR e XP",
                "&7Compare a evolução das clans do servidor.",
                "",
                "&6➜ Clique para abrir o ranking"));
        inv.setItem(17, item(Material.NAME_TAG, "&f&lClans do servidor",
                "&8🌎 &7Explore todas as clans existentes.",
                "&f✦ &7Consulte nome, TAG, líder e membros.",
                "&7Veja também jogadores online e KDR médio.",
                "",
                "&f➜ Clique para explorar"));
        p.openInventory(inv);
    }

    private void decorateClanMenu(Player p, Clan clan) {
        if (p.getOpenInventory() == null) return;
        if (!clean(p.getOpenInventory().getTitle()).startsWith("Clan de ")) return;
        Inventory inv = p.getOpenInventory().getTopInventory();
        inv.setItem(20, item(Material.ANVIL, "&e&lUpgrades do Clan",
                "&8⚙ &7Evolua os benefícios da sua clan.",
                "&e✦ &7XP disponível: &f" + clan.xp(),
                "",
                "&e➜ Clique para abrir"));
    }

    private void openProfile(Player p) {
        Clan clan = plugin.clans().byPlayer(p.getUniqueId());
        Inventory inv = Bukkit.createInventory(null, 27, "§8ᴄʟᴀɴ • ᴘᴇʀғɪʟ");
        inv.setItem(13, profileHead(p, clan));
        if (clan != null) {
            inv.setItem(11, item(Material.NAME_TAG, "&e&lClan", "&8🏠 &7Nome: &f" + clan.name(), "&3✦ &7TAG: &f" + clan.tag(), "&3✦ &7Cargo: &f" + roleName(clan.role(p.getUniqueId()))));
            inv.setItem(15, item(Material.IRON_SWORD, "&c&lDesempenho", "&8⚔ &7KDR da clan: &e" + kdr(plugin.clans().clanKdr(clan)), "&3✦ &7Membros: &f" + clan.members().size()));
        }
        p.openInventory(inv);
    }

    public void openRanking(Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, RANKING);
        List<Clan> clans = new ArrayList<>(plugin.clans().ranking());
        for (int i = 0; i < Math.min(45, clans.size()); i++) {
            Clan clan = clans.get(i);
            Material material = i == 0 ? Material.GOLD_BLOCK : i == 1 ? Material.IRON_BLOCK : i == 2 ? Material.COPPER_BLOCK : Material.NAME_TAG;
            inv.setItem(i, item(material, "&f#" + (i + 1) + " &b&l" + clan.name(),
                    "&8🏆 &7Posição: &f#" + (i + 1),
                    "&3🏷 &7TAG: &f" + clan.tag(), "&e⭐ &7Nível: &f" + clan.level(), "&8✦ &7XP: &f" + clan.xp(),
                    "&6⚔ &7KDR médio: &e" + kdr(plugin.clans().clanKdr(clan)), "&b👥 &7Membros: &f" + clan.members().size(),
                    "&a● &7Online: &f" + clan.onlineCount()));
        }
        p.openInventory(inv);
    }

    public void openUpgrades(Player p) {
        Inventory inv = Bukkit.createInventory(null, 27, UPGRADES);
        Clan clan = plugin.clans().byPlayer(p.getUniqueId());
        if (clan == null) {
            inv.setItem(13, item(Material.BARRIER, "&c&lNenhum Clan", "&8⚠ &7Você precisa estar em um clan."));
        } else {
            boolean manager = clan.role(p.getUniqueId()) != null && clan.role(p.getUniqueId()).canManage();
            inv.setItem(11, item(Material.CHEST, "&b&lLimite de membros",
                    "&8👥 &7Nível do upgrade: &f" + clan.upgrade("member_limit"),
                    "&b✦ &7Capacidade: &f" + clan.memberLimit(plugin.getConfig().getInt("clan.max-members", 30)),
                    "&e💰 &7Próximo custo: &e" + plugin.clans().upgradeCost(clan, "member_limit") + " XP",
                    "", manager ? "&a➜ Clique para comprar" : "&c✘ Somente líderes e moderadores"));
            inv.setItem(13, item(Material.EXPERIENCE_BOTTLE, "&e&lXP do Clan",
                    "&8✨ &7XP disponível: &f" + clan.xp(), "&e⭐ &7Nível atual: &f" + clan.level(),
                    "&3✦ &7XP para o próximo nível: &f" + (clan.xp() + clan.xpForNextLevel() - clan.xpIntoLevel())));
            inv.setItem(15, item(Material.EXPERIENCE_BOTTLE, "&d&lBônus de XP",
                    "&8✨ &7Nível do upgrade: &f" + clan.upgrade("xp_boost"),
                    "&d✦ &7Bônus atual: &e+" + (int)((clan.xpMultiplier() - 1.0D) * 100) + "%",
                    "&e💰 &7Próximo custo: &e" + plugin.clans().upgradeCost(clan, "xp_boost") + " XP",
                    "", manager ? "&a➜ Clique para comprar" : "&c✘ Somente líderes e moderadores"));
        }
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
                case 11 -> { }
                case 12 -> {
                    Clan clan = plugin.clans().byPlayer(p.getUniqueId());
                    p.closeInventory();
                    if (clan == null) p.performCommand("clan criar");
                    else {
                        new ClanCommand(plugin).openMenu(p, clan);
                        decorateClanMenu(p, clan);
                    }
                }
                case 14 -> { p.closeInventory(); new ClanGui(plugin).openInvites(p); }
                case 16 -> openRanking(p);
                case 17 -> { p.closeInventory(); new ClanGui(plugin).openAll(p); }
                default -> { }
            }
            return;
        }
        if (title.startsWith("Clan de ")) {
            if (event.getRawSlot() == 20) {
                Clan clan = plugin.clans().byPlayer(p.getUniqueId());
                if (clan != null) {
                    p.closeInventory();
                    openUpgrades(p);
                }
            }
            return;
        }
        if (title.equals(clean(RANKING))) return;
        if (title.equals(clean(UPGRADES))) {
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
        return title.equals(clean(DASHBOARD)) || title.equals(clean(RANKING)) || title.equals(clean(UPGRADES)) || title.startsWith("Clan de ") || title.equals("ᴄʟᴀɴ • ᴘᴇʀғɪʟ");
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
