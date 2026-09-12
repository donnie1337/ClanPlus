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
    public static final String MEMBERS = "§8Membros da clan";
    public static final String DELETE = "§8Excluir clan";
    private final ClanPlus plugin;

    public ClanGui(ClanPlus plugin) { this.plugin = plugin; }

    private String color(String s) { return ChatColor.translateAlternateColorCodes('&', s); }

    private ItemStack item(Material material, String name, String... lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(color(name).replace("§l", ""));
        if (lore.length > 0) meta.setLore(java.util.Arrays.stream(lore).map(this::color).toList());
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack playerHead(OfflinePlayer player, String name, String... lore) {
        ItemStack stack = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) stack.getItemMeta();
        meta.setOwningPlayer(player);
        meta.setDisplayName(color(name).replace("§l", ""));
        meta.setLore(java.util.Arrays.stream(lore).map(this::color).toList());
        stack.setItemMeta(meta);
        return stack;
    }

    private void fill(Inventory inv, Material material) {
        // Sem vidro decorativo: os slots vazios permanecem vazios.
    }

    private String coins(Player player) {
        String placeholder = plugin.getConfig().getString("coins.placeholder", "");
        if (placeholder == null || placeholder.isBlank() || !Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) return "N/D";
        try {
            Class<?> clazz = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            Method method = clazz.getMethod("setPlaceholders", OfflinePlayer.class, String.class);
            String value = String.valueOf(method.invoke(null, player, placeholder));
            return value.equals(placeholder) ? "N/D" : value;
        } catch (ReflectiveOperationException ex) {
            return "N/D";
        }
    }

    private String kdr(double value) { return String.format(Locale.US, "%.2f", value); }

    public void openMain(Player p) {
        Inventory inv = Bukkit.createInventory(null, 27, MAIN);
        Clan c = plugin.clans().byPlayer(p.getUniqueId());
        double playerKdr = plugin.clans().kdr(p.getUniqueId());
        String clanName = c == null ? "Nenhuma" : c.name();
        String tag = c == null ? "Nenhuma" : c.tag();
        String role = c == null ? "Nenhum" : roleName(c.role(p.getUniqueId()));
        int kills = plugin.clans().kills(p.getUniqueId());
        int deaths = plugin.clans().deaths(p.getUniqueId());

        ItemStack profile = playerHead(p, "&3&l" + p.getName(),
                "&8&m--------------------",
                "&7Seu perfil no sistema de clans",
                "",
                "&3▪ &7Clan: &f" + clanName,
                "&3▪ &7Tag: &f" + tag,
                "&3▪ &7Cargo: &f" + role,
                "&3▪ &7KDR: &e" + kdr(playerKdr),
                "&3▪ &7Abates: &a" + kills,
                "&3▪ &7Mortes: &c" + deaths,
                "&3▪ &7Coins: &6" + coins(p),
                "",
                "&8Seu desempenho é atualizado automaticamente.");
        inv.setItem(10, profile);

        if (c == null) {
            inv.setItem(11, item(Material.PAPER, "&a&lCriar Clan",
                    "&8&m--------------------",
                    "&7Crie sua própria clan e comece",
                    "&7a construir sua história no servidor.",
                    "",
                    "&3▪ &7Defina um nome exclusivo",
                    "&3▪ &7Escolha uma TAG de 3 letras",
                    "&3▪ &7Convide seus amigos",
                    "",
                    "&aClique para começar"));
        } else {
            inv.setItem(11, item(Material.CHEST, "&b&lMeu Clan",
                    "&8&m--------------------",
                    "&7Acesse o painel completo da sua clan.",
                    "",
                    "&3▪ &7Clan: &f" + c.name(),
                    "&3▪ &7TAG: &f" + c.tag(),
                    "&3▪ &7Seu cargo: &f" + role,
                    "&3▪ &7Membros: &f" + c.members().size(),
                    "&3▪ &7Online: &a" + c.onlineCount(),
                    "&3▪ &7KDR médio: &e" + kdr(plugin.clans().clanKdr(c)),
                    "",
                    "&bClique para gerenciar"));
        }

        inv.setItem(13, item(Material.WRITABLE_BOOK, "&e&lConvites recebidos",
                "&8&m--------------------",
                "&7Consulte os convites enviados",
                "&7para você por outras clans.",
                "",
                "&3▪ &7Aceite convites pendentes",
                "&3▪ &7Recuse convites indesejados",
                "&3▪ &7Veja líder, TAG e ID do convite",
                "",
                "&eClique para visualizar"));

        inv.setItem(14, item(Material.NETHER_STAR, "&6&lClans mais Top",
                "&8&m--------------------",
                "&7Ranking das clans com melhor",
                "&7desempenho médio de KDR.",
                "",
                "&3▪ &7Posição no ranking",
                "&3▪ &7KDR médio da clan",
                "&3▪ &7Quantidade de membros",
                "",
                "&6Clique para ver o ranking"));

        inv.setItem(16, item(Material.NAME_TAG, "&f&lClans do servidor",
                "&8&m--------------------",
                "&7Veja as clans existentes no servidor",
                "&7e conheça seus principais dados.",
                "",
                "&3▪ &7Nome e TAG",
                "&3▪ &7Líder da clan",
                "&3▪ &7Membros e jogadores online",
                "&3▪ &7KDR médio",
                "",
                "&fClique para explorar"));

        p.openInventory(inv);
    }

    public void openInvites(Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, INVITES);
        plugin.clans().cleanupExpiredInvites();
        int slot = 0;
        for (Clan c : plugin.clans().all()) for (Clan.Invite invite : c.invites().values()) {
            if (!invite.player().equals(p.getUniqueId()) || slot >= 45) continue;
            inv.setItem(slot++, item(Material.PAPER, "&e&lConvite de &f" + c.name(),
                    "&8&m--------------------",
                    "&3▪ &7TAG: &f" + c.tag(),
                    "&3▪ &7Líder: &f" + name(c.owner()),
                    "&3▪ &7Membros: &f" + c.members().size(),
                    "&3▪ &7KDR médio: &e" + kdr(plugin.clans().clanKdr(c)),
                    "&3▪ &7ID: &e" + invite.id(),
                    "",
                    "&aClique para aceitar",
                    "&cShift + clique para recusar"));
        }
        if (slot == 0) inv.setItem(22, item(Material.BARRIER, "&c&lNenhum convite", "&7Você não possui convites pendentes.", "", "&8Quando alguém convidar você,", "&8o convite aparecerá aqui."));
        inv.setItem(49, item(Material.ARROW, "&7&lVoltar", "&8Retorna ao painel principal."));
        p.openInventory(inv);
    }

    public void openTop(Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, TOP);
        List<Clan> clans = new ArrayList<>(plugin.clans().all());
        clans.sort(Comparator.comparingDouble((Clan c) -> plugin.clans().clanKdr(c)).reversed());
        for (int i = 0; i < Math.min(45, clans.size()); i++) {
            Clan c = clans.get(i);
            Material material = i == 0 ? Material.GOLD_BLOCK : i == 1 ? Material.IRON_BLOCK : i == 2 ? Material.COPPER_BLOCK : Material.NAME_TAG;
            inv.setItem(i, item(material, (i == 0 ? "&6&l#" : i == 1 ? "&f&l#" : i == 2 ? "&c&l#" : "&7#") + (i + 1) + " &f" + c.name(),
                    "&8&m--------------------",
                    "&3▪ &7TAG: &f" + c.tag(),
                    "&3▪ &7Líder: &f" + name(c.owner()),
                    "&3▪ &7Membros: &f" + c.members().size(),
                    "&3▪ &7Online: &a" + c.onlineCount(),
                    "&3▪ &7KDR médio: &e" + kdr(plugin.clans().clanKdr(c)),
                    "",
                    i == 0 ? "&6★ Clan líder do ranking" : "&7Posição &f#" + (i + 1)));
        }
        inv.setItem(49, item(Material.ARROW, "&7&lVoltar", "&8Retorna ao painel principal."));
        p.openInventory(inv);
    }

    public void openAll(Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, ALL);
        int slot = 0;
        for (Clan c : plugin.clans().all()) {
            if (slot >= 45) break;
            inv.setItem(slot++, item(Material.NAME_TAG, "&f&l" + c.name(),
                    "&8&m--------------------",
                    "&3▪ &7TAG: &f" + c.tag(),
                    "&3▪ &7Líder: &f" + name(c.owner()),
                    "&3▪ &7Membros: &f" + c.members().size(),
                    "&3▪ &7Online: &a" + c.onlineCount(),
                    "&3▪ &7KDR médio: &e" + kdr(plugin.clans().clanKdr(c)),
                    "",
                    "&8Clique para ver os detalhes da clan."));
        }
        if (slot == 0) inv.setItem(22, item(Material.BARRIER, "&c&lNenhum clan", "&7Ainda não existem clans no servidor."));
        inv.setItem(49, item(Material.ARROW, "&7&lVoltar", "&8Retorna ao painel principal."));
        p.openInventory(inv);
    }

    public void openKdr(Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, "§8Ranking de KDR");
        List<UUID> players = new ArrayList<>(plugin.clans().statsPlayers());
        players.sort(Comparator.comparingDouble((UUID u) -> plugin.clans().kdr(u)).reversed());
        for (int i = 0; i < Math.min(45, players.size()); i++) {
            UUID uuid = players.get(i);
            OfflinePlayer target = Bukkit.getOfflinePlayer(uuid);
            Clan c = plugin.clans().byPlayer(uuid);
            inv.setItem(i, playerHead(target, "&e&l#" + (i + 1) + " &f" + String.valueOf(target.getName()),
                    "&8&m--------------------",
                    "&3▪ &7Clan: &f" + (c == null ? "Nenhuma" : c.name()),
                    "&3▪ &7TAG: &f" + (c == null ? "Nenhuma" : c.tag()),
                    "&3▪ &7KDR: &e" + kdr(plugin.clans().kdr(uuid)),
                    "&3▪ &7Abates: &a" + plugin.clans().kills(uuid),
                    "&3▪ &7Mortes: &c" + plugin.clans().deaths(uuid)));
        }
        inv.setItem(49, item(Material.ARROW, "&7&lVoltar", "&8Retorna ao painel principal."));
        p.openInventory(inv);
    }

    public void openMembers(Player p, Clan c) {
        Inventory inv = Bukkit.createInventory(null, 54, MEMBERS);
        List<UUID> members = new ArrayList<>(c.members().keySet());
        members.sort(Comparator.comparing((UUID u) -> u.equals(c.owner()) ? 0 : 1).thenComparing(u -> String.valueOf(Bukkit.getOfflinePlayer(u).getName()), String.CASE_INSENSITIVE_ORDER));
        for (int i = 0; i < Math.min(45, members.size()); i++) {
            UUID uuid = members.get(i);
            OfflinePlayer target = Bukkit.getOfflinePlayer(uuid);
            String role = roleName(c.role(uuid));
            String action = uuid.equals(c.owner()) ? "&8Líder não pode ser expulso" : "&cClique para expulsar";
            inv.setItem(i, playerHead(target, "&f" + String.valueOf(target.getName()),
                    "&8&m--------------------",
                    "&3▪ &7Cargo: &f" + role,
                    "&3▪ &7KDR: &e" + kdr(plugin.clans().kdr(uuid)),
                    "&3▪ &7Abates: &a" + plugin.clans().kills(uuid),
                    "&3▪ &7Mortes: &c" + plugin.clans().deaths(uuid),
                    "",
                    action));
        }
        inv.setItem(49, item(Material.ARROW, "&7&lVoltar", "&8Retorna ao painel da clan."));
        p.openInventory(inv);
    }

    public void openDelete(Player p, Clan c) {
        Inventory inv = Bukkit.createInventory(null, 27, DELETE);
        inv.setItem(11, item(Material.LIME_WOOL, "&a&lConfirmar exclusão", "&7Excluir a clan definitivamente.", "&cEsta ação não pode ser desfeita."));
        inv.setItem(15, item(Material.RED_WOOL, "&c&lCancelar", "&7Manter a clan."));
        p.openInventory(inv);
    }

    public void openClan(Player p, Clan c) {
        Inventory inv = Bukkit.createInventory(null, 27, "§8Clan de " + c.name());
        inv.setItem(10, item(Material.PLAYER_HEAD, "&f&lInformações", "&7Nome: &f" + c.name(), "&7TAG: &f" + c.tag(), "&7Líder: &f" + name(c.owner()), "&7Membros: &f" + c.members().size(), "&7Online: &a" + c.onlineCount(), "&7KDR médio: &e" + kdr(plugin.clans().clanKdr(c))));
        inv.setItem(12, item(Material.PLAYER_HEAD, "&b&lMembros", "&7Veja os membros da clan."));
        inv.setItem(14, item(Material.CHEST, "&6&lBaú da clan", "&7Acesse o baú compartilhado."));
        inv.setItem(16, item(Material.RED_BED, "&a&lHome", "&7Teleporte para a home da clan."));
        inv.setItem(18, item(Material.BARRIER, "&c&lSair", "&7Sair da clan."));
        inv.setItem(22, item(Material.ARROW, "&7&lVoltar", "&8Retorna ao painel principal."));
        p.openInventory(inv);
    }

    public void openConfig(Player p, Clan c) {
        Inventory inv = Bukkit.createInventory(null, 27, "§8Configuração da clan");
        inv.setItem(11, item(Material.NAME_TAG, "&e&lAlterar TAG", "&7Atualize a TAG da clan."));
        inv.setItem(13, item(Material.PAPER, "&f&lAlterar nome", "&7Atualize o nome da clan."));
        inv.setItem(15, item(Material.BARRIER, "&c&lExcluir clan", "&7Excluir definitivamente a clan."));
        inv.setItem(22, item(Material.ARROW, "&7&lVoltar", "&8Retorna ao painel da clan."));
        p.openInventory(inv);
    }

    private String roleName(ClanRole role) {
        return switch (role) {
            case LEADER -> "Líder";
            case MODERATOR -> "Moderador";
            case MEMBER -> "Membro";
        };
    }

    private String name(UUID uuid) {
        OfflinePlayer p = Bukkit.getOfflinePlayer(uuid);
        return p.getName() == null ? uuid.toString().substring(0, 8) : p.getName();
    }
}
