package com.donnie1337.clanplus;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class ClanGui {
    public static final String MAIN = "§8ᴄʟᴀɴ";
    public static final String INVITES = "§8Convites recebidos";
    public static final String TOP = "§8Clans mais top";
    public static final String ALL = "§8Clans do servidor";
    private final ClanPlus plugin;

    public ClanGui(ClanPlus plugin) { this.plugin = plugin; }
    private String color(String s) { return ChatColor.translateAlternateColorCodes('&', s); }
    private ItemStack item(Material material, String name, String... lore) { ItemStack stack = new ItemStack(material); ItemMeta meta = stack.getItemMeta(); meta.setDisplayName(color(name)); if (lore.length > 0) meta.setLore(java.util.Arrays.stream(lore).map(this::color).toList()); stack.setItemMeta(meta); return stack; }
    private String coins(Player player) {
        String placeholder = plugin.getConfig().getString("coins.placeholder", "");
        if (placeholder == null || placeholder.isBlank() || !Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) return "N/D";
        try { Class<?> clazz = Class.forName("me.clip.placeholderapi.PlaceholderAPI"); Method method = clazz.getMethod("setPlaceholders", OfflinePlayer.class, String.class); String value = String.valueOf(method.invoke(null, player, placeholder)); return value.equals(placeholder) ? "N/D" : value; }
        catch (ReflectiveOperationException ex) { return "N/D"; }
    }
    public void openMain(Player p) {
        Inventory inv = Bukkit.createInventory(null, 27, MAIN);
        Clan c = plugin.clans().byPlayer(p.getUniqueId());
        int kills = plugin.clans().kills(p.getUniqueId()), deaths = plugin.clans().deaths(p.getUniqueId());
        String clanName = c == null ? "Nenhum" : c.name() + " [" + c.tag() + "]";
        String role = c == null ? "Nenhum" : roleName(c.role(p.getUniqueId()));
        ItemStack profile = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skull = (SkullMeta) profile.getItemMeta(); skull.setOwningPlayer(p); skull.setDisplayName(color("&b&l" + p.getName()));
        skull.setLore(List.of(color("&7Clan: &f" + clanName), color("&7Cargo: &f" + role), color("&7Coins: &6" + coins(p)), "", color("&7KDR: &e" + String.format(Locale.US, "%.2f", plugin.clans().kdr(p.getUniqueId()))), color("&7Kills: &a" + kills), color("&7Mortes: &c" + deaths)));
        profile.setItemMeta(skull); inv.setItem(4, profile);
        inv.setItem(10, item(c == null ? Material.EMERALD : Material.CHEST, c == null ? "&aCriar Clan" : "&bMeu Clan", c == null ? "&7Crie seu próprio clan." : "&7Gerencie seu clan."));
        inv.setItem(12, item(Material.WRITABLE_BOOK, "&eConvites recebidos", "&7Veja e aceite seus convites."));
        inv.setItem(14, item(Material.NETHER_STAR, "&6Clans mais Top", "&7Ranking baseado no KDR médio", "&7dos jogadores de cada clan."));
        inv.setItem(16, item(Material.NAME_TAG, "&fClans do servidor", "&7Veja todos os clans existentes."));
        inv.setItem(22, item(Material.DIAMOND_SWORD, "&cRanking de KDR", "&7Veja os melhores jogadores."));
        inv.setItem(26, item(Material.BARRIER, "&cFechar")); p.openInventory(inv);
    }
    public void openInvites(Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, INVITES); plugin.clans().cleanupExpiredInvites(); int slot = 0;
        for (Clan c : plugin.clans().all()) for (Clan.Invite invite : c.invites().values()) if (invite.player().equals(p.getUniqueId()) && slot < 45) inv.setItem(slot++, item(Material.PAPER, "&eConvite: &f" + c.name(), "&7Tag: &6" + c.tag(), "&7Líder: &f" + name(c.owner()), "&7ID: &e" + invite.id(), "", "&aClique para aceitar", "&cShift + clique para recusar"));
        if (slot == 0) inv.setItem(22, item(Material.BARRIER, "&cNenhum convite", "&7Você não possui convites pendentes.")); inv.setItem(49, item(Material.ARROW, "&7Voltar")); p.openInventory(inv);
    }
    public void openTop(Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, TOP); List<Clan> clans = new ArrayList<>(plugin.clans().all()); clans.sort(Comparator.comparingDouble((Clan c) -> plugin.clans().clanKdr(c)).reversed());
        for (int i = 0; i < Math.min(45, clans.size()); i++) { Clan c = clans.get(i); inv.setItem(i, item(i == 0 ? Material.GOLD_BLOCK : i == 1 ? Material.IRON_BLOCK : i == 2 ? Material.COPPER_BLOCK : Material.NAME_TAG, "&e#" + (i + 1) + " &f" + c.name() + " &8[&6" + c.tag() + "&8]", "&7KDR médio: &e" + String.format(Locale.US, "%.2f", plugin.clans().clanKdr(c)), "&7Membros: &f" + c.members().size())); }
        inv.setItem(49, item(Material.ARROW, "&7Voltar")); p.openInventory(inv);
    }
    public void openAll(Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, ALL); int slot = 0;
        for (Clan c : plugin.clans().all()) { if (slot >= 45) break; inv.setItem(slot++, item(Material.NAME_TAG, "&f" + c.name() + " &8[&6" + c.tag() + "&8]", "&7Líder: &f" + name(c.owner()), "&7Membros: &f" + c.members().size(), "&7Online: &f" + c.onlineCount(), "&7KDR médio: &e" + String.format(Locale.US, "%.2f", plugin.clans().clanKdr(c)))); }
        if (slot == 0) inv.setItem(22, item(Material.BARRIER, "&cNenhum clan", "&7Ainda não existem clans.")); inv.setItem(49, item(Material.ARROW, "&7Voltar")); p.openInventory(inv);
    }
    public void openKdr(Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, "§8Ranking de KDR"); List<UUID> players = new ArrayList<>(plugin.clans().statsPlayers()); players.sort(Comparator.comparingDouble((UUID u) -> plugin.clans().kdr(u)).reversed());
        for (int i = 0; i < Math.min(45, players.size()); i++) { UUID uuid = players.get(i); OfflinePlayer target = Bukkit.getOfflinePlayer(uuid); Clan c = plugin.clans().byPlayer(uuid); inv.setItem(i, item(Material.PLAYER_HEAD, "&e#" + (i + 1) + " &f" + String.valueOf(target.getName()), "&7Clan: &f" + (c == null ? "Nenhum" : c.name() + " [" + c.tag() + "]"), "&7KDR: &e" + String.format(Locale.US, "%.2f", plugin.clans().kdr(uuid)), "&7Kills: &a" + plugin.clans().kills(uuid), "&7Mortes: &c" + plugin.clans().deaths(uuid))); }
        inv.setItem(49, item(Material.ARROW, "&7Voltar")); p.openInventory(inv);
    }
    private String name(UUID uuid) { String name = Bukkit.getOfflinePlayer(uuid).getName(); return name == null ? uuid.toString().substring(0, 8) : name; }
    private String roleName(ClanRole role) { if (role == null) return "Nenhum"; return switch (role) { case LEADER -> "Líder"; case MODERATOR -> "Moderador"; case MEMBER -> "Membro"; }; }
}
