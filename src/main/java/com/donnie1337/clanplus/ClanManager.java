package com.donnie1337.clanplus;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class ClanManager {
    private final ClanPlus plugin;
    private final Map<String, Clan> clans = new LinkedHashMap<>();
    private File file;
    private YamlConfiguration data;

    public ClanManager(ClanPlus plugin) { this.plugin = plugin; }

    public void load() {
        file = new File(plugin.getDataFolder(), plugin.getConfig().getString("storage.file", "clans.yml"));
        if (!file.exists()) {
            try { file.getParentFile().mkdirs(); file.createNewFile(); } catch (IOException e) { plugin.getLogger().severe("Não foi possível criar clans.yml: " + e.getMessage()); }
        }
        data = YamlConfiguration.loadConfiguration(file);
        clans.clear();
        ConfigurationSection root = data.getConfigurationSection("clans");
        if (root == null) return;
        for (String id : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(id);
            if (s == null) continue;
            try {
                UUID owner = UUID.fromString(s.getString("owner"));
                Clan clan = new Clan(id, s.getString("name", id), s.getString("tag", id), owner);
                clan.members().clear();
                ConfigurationSection members = s.getConfigurationSection("members");
                if (members != null) for (String uuid : members.getKeys(false)) clan.members().put(UUID.fromString(uuid), ClanRole.valueOf(members.getString(uuid, "MEMBER")));
                clan.setFriendlyFire(s.getBoolean("friendly-fire", false));
                String world = s.getString("home.world");
                if (world != null) {
                    World w = Bukkit.getWorld(world);
                    if (w != null) clan.setHome(new Location(w, s.getDouble("home.x"), s.getDouble("home.y"), s.getDouble("home.z"), (float)s.getDouble("home.yaw"), (float)s.getDouble("home.pitch")));
                }
                List<?> items = s.getList("chest");
                if (items != null) {
                    ItemStack[] chest = new ItemStack[27];
                    for (int i = 0; i < Math.min(27, items.size()); i++) if (items.get(i) instanceof ItemStack item) chest[i] = item;
                    clan.setChest(chest);
                }
                ConfigurationSection invites = s.getConfigurationSection("invites");
                if (invites != null) for (String key : invites.getKeys(false)) {
                    ConfigurationSection inv = invites.getConfigurationSection(key);
                    if (inv != null) clan.invites().put(Integer.parseInt(key), new Clan.Invite(Integer.parseInt(key), UUID.fromString(inv.getString("player")), inv.getLong("expires")));
                }
                clans.put(id, clan);
            } catch (Exception ex) { plugin.getLogger().warning("Clan inválida ignorada: " + id + " - " + ex.getMessage()); }
        }
        cleanupExpiredInvites();
    }

    public synchronized void save() {
        if (data == null) return;
        data.set("clans", null);
        for (Clan c : clans.values()) {
            String p = "clans." + c.id();
            data.set(p + ".name", c.name());
            data.set(p + ".tag", c.tag());
            data.set(p + ".owner", c.owner().toString());
            for (Map.Entry<UUID, ClanRole> m : c.members().entrySet()) data.set(p + ".members." + m.getKey(), m.getValue().name());
            data.set(p + ".friendly-fire", c.friendlyFire());
            if (c.home() != null) {
                data.set(p + ".home.world", c.home().getWorld().getName());
                data.set(p + ".home.x", c.home().getX()); data.set(p + ".home.y", c.home().getY()); data.set(p + ".home.z", c.home().getZ());
                data.set(p + ".home.yaw", c.home().getYaw()); data.set(p + ".home.pitch", c.home().getPitch());
            }
            data.set(p + ".chest", Arrays.asList(c.chest()));
            for (Clan.Invite inv : c.invites().values()) {
                data.set(p + ".invites." + inv.id() + ".player", inv.player().toString());
                data.set(p + ".invites." + inv.id() + ".expires", inv.expiresAt());
            }
        }
        try { data.save(file); } catch (IOException e) { plugin.getLogger().severe("Falha ao salvar clans.yml: " + e.getMessage()); }
    }

    public Collection<Clan> all() { return Collections.unmodifiableCollection(clans.values()); }
    public Clan byId(String id) { return clans.get(id); }
    public Clan byName(String name) { return clans.values().stream().filter(c -> c.name().equalsIgnoreCase(name)).findFirst().orElse(null); }
    public Clan byTag(String tag) { return clans.values().stream().filter(c -> c.tag().equalsIgnoreCase(tag)).findFirst().orElse(null); }
    public Clan byPlayer(UUID uuid) { return clans.values().stream().filter(c -> c.hasMember(uuid)).findFirst().orElse(null); }
    public boolean nameTaken(String name) { return byName(name) != null; }
    public boolean tagTaken(String tag) { return byTag(tag) != null; }
    public Clan create(String name, String tag, UUID owner) { String id = UUID.randomUUID().toString(); Clan c = new Clan(id, name, tag, owner); clans.put(id, c); save(); return c; }
    public void delete(Clan c) { clans.remove(c.id()); save(); }
    public void cleanupExpiredInvites() { long now = System.currentTimeMillis(); for (Clan c : clans.values()) c.invites().values().removeIf(i -> i.expiresAt() < now); }
    public int nextInviteId(Clan c) { int id = 1; while (c.invites().containsKey(id)) id++; return id; }
}
