package dev.hookforge.adapter.permissions;

import dev.hookforge.api.permission.PermissionService;
import dev.hookforge.api.result.HookResult;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/** Uses Bukkit's effective permission calculation; LuckPerms integrates into this API. */
public final class BukkitPermissionAdapter implements PermissionService {
    private final JavaPlugin plugin;
    private volatile boolean enabled = true;
    public BukkitPermissionAdapter(JavaPlugin plugin) { this.plugin = plugin; }
    public void configure(boolean enabled) { this.enabled = enabled; }
    @Override public String providerName() {
        var luckPerms = Bukkit.getPluginManager().getPlugin("LuckPerms");
        return luckPerms != null && luckPerms.isEnabled() ? "Bukkit permissions (LuckPerms detected)" : "Bukkit permissions";
    }
    @Override public boolean isAvailable() { return enabled; }
    @Override public HookResult<Boolean> has(Player player, String permission) {
        if (!enabled) return HookResult.failure("Permission integration đã bị tắt trong config.");
        if (player == null) return HookResult.failure("Player không được null.");
        if (permission == null || permission.isBlank()) return HookResult.failure("Permission node không được để trống.");
        return HookResult.success(player.hasPermission(permission), "Đã kiểm tra quyền qua Bukkit.");
    }
}
