package com.donnie1337.clanplus;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class Clan {
    private final String id;
    private String name;
    private String tag;
    private UUID owner;
    private final Map<UUID, ClanRole> members = new LinkedHashMap<>();
    private final Map<Integer, Invite> invites = new LinkedHashMap<>();
    private final Map<String, Integer> upgrades = new LinkedHashMap<>();
    private Location home;
    private ItemStack[] chest = new ItemStack[27];
    private boolean friendlyFire = false;
    private long xp;
    private long createdAt;

    public Clan(String id, String name, String tag, UUID owner) {
        this.id = id;
        this.name = name;
        this.tag = tag;
        this.owner = owner;
        this.members.put(owner, ClanRole.LEADER);
        this.createdAt = System.currentTimeMillis();
    }

    public String id() { return id; }
    public String name() { return name; }
    public String tag() { return tag; }
    public UUID owner() { return owner; }
    public Map<UUID, ClanRole> members() { return members; }
    public Map<Integer, Invite> invites() { return invites; }
    public Map<String, Integer> upgrades() { return upgrades; }
    public Location home() { return home; }
    public ItemStack[] chest() { return chest; }
    public boolean friendlyFire() { return friendlyFire; }
    public long xp() { return xp; }
    public long createdAt() { return createdAt; }

    public void setName(String name) { this.name = name; }
    public void setTag(String tag) { this.tag = tag; }
    public void setOwner(UUID owner) { this.owner = owner; members.put(owner, ClanRole.LEADER); }
    public void setHome(Location home) { this.home = home; }
    public void setChest(ItemStack[] chest) { this.chest = chest == null ? new ItemStack[27] : chest; }
    public void setFriendlyFire(boolean friendlyFire) { this.friendlyFire = friendlyFire; }
    public void setXp(long xp) { this.xp = Math.max(0L, xp); }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt > 0 ? createdAt : System.currentTimeMillis(); }
    public void setUpgrade(String key, int level) { if (key != null && !key.isBlank()) upgrades.put(key, Math.max(0, level)); }

    public int upgrade(String key) { return upgrades.getOrDefault(key, 0); }
    public int level() { return (int) Math.min(100, xp / 1000L + 1); }
    public long xpIntoLevel() { return xp % 1000L; }
    public long xpForNextLevel() { return 1000L; }
    public double levelProgress() { return xpIntoLevel() / 1000.0D; }
    public int memberLimit(int base) { return base + upgrade("member_limit") * 5; }
    public double xpMultiplier() { return 1.0D + upgrade("xp_boost") * 0.10D; }

    public ClanRole role(UUID player) { return members.get(player); }
    public boolean hasMember(UUID player) { return members.containsKey(player); }
    public int onlineCount() { return (int) members.keySet().stream().filter(u -> org.bukkit.Bukkit.getPlayer(u) != null).count(); }

    public record Invite(int id, UUID player, long expiresAt) {}
}
