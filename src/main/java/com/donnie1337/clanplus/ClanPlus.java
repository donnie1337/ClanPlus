package com.donnie1337.clanplus;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ClanPlus extends JavaPlugin {
    private ClanManager clanManager; private Map<String,String> messages; private final Map<String,Inventory> openClanChests=new HashMap<>();
    @Override public void onEnable(){saveDefaultConfig();saveResource("messages.yml",false);loadMessages();clanManager=new ClanManager(this);clanManager.load();ClanCommand cc=new ClanCommand(this);PluginCommand clan=getCommand("clan");if(clan!=null){clan.setExecutor(cc);clan.setTabCompleter(cc);}PluginCommand clans=getCommand("clans");if(clans!=null)clans.setExecutor(new ClansCommand(this));PluginCommand admin=getCommand("clanadmin");if(admin!=null){ClanAdminCommand a=new ClanAdminCommand(this);admin.setExecutor(a);admin.setTabCompleter(a);}getServer().getPluginManager().registerEvents(new ClanListener(this),this);getServer().getPluginManager().registerEvents(new ClanFeatureListener(this),this);long interval=6000L;getServer().getScheduler().runTaskTimer(this,()->{if(clanManager!=null){saveOpenChests();clanManager.save();}},interval,interval);getLogger().info("ClanPlus habilitado com "+clanManager.all().size()+" clan(s).");}
    @Override public void onDisable(){saveOpenChests();if(clanManager!=null)clanManager.save();}
    private void saveOpenChests(){if(clanManager==null)return;for(Map.Entry<String,Inventory> e:openClanChests.entrySet()){Clan c=clanManager.byId(e.getKey());if(c!=null)c.setChest(e.getValue().getContents());}}
    public Inventory clanChest(Clan clan){Inventory existing=openClanChests.get(clan.id());if(existing!=null)return existing;Inventory inv=Bukkit.createInventory(null,27,ChatColor.translateAlternateColorCodes('&',raw("chest-title","%name%",clan.name())));inv.setContents(clan.chest());openClanChests.put(clan.id(),inv);return inv;}
    public boolean isManagedClanChest(Inventory inventory){return inventory!=null&&openClanChests.values().stream().anyMatch(i->i==inventory);}
    public void persistClanChest(Clan clan,Inventory inventory){if(clan==null||inventory==null||!isManagedClanChest(inventory))return;clan.setChest(inventory.getContents());clanManager.save();boolean inUse=false;for(Player p:Bukkit.getOnlinePlayers())if(p.getOpenInventory().getTopInventory()==inventory){inUse=true;break;}if(!inUse)openClanChests.remove(clan.id());}
    public void closeClanChest(Clan clan){if(clan==null)return;Inventory inv=openClanChests.remove(clan.id());if(inv!=null){clan.setChest(inv.getContents());for(Player p:Bukkit.getOnlinePlayers())if(p.getOpenInventory().getTopInventory()==inv)p.closeInventory();}}
    public void loadMessages(){var cfg=org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(new java.io.File(getDataFolder(),"messages.yml"));messages=new java.util.HashMap<>();for(String key:cfg.getKeys(false))messages.put(key,cfg.getString(key,""));}
    public String msg(String key,String... replacements){String value=messages.getOrDefault(key,key);for(int i=0;i+1<replacements.length;i+=2)value=value.replace(replacements[i],replacements[i+1]);return ChatColor.translateAlternateColorCodes('&',messages.getOrDefault("prefix","")+value);}
    public String raw(String key,String... replacements){String value=messages.getOrDefault(key,key);for(int i=0;i+1<replacements.length;i+=2)value=value.replace(replacements[i],replacements[i+1]);return ChatColor.translateAlternateColorCodes('&',value);}
    public String getPlayerTag(UUID playerId){if(playerId==null||clanManager==null)return"";Clan c=clanManager.byPlayer(playerId);return c==null?"":c.tag();}
    public String getPlayerTag(Player player){return player==null?"":getPlayerTag(player.getUniqueId());}
    public ClanManager clans(){return clanManager;}
}
