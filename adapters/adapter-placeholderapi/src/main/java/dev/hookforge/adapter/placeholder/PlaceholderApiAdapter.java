package dev.hookforge.adapter.placeholder;

import dev.hookforge.api.placeholder.PlaceholderService;
import dev.hookforge.api.result.HookResult;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/** PlaceholderAPI bridge that never loads PAPI classes until the plugin is installed. */
public final class PlaceholderApiAdapter implements PlaceholderService {
    private final JavaPlugin plugin;
    private volatile boolean enabled = true;
    private volatile String preference = "auto";
    public PlaceholderApiAdapter(JavaPlugin plugin) { this.plugin = plugin; }
    public void configure(boolean enabled, String preference) {
        this.enabled = enabled;
        this.preference = preference == null ? "auto" : preference.toLowerCase(java.util.Locale.ROOT);
    }
    @Override public String providerName() { return "PlaceholderAPI"; }
    @Override public boolean isAvailable() {
        if (!enabled || "none".equals(preference)) return false;
        Plugin papi = Bukkit.getPluginManager().getPlugin("PlaceholderAPI");
        return papi != null && papi.isEnabled();
    }
    @Override public HookResult<String> expand(OfflinePlayer player, String input) {
        if (player == null) return HookResult.failure("Player không được null.");
        if (input == null) return HookResult.failure("Chuỗi đầu vào không được null.");
        if (!isAvailable()) return HookResult.failure("PlaceholderAPI chưa được bật hoặc integration đang tắt trong config.");
        try {
            Plugin papi = Bukkit.getPluginManager().getPlugin("PlaceholderAPI");
            Class<?> api = Class.forName("me.clip.placeholderapi.PlaceholderAPI", false, papi.getClass().getClassLoader());
            Method method = api.getMethod("setPlaceholders", OfflinePlayer.class, String.class);
            Object output = method.invoke(null, player, input);
            if (!(output instanceof String text)) return HookResult.failure("PlaceholderAPI trả về kết quả không hợp lệ.");
            return HookResult.success(text, "Đã mở rộng placeholder.");
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause();
            return HookResult.failure("PlaceholderAPI error: " + (cause == null ? ex.getMessage() : cause.getMessage()));
        } catch (ReflectiveOperationException | LinkageError ex) {
            return HookResult.failure("Không gọi được API PlaceholderAPI: " + ex.getMessage());
        }
    }
}
