package dev.hookforge.api;

import dev.hookforge.api.config.ConfigExplanation;
import dev.hookforge.api.diagnostic.Diagnostic;
import dev.hookforge.api.economy.EconomyService;
import dev.hookforge.api.permission.PermissionService;
import dev.hookforge.api.placeholder.PlaceholderService;
import java.util.List;
import java.util.Optional;

/** Public entry point exposed through Bukkit's ServicesManager. */
public interface HookForgeAPI {
    /** Version of the running HookForge plugin. */
    String version();
    /** Economy bridge (Vault-backed in the MVP). */
    EconomyService economy();
    /** Placeholder expansion bridge (PlaceholderAPI-backed in the MVP). */
    PlaceholderService placeholders();
    /** Permission check bridge using Bukkit's resolved permission system. */
    PermissionService permissions();
    /** Creates a fresh diagnostic snapshot. */
    List<Diagnostic> diagnostics();
    /** Finds an explanation for a config path such as integrations.economy.enabled. */
    Optional<ConfigExplanation> explain(String key);
}
