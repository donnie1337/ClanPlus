package com.donnie1337.clanplus;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ClanListener implements Listener {
    private final ClanPlus plugin;
    private final Map<UUID, CreateStep> createSteps = new HashMap<>();
    private final Map<UUID, String> createNames = new HashMap<>();
    private enum CreateStep { NAME, TAG }

    public ClanListener(ClanPlus plugin) { this.plugin = plugin; }
    private String clean(String s) { return ChatColor.stripColor(s); }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player victim = e.getEntity();
        plugin.clans().recordDeath(victim.getUniqueId());
        Player killer = victim.getKiller();
        if (killer != null && !killer.getUniqueId().equals(victim.getUniqueId())) plugin.clans().recordKill(killer.getUniqueId());
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        String title = clean(e.getView().getTitle());
        if (title.startsWith("Baú da clan:")) return;

        if (title.equals("ᴄʟᴀɴ")) {
            e.setCancelled(true);
            switch (e.getRawSlot()) {
                case 11 -> { if (plugin.clans().byPlayer(p.getUniqueId()) == null) openCreateGui(p); else { p.closeInventory(); p.performCommand("clan menu"); } }
                case 12 -> new ClanGui(plugin).openInvites(p);
                case 14 -> new ClanGui(plugin).openTop(p);
                case 16 -> new ClanGui(plugin).openAll(p);
                case 22 -> new ClanGui(plugin).openKdr(p);
                case 26 -> p.closeInventory();
                default -> { }
            }
            return;
        }

        if (title.equals("Criar clan")) {
            e.setCancelled(true);
            if (e.getRawSlot() == 13) { p.closeInventory(); startCreation(p); }
            else if (e.getRawSlot() == 22) p.closeInventory();
            return;
        }

        if (title.equals("Convites recebidos")) {
            e.setCancelled(true);
            if (e.getRawSlot() == 49) { new ClanGui(plugin).openMain(p); return; }
            if (e.getRawSlot() < 0 || e.getRawSlot() >= e.getInventory().getSize()) return;
            if (e.getCurrentItem() == null || !e.getCurrentItem().hasItemMeta() || e.getCurrentItem().getItemMeta().getLore() == null) return;
            for (String line : e.getCurrentItem().getItemMeta().getLore()) {
                String lore = ChatColor.stripColor(line);
                if (!lore.startsWith("ID: ")) continue;
                try { int id = Integer.parseInt(lore.substring(4).trim()); p.closeInventory(); p.performCommand(e.isShiftClick() ? "clan recusar " + id : "clan aceitar " + id); } catch (NumberFormatException ignored) { }
                return;
            }
            return;
        }

        if (title.equals("Clans mais top") || title.equals("Clans do servidor") || title.equals("Ranking de KDR")) {
            e.setCancelled(true);
            if (e.getRawSlot() == 49) new ClanGui(plugin).openMain(p);
            return;
        }

        if (title.equals("Membros da clan")) {
            e.setCancelled(true);
            Clan c = plugin.clans().byPlayer(p.getUniqueId());
            if (c == null) { p.closeInventory(); return; }
            if (e.getRawSlot() == 49) { new ClanCommand(plugin).openMenu(p, c); return; }
            if (e.getRawSlot() < 0 || e.getRawSlot() >= 45) return;
            if (e.getCurrentItem() == null || !(e.getCurrentItem().getItemMeta() instanceof org.bukkit.inventory.meta.SkullMeta)) return;
            org.bukkit.inventory.meta.SkullMeta meta = (org.bukkit.inventory.meta.SkullMeta) e.getCurrentItem().getItemMeta();
            if (meta.getOwningPlayer() == null) return;
            UUID target = meta.getOwningPlayer().getUniqueId();
            if (target.equals(c.owner())) { p.sendMessage(plugin.msg("cannot-kick-leader")); return; }
            if (!c.role(p.getUniqueId()).canManage()) { p.sendMessage(plugin.msg("only-leader-mod")); return; }
            ClanRole targetRole = c.role(target);
            if (targetRole == null) return;
            if (!c.owner().equals(p.getUniqueId()) && targetRole != ClanRole.MEMBER) { p.sendMessage(plugin.msg("moderator-cannot-kick-manager")); return; }
            String targetName = meta.getOwningPlayer().getName();
            c.members().remove(target); plugin.clans().save();
            p.sendMessage(plugin.msg("member-kicked", "%player%", targetName == null ? target.toString().substring(0, 8) : targetName));
            Player online = Bukkit.getPlayer(target); if (online != null) online.sendMessage(plugin.msg("kicked-from-clan", "%name%", c.name()));
            new ClanGui(plugin).openMembers(p, c); return;
        }

        if (title.equals("Excluir clan")) {
            e.setCancelled(true);
            Clan c = plugin.clans().byPlayer(p.getUniqueId());
            if (c == null || !c.owner().equals(p.getUniqueId())) { p.closeInventory(); return; }
            if (e.getRawSlot() == 15) { new ClanCommand(plugin).openMenu(p, c); return; }
            if (e.getRawSlot() != 11) return;
            if (c.members().size() > 1) { p.sendMessage(plugin.msg("delete-requires-empty")); new ClanGui(plugin).openDelete(p, c); return; }
            String name = c.name(); plugin.clans().delete(c); p.closeInventory(); p.sendMessage(plugin.msg("deleted", "%name%", name)); return;
        }

        if (title.startsWith("Clan de ")) {
            e.setCancelled(true);
            Clan c = plugin.clans().byPlayer(p.getUniqueId());
            if (c == null) { p.closeInventory(); return; }
            switch (e.getRawSlot()) {
                case 10 -> { p.closeInventory(); p.sendMessage(plugin.msg("prefix") + ChatColor.WHITE + "Informações:"); p.sendMessage(ChatColor.GRAY + "Nome: " + ChatColor.WHITE + c.name()); p.sendMessage(ChatColor.GRAY + "Tag: " + ChatColor.GOLD + c.tag()); p.sendMessage(ChatColor.GRAY + "Membros: " + ChatColor.WHITE + c.members().size()); p.sendMessage(ChatColor.GRAY + "KDR médio: " + ChatColor.YELLOW + String.format(java.util.Locale.US, "%.2f", plugin.clans().clanKdr(c))); }
                case 12 -> new ClanGui(plugin).openMembers(p, c);
                case 14 -> { p.closeInventory(); p.performCommand("clan bau"); }
                case 16 -> { p.closeInventory(); p.performCommand("clan home"); }
                case 22 -> { p.closeInventory(); p.sendMessage(ChatColor.YELLOW + "Use /clan tag <TAG> para alterar a tag."); }
                case 24 -> { if (c.role(p.getUniqueId()).canManage()) { p.closeInventory(); p.performCommand("clan config"); } }
                case 18 -> { if (c.owner().equals(p.getUniqueId())) new ClanGui(plugin).openDelete(p, c); else p.sendMessage(plugin.msg("only-leader")); }
            }
            return;
        }

        if (title.equals("Configuração da clan")) {
            e.setCancelled(true); Clan c = plugin.clans().byPlayer(p.getUniqueId());
            if (c == null || !c.role(p.getUniqueId()).canManage()) { p.closeInventory(); return; }
            if (e.getRawSlot() == 11) { c.setFriendlyFire(!c.friendlyFire()); plugin.clans().save(); p.closeInventory(); p.performCommand("clan config"); }
            if (e.getRawSlot() == 15) { p.closeInventory(); p.performCommand("clan menu"); }
        }
    }

    private void openCreateGui(Player p) {
        Inventory inv = Bukkit.createInventory(null, 27, "§8Criar clan");
        ItemStack paper = new ItemStack(Material.PAPER); ItemMeta meta = paper.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "Criar Clan");
        meta.setLore(java.util.List.of(ChatColor.GRAY + "Clique para iniciar a criação.", "", ChatColor.YELLOW + "Você irá digitar o nome", ChatColor.YELLOW + "e depois a tag pelo chat."));
        paper.setItemMeta(meta); inv.setItem(13, paper);
        ItemStack back = new ItemStack(Material.ARROW); ItemMeta backMeta = back.getItemMeta(); backMeta.setDisplayName(ChatColor.GRAY + "Voltar"); back.setItemMeta(backMeta); inv.setItem(22, back);
        p.openInventory(inv);
    }

    private void startCreation(Player p) {
        if (plugin.clans().byPlayer(p.getUniqueId()) != null) { p.sendMessage(plugin.msg("already-clan")); return; }
        createSteps.put(p.getUniqueId(), CreateStep.NAME); createNames.remove(p.getUniqueId());
        p.sendMessage(plugin.raw("prefix") + ChatColor.YELLOW + "Digite no chat o " + ChatColor.WHITE + "nome da clan" + ChatColor.YELLOW + "."); sendCancelButton(p);
    }

    private void sendCancelButton(Player p) {
        TextComponent component = new TextComponent(ChatColor.RED + "§lClique AQUI para cancelar");
        component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/clan cancelar"));
        p.spigot().sendMessage(component);
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        UUID uuid = e.getPlayer().getUniqueId(); CreateStep step = createSteps.get(uuid); if (step == null) return;
        e.setCancelled(true); Player p = e.getPlayer(); String input = e.getMessage().trim();
        if (input.equalsIgnoreCase("cancelar")) { cancelCreation(p); return; }
        if (step == CreateStep.NAME) {
            int min = plugin.getConfig().getInt("clan.name-min-length", 3), max = plugin.getConfig().getInt("clan.name-max-length", 16);
            if (!input.matches("[A-Za-z0-9_\\-]+") || input.length() < min || input.length() > max) { p.sendMessage(plugin.raw("prefix") + ChatColor.RED + "Nome inválido. Use " + min + "-" + max + " caracteres, letras, números, _ ou -."); sendCancelButton(p); return; }
            if (plugin.clans().nameTaken(input)) { p.sendMessage(plugin.raw("prefix") + ChatColor.RED + "Já existe uma clan com esse nome."); sendCancelButton(p); return; }
            createNames.put(uuid, input); createSteps.put(uuid, CreateStep.TAG);
            p.sendMessage(plugin.raw("prefix") + ChatColor.YELLOW + "Agora digite no chat a " + ChatColor.WHITE + "TAG" + ChatColor.YELLOW + " da clan (3 letras MAIÚSCULAS, cores permitidas)."); sendCancelButton(p); return;
        }
        String name = createNames.remove(uuid); createSteps.remove(uuid);
        Bukkit.getScheduler().runTask(plugin, () -> { if (p.isOnline()) p.performCommand("clan criar " + name + " " + input); });
    }

    private void cancelCreation(Player p) {
        createSteps.remove(p.getUniqueId()); createNames.remove(p.getUniqueId());
        p.sendMessage(plugin.raw("prefix") + ChatColor.GRAY + "Criação da clan cancelada.");
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent e) {
        if (!e.getMessage().equalsIgnoreCase("/clan cancelar")) return;
        if (!createSteps.containsKey(e.getPlayer().getUniqueId())) return;
        e.setCancelled(true); cancelCreation(e.getPlayer());
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        String title = clean(e.getView().getTitle());
        if (!title.startsWith("Baú da clan:")) return;
        if (!(e.getPlayer() instanceof Player p)) return;
        Clan c = plugin.clans().byPlayer(p.getUniqueId()); if (c == null) return;
        c.setChest(Arrays.copyOf(e.getInventory().getContents(), 27)); plugin.clans().save();
    }
}
