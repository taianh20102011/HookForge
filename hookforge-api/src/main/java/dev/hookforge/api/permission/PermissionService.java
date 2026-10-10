package dev.hookforge.api.permission;

import dev.hookforge.api.result.HookResult;
import org.bukkit.entity.Player;

/** Checks effective Bukkit permissions; does not grant or revoke permissions. */
public interface PermissionService {
    String providerName();
    boolean isAvailable();
    HookResult<Boolean> has(Player player, String permission);
}
