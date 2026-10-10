package dev.hookforge.core;

import dev.hookforge.api.HookForgeAPI;
import dev.hookforge.api.config.ConfigExplanation;
import dev.hookforge.api.diagnostic.Diagnostic;
import dev.hookforge.api.economy.EconomyService;
import dev.hookforge.api.permission.PermissionService;
import dev.hookforge.api.placeholder.PlaceholderService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/** Default API implementation. Diagnostics are generated on demand so config changes are reflected. */
public final class HookForgeAPIImpl implements HookForgeAPI {
    private final String version;
    private final EconomyService economy;
    private final PlaceholderService placeholders;
    private final PermissionService permissions;
    private final Supplier<List<Diagnostic>> diagnosticSupplier;
    private final Supplier<Map<String, ConfigExplanation>> explanationSupplier;

    public HookForgeAPIImpl(String version, EconomyService economy, PlaceholderService placeholders,
                            PermissionService permissions, Supplier<List<Diagnostic>> diagnosticSupplier,
                            Map<String, ConfigExplanation> explanations) {
        this(version, economy, placeholders, permissions, diagnosticSupplier, () -> Map.copyOf(explanations));
    }

    public HookForgeAPIImpl(String version, EconomyService economy, PlaceholderService placeholders,
                            PermissionService permissions, Supplier<List<Diagnostic>> diagnosticSupplier,
                            Supplier<Map<String, ConfigExplanation>> explanationSupplier) {
        this.version = version;
        this.economy = economy;
        this.placeholders = placeholders;
        this.permissions = permissions;
        this.diagnosticSupplier = diagnosticSupplier;
        this.explanationSupplier = explanationSupplier;
    }

    @Override public String version() { return version; }
    @Override public EconomyService economy() { return economy; }
    @Override public PlaceholderService placeholders() { return placeholders; }
    @Override public PermissionService permissions() { return permissions; }
    @Override public List<Diagnostic> diagnostics() { return List.copyOf(diagnosticSupplier.get()); }
    @Override public Optional<ConfigExplanation> explain(String key) {
        if (key == null) return Optional.empty();
        return Optional.ofNullable(explanationSupplier.get().get(key.trim().toLowerCase(java.util.Locale.ROOT)));
    }
}
