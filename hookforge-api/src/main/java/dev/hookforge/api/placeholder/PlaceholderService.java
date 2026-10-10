package dev.hookforge.api.placeholder;

import dev.hookforge.api.result.HookResult;
import org.bukkit.OfflinePlayer;

/** Expands PlaceholderAPI tokens for a player. */
public interface PlaceholderService {
    String providerName();
    boolean isAvailable();
    HookResult<String> expand(OfflinePlayer player, String input);
}
