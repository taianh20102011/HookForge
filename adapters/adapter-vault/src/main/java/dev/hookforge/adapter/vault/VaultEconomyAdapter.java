package dev.hookforge.adapter.vault;

import dev.hookforge.api.economy.EconomyService;
import dev.hookforge.api.result.HookResult;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/** Vault adapter using reflection so Vault remains an optional runtime dependency. */
public final class VaultEconomyAdapter implements EconomyService {
    private final JavaPlugin plugin;
    private volatile boolean enabled = true;
    private volatile String preference = "auto";

    public VaultEconomyAdapter(JavaPlugin plugin) { this.plugin = plugin; }
    public void configure(boolean enabled, String preference) {
        this.enabled = enabled;
        this.preference = preference == null ? "auto" : preference.toLowerCase(java.util.Locale.ROOT);
    }

    @Override public String providerName() { return "Vault Economy"; }

    @Override public boolean isAvailable() {
        if (!enabled || "none".equals(preference)) return false;
        try { return provider() != null; }
        catch (ReflectiveOperationException | LinkageError ex) { return false; }
    }

    @Override public HookResult<Double> balance(OfflinePlayer player) {
        if (player == null) return HookResult.failure("Player không được null.");
        try {
            Object provider = requireProvider();
            Object result = invokePlayer(provider, "getBalance", player);
            if (!(result instanceof Number number)) return HookResult.failure("Vault trả về số dư không hợp lệ.");
            return HookResult.success(number.doubleValue(), "Đã đọc số dư qua Vault.");
        } catch (ReflectiveOperationException | LinkageError ex) { return HookResult.failure(explain(ex)); }
    }

    @Override public HookResult<Boolean> has(OfflinePlayer player, double amount) {
        if (player == null) return HookResult.failure("Player không được null.");
        if (!validAmount(amount)) return HookResult.failure("Số tiền phải hữu hạn và không âm.");
        try {
            Object result = invokePlayerAmount(requireProvider(), "has", player, amount);
            if (!(result instanceof Boolean value)) return HookResult.failure("Vault trả về kết quả kiểm tra không hợp lệ.");
            return HookResult.success(value, "Đã kiểm tra số dư qua Vault.");
        } catch (ReflectiveOperationException | LinkageError ex) { return HookResult.failure(explain(ex)); }
    }

    @Override public HookResult<Double> deposit(OfflinePlayer player, double amount) {
        return transact(player, amount, "depositPlayer");
    }

    @Override public HookResult<Double> withdraw(OfflinePlayer player, double amount) {
        return transact(player, amount, "withdrawPlayer");
    }

    private HookResult<Double> transact(OfflinePlayer player, double amount, String method) {
        if (player == null) return HookResult.failure("Player không được null.");
        if (!Double.isFinite(amount) || amount <= 0) return HookResult.failure("Số tiền phải lớn hơn 0 và hữu hạn.");
        try {
            Object response = invokePlayerAmount(requireProvider(), method, player, amount);
            if (response == null) return HookResult.failure("Vault không trả về EconomyResponse.");
            Method transactionSuccess = response.getClass().getMethod("transactionSuccess");
            Object successful = transactionSuccess.invoke(response);
            if (!(successful instanceof Boolean ok) || !ok) {
                String reason = "Giao dịch bị provider từ chối.";
                try { Object error = response.getClass().getMethod("errorMessage").invoke(response); if (error != null && !error.toString().isBlank()) reason += " " + error; }
                catch (ReflectiveOperationException ignored) { }
                return HookResult.failure(reason);
            }
            return balance(player);
        } catch (ReflectiveOperationException | LinkageError ex) { return HookResult.failure(explain(ex)); }
    }

    private Object requireProvider() throws ReflectiveOperationException {
        Object result = provider();
        if (result == null) throw new ReflectiveOperationException("Không tìm thấy Vault Economy provider. Hãy cài Vault và plugin kinh tế tương thích.");
        return result;
    }

    private Object provider() throws ReflectiveOperationException {
        Plugin vault = Bukkit.getPluginManager().getPlugin("Vault");
        if (vault == null || !vault.isEnabled()) return null;
        ClassLoader loader = vault.getClass().getClassLoader();
        Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy", false, loader);
        @SuppressWarnings({"rawtypes", "unchecked"})
        RegisteredServiceProvider<?> registration = Bukkit.getServicesManager().getRegistration((Class) economyClass);
        return registration == null ? null : registration.getProvider();
    }

    private Object invokePlayer(Object provider, String name, OfflinePlayer player) throws ReflectiveOperationException {
        try { return provider.getClass().getMethod(name, OfflinePlayer.class).invoke(provider, player); }
        catch (NoSuchMethodException e) {
            if (player.getName() == null) throw new NoSuchMethodException(name + " cần player có tên, nhưng tên đang null");
            return provider.getClass().getMethod(name, String.class).invoke(provider, player.getName());
        } catch (InvocationTargetException e) { throw unwrap(e); }
    }

    private Object invokePlayerAmount(Object provider, String name, OfflinePlayer player, double amount) throws ReflectiveOperationException {
        try { return provider.getClass().getMethod(name, OfflinePlayer.class, double.class).invoke(provider, player, amount); }
        catch (NoSuchMethodException e) {
            if (player.getName() == null) throw new NoSuchMethodException(name + " cần player có tên, nhưng tên đang null");
            return provider.getClass().getMethod(name, String.class, double.class).invoke(provider, player.getName(), amount);
        } catch (InvocationTargetException e) { throw unwrap(e); }
    }

    private ReflectiveOperationException unwrap(InvocationTargetException ex) {
        Throwable cause = ex.getCause();
        return new ReflectiveOperationException(cause == null ? ex.getMessage() : cause.getMessage(), cause == null ? ex : cause);
    }
    private boolean validAmount(double amount) { return Double.isFinite(amount) && amount >= 0; }
    private String explain(Throwable ex) {
        Throwable cause = ex;
        while ((cause instanceof InvocationTargetException || cause instanceof ReflectiveOperationException) && cause.getCause() != null) cause = cause.getCause();
        String message = cause.getMessage();
        return "Vault Economy error: " + (message == null || message.isBlank() ? cause.getClass().getSimpleName() : message);
    }
}
