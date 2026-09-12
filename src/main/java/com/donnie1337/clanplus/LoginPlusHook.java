package com.donnie1337.clanplus;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

/** Integração opcional e fail-closed com o LoginPlus. */
public final class LoginPlusHook {
    private static final String LOGIN_PLUS = "LoginPlus";
    private static Method isAuthenticatedMethod;
    private static boolean initialized;
    private static boolean loginPlusPresent;

    private LoginPlusHook() {}

    private static synchronized void initialize() {
        if (initialized) return;
        initialized = true;
        Plugin plugin = Bukkit.getPluginManager().getPlugin(LOGIN_PLUS);
        if (plugin == null || !plugin.isEnabled()) return;
        loginPlusPresent = true;
        try {
            isAuthenticatedMethod = plugin.getClass().getMethod("isAuthenticated", Player.class);
        } catch (ReflectiveOperationException ex) {
            isAuthenticatedMethod = null;
            Bukkit.getLogger().severe("[ClanPlus] LoginPlus encontrado, mas a API de autenticação não pôde ser acessada. O ClanPlus bloqueará o acesso por segurança.");
        }
    }

    public static boolean isAuthenticated(Player player) {
        if (player == null) return false;
        initialize();
        if (!loginPlusPresent) return true;
        if (isAuthenticatedMethod == null) return false;
        Plugin plugin = Bukkit.getPluginManager().getPlugin(LOGIN_PLUS);
        if (plugin == null || !plugin.isEnabled()) return false;
        try {
            return Boolean.TRUE.equals(isAuthenticatedMethod.invoke(plugin, player));
        } catch (ReflectiveOperationException | RuntimeException ex) {
            Bukkit.getLogger().warning("[ClanPlus] Falha ao consultar o estado de autenticação de " + player.getName() + ". Acesso bloqueado.");
            return false;
        }
    }

    public static boolean requireAuthentication(ClanPlus plugin, Player player) {
        if (isAuthenticated(player)) return true;
        player.sendMessage(plugin.msg("login-required"));
        return false;
    }

    public static void reset() {
        initialized = false;
        loginPlusPresent = false;
        isAuthenticatedMethod = null;
    }
}
