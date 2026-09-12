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
    private String plainTag(String tag) { return tag == null ? "" : tag.replaceAll("(?i)&[0-9a-f]", ""); }
    private long xpPerLevel() { return Math.max(1L, plugin.getConfig().getLong("progression.xp-per-level", 1000L)); }
    private int maxLevel() { return Math.max(1, plugin.getConfig().getInt("progression.max-level", 100)); }
    private int maxUpgrade(String key) { return Math.max(0, plugin.getConfig().getInt("progression.upgrades." + key + "-max", 10)); }

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
            if (s != null) stats.put(UUID.fromString(uuid), new int[]{Math.max(0,s.getInt("kills")), Math.max(0,s.getInt("deaths"))});
        } catch (IllegalArgumentException ignored) { plugin.getLogger().warning("UUID de estatística inválido ignorado: " + uuid); }

        ConfigurationSection root = data.getConfigurationSection("clans");
        if (root != null) for (String id : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(id); if (s == null) continue;
            try {
                UUID owner = UUID.fromString(s.getString("owner"));
                String name = s.getString("name", id);
                String tag = s.getString("tag", id);
                Clan clan = new Clan(id, name, tag, owner);
                clan.members().clear();
                ConfigurationSection members = s.getConfigurationSection("members");
                if (members != null) for (String uuid : members.getKeys(false)) try {
                    ClanRole role = ClanRole.valueOf(members.getString(uuid, "MEMBER"));
                    clan.members().put(UUID.fromString(uuid), role);
                } catch (Exception badMember) { plugin.getLogger().warning("Membro inválido ignorado no clan " + name + ": " + uuid); }
                repairState(clan);
                clan.setFriendlyFire(s.getBoolean("friendly-fire", false));
                clan.setXp(s.getLong("progression.xp", 0L));
                clan.setCreatedAt(s.getLong("progression.created-at", System.currentTimeMillis()));
                ConfigurationSection upgrades = s.getConfigurationSection("progression.upgrades");
                if (upgrades != null) for (String key : upgrades.getKeys(false)) clan.setUpgrade(key, Math.min(upgrades.getInt(key, 0), maxUpgrade(key)));
                String world = s.getString("home.world");
                if (world != null) {
                    World w = Bukkit.getWorld(world);
                    if (w != null) clan.setHome(new Location(w, s.getDouble("home.x"), s.getDouble("home.y"), s.getDouble("home.z"), (float)s.getDouble("home.yaw"), (float)s.getDouble("home.pitch")));
                }
                List<?> items = s.getList("chest");
                if (items != null) {
                    ItemStack[] chest = new ItemStack[27];
                    for (int i = 0; i < Math.min(27, items.size()); i++) if (items.get(i) instanceof ItemStack item) chest[i] = item.clone();
                    clan.setChest(chest);
                }
                ConfigurationSection invites = s.getConfigurationSection("invites");
                if (invites != null) for (String key : invites.getKeys(false)) try {
                    ConfigurationSection inv = invites.getConfigurationSection(key); if (inv == null) continue;
                    int inviteId = Integer.parseInt(key); UUID player = UUID.fromString(inv.getString("player"));
                    clan.invites().put(inviteId, new Clan.Invite(inviteId, player, inv.getLong("expires")));
                } catch (Exception badInvite) { plugin.getLogger().warning("Convite inválido ignorado no clan " + name + ": " + key); }
                clans.put(id, clan);
            } catch (Exception ex) { plugin.getLogger().warning("Clan inválido ignorado: " + id + " - " + ex.getMessage()); }
        }
        validateAll();
        cleanupExpiredInvites();
    }

    private void repairState(Clan clan) {
        if (clan.members().isEmpty()) clan.members().put(clan.owner(), ClanRole.LEADER);
        clan.members().remove(null);
        clan.members().entrySet().removeIf(e -> e.getKey() == null || e.getValue() == null);
        ClanRole ownerRole = clan.members().get(clan.owner());
        if (ownerRole == null) clan.members().put(clan.owner(), ClanRole.LEADER);
        else if (ownerRole != ClanRole.LEADER) clan.members().put(clan.owner(), ClanRole.LEADER);
        clan.members().replaceAll((uuid, role) -> uuid.equals(clan.owner()) ? ClanRole.LEADER : role);
    }

    private void validateAll() {
        Map<UUID, String> seen = new HashMap<>();
        Iterator<Map.Entry<String, Clan>> it = clans.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Clan> entry = it.next(); Clan c = entry.getValue();
            repairState(c);
            if (c.name() == null || c.name().isBlank() || c.tag() == null || c.tag().isBlank()) { it.remove(); continue; }
            for (UUID uuid : new ArrayList<>(c.members().keySet())) {
                String previous = seen.putIfAbsent(uuid, c.id());
                if (previous != null && !previous.equals(c.id())) c.members().remove(uuid);
            }
            if (!c.members().containsKey(c.owner())) c.members().put(c.owner(), ClanRole.LEADER);
        }
    }

    public synchronized void save() {
        if (data == null || file == null) return;
        data.set("clans", null); data.set("stats", null);
        for (Map.Entry<UUID, int[]> entry : stats.entrySet()) {
            data.set("stats." + entry.getKey() + ".kills", Math.max(0, entry.getValue()[0]));
            data.set("stats." + entry.getKey() + ".deaths", Math.max(0, entry.getValue()[1]));
        }
        for (Clan c : clans.values()) {
            repairState(c);
            String p = "clans." + c.id();
            data.set(p + ".name", c.name()); data.set(p + ".tag", c.tag()); data.set(p + ".owner", c.owner().toString());
            for (Map.Entry<UUID, ClanRole> m : c.members().entrySet()) data.set(p + ".members." + m.getKey(), m.getValue().name());
            data.set(p + ".friendly-fire", c.friendlyFire()); data.set(p + ".progression.xp", c.xp()); data.set(p + ".progression.created-at", c.createdAt());
            for (Map.Entry<String,Integer> upgrade : c.upgrades().entrySet()) data.set(p + ".progression.upgrades." + upgrade.getKey(), Math.min(upgrade.getValue(), maxUpgrade(upgrade.getKey())));
            if (c.home() != null && c.home().getWorld() != null) {
                data.set(p + ".home.world", c.home().getWorld().getName()); data.set(p + ".home.x", c.home().getX()); data.set(p + ".home.y", c.home().getY()); data.set(p + ".home.z", c.home().getZ()); data.set(p + ".home.yaw", c.home().getYaw()); data.set(p + ".home.pitch", c.home().getPitch());
            }
            ItemStack[] chest = c.chest(); List<ItemStack> items = new ArrayList<>(27); for (ItemStack item : chest) items.add(item == null ? null : item.clone()); data.set(p + ".chest", items);
            for (Clan.Invite inv : c.invites().values()) { data.set(p + ".invites." + inv.id() + ".player", inv.player().toString()); data.set(p + ".invites." + inv.id() + ".expires", inv.expiresAt()); }
        }
        File temp = new File(file.getParentFile(), file.getName() + ".tmp");
        File backup = new File(file.getParentFile(), file.getName() + ".bak");
        File backup1 = new File(file.getParentFile(), file.getName() + ".bak.1");
        File backup2 = new File(file.getParentFile(), file.getName() + ".bak.2");
        try {
            data.save(temp);
            if (file.exists()) {
                if (backup1.exists()) Files.copy(backup1.toPath(), backup2.toPath(), StandardCopyOption.REPLACE_EXISTING);
                if (backup.exists()) Files.copy(backup.toPath(), backup1.toPath(), StandardCopyOption.REPLACE_EXISTING);
                Files.copy(file.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            try { Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (Exception ignored) { Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING); }
        } catch (Exception e) { plugin.getLogger().severe("Falha ao salvar clans.yml com segurança: " + e.getMessage()); }
    }

    public Collection<Clan> all() { return Collections.unmodifiableCollection(clans.values()); }
    public Clan byId(String id) { return id == null ? null : clans.get(id); }
    public Clan byName(String name) { return name == null ? null : clans.values().stream().filter(c -> c.name().equalsIgnoreCase(name)).findFirst().orElse(null); }
    public Clan byTag(String tag) { String plain = plainTag(tag); return clans.values().stream().filter(c -> plainTag(c.tag()).equalsIgnoreCase(plain)).findFirst().orElse(null); }
    public Clan byPlayer(UUID uuid) { return uuid == null ? null : clans.values().stream().filter(c -> c.hasMember(uuid)).findFirst().orElse(null); }
    public boolean nameTaken(String name) { return byName(name) != null; }
    public boolean tagTaken(String tag) { return byTag(tag) != null; }
    public Clan create(String name, String tag, UUID owner) { String id = UUID.randomUUID().toString(); Clan c = new Clan(id,name,tag,owner); clans.put(id,c); stats.putIfAbsent(owner,new int[2]); save(); return c; }
    public void delete(Clan c) { if (c != null) { clans.remove(c.id()); save(); } }
    public void cleanupExpiredInvites() { long now=System.currentTimeMillis(); for (Clan c:clans.values()) c.invites().values().removeIf(i->i.expiresAt()<=now); }
    public int nextInviteId(Clan c) { int id=1; while(c.invites().containsKey(id)) id++; return id; }

    public void recordDeath(UUID uuid) { stats.computeIfAbsent(uuid,x->new int[2])[1]++; Clan c=byPlayer(uuid); if(c!=null)addXp(c,Math.max(0,plugin.getConfig().getLong("progression.death-xp",2L))); save(); }
    public void recordKill(UUID uuid) { stats.computeIfAbsent(uuid,x->new int[2])[0]++; Clan c=byPlayer(uuid); if(c!=null)addXp(c,Math.round(Math.max(0,plugin.getConfig().getLong("progression.kill-xp",10L))*c.xpMultiplier(maxUpgrade("xp-boost")))); save(); }
    public void addXp(Clan clan,long amount){if(clan!=null&&amount>0)clan.setXp(clan.xp()+amount);}
    public int upgradeCost(Clan clan,String key){int level=clan==null?0:clan.upgrade(key);return Math.max(1,plugin.getConfig().getInt("progression.upgrades.base-cost",500))*(level+1);}
    public boolean buyUpgrade(Clan clan,String key){if(clan==null||(!key.equals("member_limit")&&!key.equals("xp_boost")))return false;int max=maxUpgrade(key);if(clan.upgrade(key)>=max)return false;int cost=upgradeCost(clan,key);if(clan.xp()<cost)return false;clan.setXp(clan.xp()-cost);clan.setUpgrade(key,clan.upgrade(key)+1);save();return true;}
    public int level(Clan c){return c==null?0:c.level(xpPerLevel(),maxLevel());}
    public List<Clan> ranking(){List<Clan> result=new ArrayList<>(clans.values());result.sort(Comparator.comparingInt((Clan c)->level(c)).reversed().thenComparing(Comparator.comparingDouble(this::clanKdr).reversed()).thenComparing(Comparator.comparingLong(Clan::xp).reversed()).thenComparing(Clan::name,String.CASE_INSENSITIVE_ORDER));return result;}
    public int rank(Clan clan){if(clan==null)return 0;List<Clan> list=ranking();for(int i=0;i<list.size();i++)if(list.get(i).id().equals(clan.id()))return i+1;return 0;}
    public int kills(UUID uuid){return stats.getOrDefault(uuid,new int[2])[0];}
    public int deaths(UUID uuid){return stats.getOrDefault(uuid,new int[2])[1];}
    public double kdr(UUID uuid){int deaths=deaths(uuid);return deaths==0?kills(uuid):(double)kills(uuid)/deaths;}
    public Set<UUID> statsPlayers(){return Collections.unmodifiableSet(stats.keySet());}
    public double clanKdr(Clan clan){if(clan==null||clan.members().isEmpty())return 0.0;return clan.members().keySet().stream().mapToDouble(this::kdr).average().orElse(0.0);}
    public int memberLimit(Clan clan){return clan==null?plugin.getConfig().getInt("clan.max-members",30):clan.memberLimit(plugin.getConfig().getInt("clan.max-members",30),maxUpgrade("member-limit"));}
}
