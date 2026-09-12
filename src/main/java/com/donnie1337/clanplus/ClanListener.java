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
import org.bukkit.event.inventory.InventoryDragEvent;
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
        if (LoginPlusHook.isAuthenticated(victim)) plugin.clans().recordDeath(victim.getUniqueId());
        Player killer = victim.getKiller();
        if (killer != null && !killer.getUniqueId().equals(victim.getUniqueId()) && LoginPlusHook.isAuthenticated(killer)) plugin.clans().recordKill(killer.getUniqueId());
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (!LoginPlusHook.requireAuthentication(plugin, p)) { e.setCancelled(true); p.closeInventory(); return; }
        String title = clean(e.getView().getTitle());
        if (title.startsWith("Baú da clan:")) return;
        e.setCancelled(true);
        if (title.equals("ᴄʟᴀɴ")) {
            switch (e.getRawSlot()) {
                case 11 -> { if (plugin.clans().byPlayer(p.getUniqueId()) == null) startCreation(p); else { p.closeInventory(); p.performCommand("clan menu"); } }
                case 13 -> new ClanGui(plugin).openInvites(p);
                case 14 -> new ClanGui(plugin).openTop(p);
                case 16 -> new ClanGui(plugin).openAll(p);
                case 22 -> new ClanGui(plugin).openKdr(p);
                case 26 -> p.closeInventory();
                default -> { }
            }
            return;
        }
        if (title.equals("Convites recebidos")) {
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
            if (e.getRawSlot() == 49) new ClanGui(plugin).openMain(p);
            return;
        }
        if (title.equals("Membros da clan")) {
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
            Clan c = plugin.clans().byPlayer(p.getUniqueId());
            if (c == null || !c.owner().equals(p.getUniqueId())) { p.closeInventory(); return; }
            if (e.getRawSlot() == 15) { new ClanCommand(plugin).openMenu(p, c); return; }
            if (e.getRawSlot() != 11) return;
            if (c.members().size() > 1) { p.sendMessage(plugin.msg("delete-requires-empty")); new ClanGui(plugin).openDelete(p, c); return; }
            String name = c.name(); plugin.clans().delete(c); p.closeInventory(); p.sendMessage(plugin.msg("deleted", "%name%", name)); return;
        }
        if (title.startsWith("Clan de ")) {
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
            Clan c = plugin.clans().byPlayer(p.getUniqueId());
            if (c == null || !c.role(p.getUniqueId()).canManage()) { p.closeInventory(); return; }
            if (e.getRawSlot() == 11) { c.setFriendlyFire(!c.friendlyFire()); plugin.clans().save(); p.closeInventory(); p.performCommand("clan config"); }
            if (e.getRawSlot() == 15) { p.closeInventory(); p.performCommand("clan menu"); }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        String title = clean(e.getView().getTitle());
        if (title.startsWith("Baú da clan:")) return;
        if (isClanGui(title)) e.setCancelled(true);
    }

    private boolean isClanGui(String title) {
        return title.equals("ᴄʟᴀɴ") || title.equals("Convites recebidos") || title.equals("Clans mais top") || title.equals("Clans do servidor") || title.equals("Ranking de KDR") || title.equals("Membros da clan") || title.equals("Excluir clan") || title.startsWith("Clan de ") || title.equals("Configuração da clan");
    }

    private void startCreation(Player p) {
        if (!LoginPlusHook.requireAuthentication(plugin, p)) return;
        if (plugin.clans().byPlayer(p.getUniqueId()) != null) { p.sendMessage(plugin.msg("already-clan")); return; }
        p.closeInventory();
        createSteps.put(p.getUniqueId(), CreateStep.NAME); createNames.remove(p.getUniqueId());
        int min = plugin.getConfig().getInt("clan.name-min-length", 3);
        int max = plugin.getConfig().getInt("clan.name-max-length", 16);
        p.sendMessage(plugin.msg("create-start", "%min%", String.valueOf(min), "%max%", String.valueOf(max)));
        sendCancelButton(p);
    }

    private void sendCancelButton(Player p) {
        TextComponent component = new TextComponent(ChatColor.GRAY + "Clique " + ChatColor.RED + "AQUI" + ChatColor.GRAY + " para cancelar");
        component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/trigger clanplus_cancel"));
        p.spigot().sendMessage(component);
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        UUID uuid = e.getPlayer().getUniqueId(); CreateStep step = createSteps.get(uuid); if (step == null) return;
        e.setCancelled(true); Player p = e.getPlayer();
        if (!LoginPlusHook.isAuthenticated(p)) { createSteps.remove(uuid); createNames.remove(uuid); p.sendMessage(plugin.msg("login-required")); return; }
        String input = e.getMessage().trim();
        if (step == CreateStep.NAME && input.equalsIgnoreCase("cancelar")) { cancelCreation(p); return; }
        if (step == CreateStep.NAME) {
            int min = plugin.getConfig().getInt("clan.name-min-length", 3), max = plugin.getConfig().getInt("clan.name-max-length", 16);
            if (!input.matches("[A-Za-z0-9_\\-]+") || input.length() < min || input.length() > max) {
                p.sendMessage(plugin.msg("create-invalid-name", "%min%", String.valueOf(min), "%max%", String.valueOf(max)));
                sendCancelButton(p);
                return;
            }
            if (plugin.clans().nameTaken(input)) {
                p.sendMessage(plugin.msg("create-name-taken"));
                sendCancelButton(p);
                return;
            }
            createNames.put(uuid, input); createSteps.put(uuid, CreateStep.TAG);
            p.sendMessage(plugin.msg("create-name-set", "%name%", input));
            return;
        }
        String name = createNames.remove(uuid); createSteps.remove(uuid);
        if (name == null || name.isBlank()) return;
        Bukkit.getScheduler().runTask(plugin, () -> { if (p.isOnline() && LoginPlusHook.isAuthenticated(p)) p.performCommand("clan criar " + name + " " + input); });
    }

    private void cancelCreation(Player p) {
        createSteps.remove(p.getUniqueId()); createNames.remove(p.getUniqueId());
        p.sendMessage(plugin.msg("create-cancelled"));
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent e) {
        String message = e.getMessage().trim();
        Player p = e.getPlayer();
        if (message.equalsIgnoreCase("/trigger clanplus_cancel")) {
            if (createSteps.get(p.getUniqueId()) == CreateStep.NAME && LoginPlusHook.isAuthenticated(p)) {
                e.setCancelled(true);
                cancelCreation(p);
            }
            return;
        }
        if (!message.regionMatches(true, 0, "/clan", 0, 5)) return;
        String rest = message.length() > 5 ? message.substring(5) : "";
        if (!rest.isEmpty() && !Character.isWhitespace(rest.charAt(0))) return;
        if (!LoginPlusHook.requireAuthentication(plugin, p)) {
            e.setCancelled(true);
            createSteps.remove(p.getUniqueId());
            createNames.remove(p.getUniqueId());
            if (p.getOpenInventory() != null && !clean(p.getOpenInventory().getTitle()).startsWith("Baú da clan:")) p.closeInventory();
            return;
        }
        if (!message.equalsIgnoreCase("/clan cancelar")) return;
        if (createSteps.get(p.getUniqueId()) != CreateStep.NAME) return;
        e.setCancelled(true); cancelCreation(p);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        String title = clean(e.getView().getTitle());
        if (!title.startsWith("Baú da clan:")) return;
        if (!(e.getPlayer() instanceof Player p)) return;
        if (!LoginPlusHook.isAuthenticated(p)) return;
        Clan c = plugin.clans().byPlayer(p.getUniqueId()); if (c == null) return;
        c.setChest(Arrays.copyOf(e.getInventory().getContents(), 27)); plugin.clans().save();
    }
}
