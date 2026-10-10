package dev.hookforge.api.economy;

import dev.hookforge.api.result.HookResult;
import org.bukkit.OfflinePlayer;

/** Economy operations. Mutating operations should be called on the server main thread. */
public interface EconomyService {
    String providerName();
    boolean isAvailable();
    HookResult<Double> balance(OfflinePlayer player);
    HookResult<Boolean> has(OfflinePlayer player, double amount);
    HookResult<Double> deposit(OfflinePlayer player, double amount);
    HookResult<Double> withdraw(OfflinePlayer player, double amount);
}
