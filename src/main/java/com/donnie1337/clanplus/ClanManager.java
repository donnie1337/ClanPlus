package com.donnie1337.clanplus;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

public final class ClanManager {
    private final ClanPlus plugin;
    private final Map<String, Clan> clans = new LinkedHashMap<>();
    private final Map<UUID, int[]> stats = new HashMap<>();
    private File file;
    private YamlConfiguration data;

    public ClanManager(ClanPlus plugin) { this.plugin = plugin; }
    private String plainTag(String tag) { return tag == null ? "" : tag.replaceAll("&[0-9a-fA-F]", ""); }

    public synchronized void load() {
        file = new File(plugin.getDataFolder(), plugin.getConfig().getString("storage.file", "clans.yml"));
        if (!file.exists()) {
            try { file.getParentFile().mkdirs(); file.createNewFile(); }
            catch (IOException e) { plugin.getLogger().severe("Não foi possível criar clans.yml: " + e.getMessage()); }
        }
        data = YamlConfiguration.loadConfiguration(file);
        clans.clear(); stats.clear();
        ConfigurationSection statsRoot = data.getConfigurationSection("stats");
        if (statsRoot != null) for (String uuid : statsRoot.getKeys(false)) try {
            ConfigurationSection s = statsRoot.getConfigurationSection(uuid);
            if (s != null) stats.put(UUID.fromString(uuid), new int[]{s.getInt("kills"), s.getInt("deaths")});
        } catch (IllegalArgumentException ignored) { }

        ConfigurationSection root = data.getConfigurationSection("clans");
        if (root != null) for (String id : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(id); if (s == null) continue;
            try {
                UUID owner = UUID.fromString(s.getString("owner"));
                Clan clan = new Clan(id, s.getString("name", id), s.getString("tag", id), owner);
                clan.members().clear();
                ConfigurationSection members = s.getConfigurationSection("members");
                if (members != null) for (String uuid : members.getKeys(false)) clan.members().put(UUID.fromString(uuid), ClanRole.valueOf(members.getString(uuid, "MEMBER")));
                clan.setFriendlyFire(s.getBoolean("friendly-fire", false));
                clan.setXp(s.getLong("progression.xp", 0L));
                clan.setCreatedAt(s.getLong("progression.created-at", System.currentTimeMillis()));
                ConfigurationSection upgrades = s.getConfigurationSection("progression.upgrades");
                if (upgrades != null) for (String key : upgrades.getKeys(false)) clan.setUpgrade(key, upgrades.getInt(key, 0));
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
                    ConfigurationSection inv = invites.getConfigurationSection(key); if (inv == null) continue;
                    int inviteId = Integer.parseInt(key);
                    clan.invites().put(inviteId, new Clan.Invite(inviteId, UUID.fromString(inv.getString("player")), inv.getLong("expires")));
                }
                clans.put(id, clan);
            } catch (Exception ex) { plugin.getLogger().warning("Clan inválida ignorada: " + id + " - " + ex.getMessage()); }
        }
        cleanupExpiredInvites();
    }

    public synchronized void save() {
        if (data == null || file == null) return;
        data.set("clans", null); data.set("stats", null);
        for (Map.Entry<UUID, int[]> entry : stats.entrySet()) {
            data.set("stats." + entry.getKey() + ".kills", entry.getValue()[0]);
            data.set("stats." + entry.getKey() + ".deaths", entry.getValue()[1]);
        }
        for (Clan c : clans.values()) {
            String p = "clans." + c.id();
            data.set(p + ".name", c.name()); data.set(p + ".tag", c.tag()); data.set(p + ".owner", c.owner().toString());
            for (Map.Entry<UUID, ClanRole> m : c.members().entrySet()) data.set(p + ".members." + m.getKey(), m.getValue().name());
            data.set(p + ".friendly-fire", c.friendlyFire());
            data.set(p + ".progression.xp", c.xp());
            data.set(p + ".progression.created-at", c.createdAt());
            for (Map.Entry<String, Integer> upgrade : c.upgrades().entrySet()) data.set(p + ".progression.upgrades." + upgrade.getKey(), upgrade.getValue());
            if (c.home() != null) {
                data.set(p + ".home.world", c.home().getWorld().getName()); data.set(p + ".home.x", c.home().getX()); data.set(p + ".home.y", c.home().getY());
                data.set(p + ".home.z", c.home().getZ()); data.set(p + ".home.yaw", c.home().getYaw()); data.set(p + ".home.pitch", c.home().getPitch());
            }
            data.set(p + ".chest", Arrays.asList(c.chest()));
            for (Clan.Invite inv : c.invites().values()) {
                data.set(p + ".invites." + inv.id() + ".player", inv.player().toString());
                data.set(p + ".invites." + inv.id() + ".expires", inv.expiresAt());
            }
        }
        File temp = new File(file.getParentFile(), file.getName() + ".tmp");
        File backup = new File(file.getParentFile(), file.getName() + ".bak");
        try {
            data.save(temp);
            if (file.exists()) Files.copy(file.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
            Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception atomicFailure) {
            try {
                data.save(temp);
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception e) {
                plugin.getLogger().severe("Falha ao salvar clans.yml com segurança: " + e.getMessage());
            }
        }
    }

    public Collection<Clan> all() { return Collections.unmodifiableCollection(clans.values()); }
    public Clan byId(String id) { return clans.get(id); }
    public Clan byName(String name) { return clans.values().stream().filter(c -> c.name().equalsIgnoreCase(name)).findFirst().orElse(null); }
    public Clan byTag(String tag) { String plain = plainTag(tag); return clans.values().stream().filter(c -> plainTag(c.tag()).equalsIgnoreCase(plain)).findFirst().orElse(null); }
    public Clan byPlayer(UUID uuid) { return clans.values().stream().filter(c -> c.hasMember(uuid)).findFirst().orElse(null); }
    public boolean nameTaken(String name) { return byName(name) != null; }
    public boolean tagTaken(String tag) { return byTag(tag) != null; }
    public Clan create(String name, String tag, UUID owner) { String id = UUID.randomUUID().toString(); Clan c = new Clan(id, name, tag, owner); clans.put(id, c); stats.putIfAbsent(owner, new int[2]); save(); return c; }
    public void delete(Clan c) { clans.remove(c.id()); save(); }
    public void cleanupExpiredInvites() { long now = System.currentTimeMillis(); for (Clan c : clans.values()) c.invites().values().removeIf(i -> i.expiresAt() < now); }
    public int nextInviteId(Clan c) { int id = 1; while (c.invites().containsKey(id)) id++; return id; }

    public void recordDeath(UUID uuid) {
        stats.computeIfAbsent(uuid, x -> new int[2])[1]++;
        Clan c = byPlayer(uuid); if (c != null) addXp(c, 2);
        save();
    }
    public void recordKill(UUID uuid) {
        stats.computeIfAbsent(uuid, x -> new int[2])[0]++;
        Clan c = byPlayer(uuid); if (c != null) addXp(c, Math.round(10 * c.xpMultiplier()));
        save();
    }
    public void addXp(Clan clan, long amount) { if (clan == null || amount <= 0) return; clan.setXp(clan.xp() + amount); }
    public int upgradeCost(Clan clan, String key) { int level = clan == null ? 0 : clan.upgrade(key); return 500 * (level + 1); }
    public boolean buyUpgrade(Clan clan, String key) {
        if (clan == null || (!key.equals("member_limit") && !key.equals("xp_boost"))) return false;
        int cost = upgradeCost(clan, key);
        if (clan.xp() < cost) return false;
        clan.setXp(clan.xp() - cost); clan.setUpgrade(key, clan.upgrade(key) + 1); save(); return true;
    }
    public List<Clan> ranking() {
        List<Clan> result = new ArrayList<>(clans.values());
        result.sort(Comparator.comparingInt(Clan::level).reversed().thenComparingDouble(this::clanKdr).reversed().thenComparingLong(Clan::xp).reversed());
        return result;
    }
    public int rank(Clan clan) { if (clan == null) return 0; List<Clan> list = ranking(); for (int i = 0; i < list.size(); i++) if (list.get(i).id().equals(clan.id())) return i + 1; return 0; }

    public int kills(UUID uuid) { return stats.getOrDefault(uuid, new int[2])[0]; }
    public int deaths(UUID uuid) { return stats.getOrDefault(uuid, new int[2])[1]; }
    public double kdr(UUID uuid) { int deaths = deaths(uuid); return deaths == 0 ? kills(uuid) : (double) kills(uuid) / deaths; }
    public Set<UUID> statsPlayers() { return Collections.unmodifiableSet(stats.keySet()); }
    public double clanKdr(Clan clan) { if (clan.members().isEmpty()) return 0.0; return clan.members().keySet().stream().mapToDouble(this::kdr).average().orElse(0.0); }
}
