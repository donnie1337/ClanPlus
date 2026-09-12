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

        // Painel principal: todos os ícones ficam alinhados na mesma linha.
        inv.setItem(10, item(clan == null ? Material.PAPER : Material.CHEST,
                clan == null ? "&aCriar Clan" : "&bMeu Clan",
                clan == null ? "&7Crie seu próprio clan." : "&7Acesse o menu da sua clan.",
                clan == null ? "&7Defina nome e TAG pelo chat." : "&7Clan: &f" + clan.name(),
                clan == null ? "" : "&7Seu cargo: &f" + role,
                "",
                clan == null ? "&aClique para começar" : "&bClique para abrir"));
        inv.setItem(11, item(Material.WRITABLE_BOOK, "&eConvites recebidos",
                "&7Convites enviados para você.", "&7Pendentes: &f" + pending, "", "&eClique para visualizar"));
        inv.setItem(12, item(Material.NETHER_STAR, "&6Ranking",
                "&7Veja a classificação das clans.", "&7Critérios: nível, KDR e XP.", "", "&6Clique para abrir"));
        inv.setItem(13, item(Material.NAME_TAG, "&fClans do servidor",
                "&7Veja todas as clans existentes.", "&7Nome, TAG, membros e KDR.", "", "&fClique para explorar"));
        inv.setItem(14, item(Material.ANVIL, "&eUpgrades do Clan",
                "&7Evolua os benefícios da sua clan.", clan == null ? "&cVocê precisa estar em um clan." : "&7XP disponível: &f" + clan.xp(), "", "&eClique para abrir"));
        p.openInventory(inv);
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
            inv.setItem(13, item(Material.EXPERIENCE_BOTTLE, "&eXP do Clan",
                    "&7Disponível: &f" + clan.xp(), "&7Nível atual: &e" + clan.level(),
                    "&7Próximo nível: &f" + (clan.xp() + clan.xpForNextLevel() - clan.xpIntoLevel())));
            inv.setItem(15, item(Material.EXPERIENCE_BOTTLE, "&dBônus de XP",
                    "&7Nível: &f" + clan.upgrade("xp_boost"),
                    "&7Bônus atual: &e+" + (int)((clan.xpMultiplier() - 1.0D) * 100) + "%",
                    "&7Próximo custo: &e" + plugin.clans().upgradeCost(clan, "xp_boost") + " XP",
                    "", manager ? "&aClique para comprar" : "&cSomente líderes e moderadores"));
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
                case 10 -> {
                    Clan clan = plugin.clans().byPlayer(p.getUniqueId());
                    p.closeInventory();
                    if (clan == null) p.performCommand("clan criar");
                    else new ClanCommand(plugin).openMenu(p, clan);
                }
                case 11 -> { p.closeInventory(); new ClanGui(plugin).openInvites(p); }
                case 12 -> openRanking(p);
                case 13 -> { p.closeInventory(); new ClanGui(plugin).openAll(p); }
                case 14 -> openUpgrades(p);
                default -> { }
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
        return title.equals(clean(DASHBOARD)) || title.equals(clean(RANKING)) || title.equals(clean(UPGRADES));
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
