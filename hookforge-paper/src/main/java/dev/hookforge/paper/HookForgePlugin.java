package dev.hookforge.paper;

import dev.hookforge.adapter.permissions.BukkitPermissionAdapter;
import dev.hookforge.adapter.placeholder.PlaceholderApiAdapter;
import dev.hookforge.adapter.vault.VaultEconomyAdapter;
import dev.hookforge.api.HookForgeAPI;
import dev.hookforge.api.config.ConfigExplanation;
import dev.hookforge.api.diagnostic.Diagnostic;
import dev.hookforge.api.diagnostic.Diagnostic.Severity;
import dev.hookforge.core.HookForgeAPIImpl;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public final class HookForgePlugin extends JavaPlugin implements CommandExecutor, TabCompleter {
    private VaultEconomyAdapter economy;
    private PlaceholderApiAdapter placeholders;
    private BukkitPermissionAdapter permissions;
    private HookForgeAPI api;
    private Map<String, ConfigExplanation> explanations;
    private LanguageManager languages;

    @Override public void onEnable() {
        saveDefaultConfig();
        // Migrate existing installations whose config.yml predates language support.
        if (!getConfig().contains("language")) {
            getConfig().set("language", "vi");
            saveConfig();
        }
        this.languages = new LanguageManager(this);
        this.languages.load(getConfig().getString("language", "vi"));
        this.economy = new VaultEconomyAdapter(this);
        this.placeholders = new PlaceholderApiAdapter(this);
        this.permissions = new BukkitPermissionAdapter(this);
        configureAdapters();
        refreshApi();
        if (getCommand("hookforge") != null) {
            getCommand("hookforge").setExecutor(this);
            getCommand("hookforge").setTabCompleter(this);
        }
        getLogger().info(tr("plugin_enabled", "version", getDescription().getVersion()));
        List<String> errors = ConfigValidator.validate(getConfig(), key -> tr(key));
        if (!errors.isEmpty()) errors.forEach(error -> getLogger().warning(tr("log_config_error", "error", error)));
    }

    @Override public void onDisable() {
        getServer().getServicesManager().unregisterAll(this);
        getLogger().info(tr("plugin_disabled"));
    }

    private String tr(String key, Object... replacements) { return languages.text(key, replacements); }

    private void refreshApi() {
        this.explanations = buildExplanations();
        // Keep the same service instance so other plugins that cached the API keep working.
        if (api == null) {
            this.api = new HookForgeAPIImpl(getDescription().getVersion(), economy, placeholders, permissions,
                    this::buildDiagnostics, this::buildExplanations);
            getServer().getServicesManager().register(HookForgeAPI.class, api, this, ServicePriority.Normal);
        }
    }

    private void configureAdapters() {
        FileConfiguration c = getConfig();
        String economyProvider = c.getString("integrations.economy.provider", "auto");
        String placeholderProvider = c.getString("integrations.placeholders.provider", "auto");
        economy.configure(c.getBoolean("integrations.economy.enabled", true), economyProvider);
        placeholders.configure(c.getBoolean("integrations.placeholders.enabled", true), placeholderProvider);
        permissions.configure(c.getBoolean("integrations.permissions.enabled", true));
    }

    private List<Diagnostic> buildDiagnostics() {
        List<Diagnostic> out = new ArrayList<>();
        out.add(new Diagnostic("hookforge.loaded", Severity.OK, tr("diag_loaded_title"),
                tr("diag_loaded_details", "version", getDescription().getVersion(), "server", Bukkit.getVersion(), "java", System.getProperty("java.version")), tr("no_action_needed")));
        List<String> configErrors = ConfigValidator.validate(getConfig(), key -> tr(key));
        if (configErrors.isEmpty()) out.add(new Diagnostic("config.valid", Severity.OK, tr("diag_config_valid_title"), tr("diag_config_valid_details"), tr("no_action_needed")));
        else out.add(new Diagnostic("config.invalid", Severity.ERROR, tr("diag_config_invalid_title"), String.join(" | ", configErrors), tr("diag_config_invalid_solution")));

        if (getConfig().getBoolean("diagnostics.compatibility-check", true)) {
            diagnoseEconomy(out);
            diagnosePlaceholders(out);
            diagnosePermissions(out);
        } else {
            out.add(new Diagnostic("compatibility.skipped", Severity.INFO, tr("diag_compat_skipped_title"), tr("diag_compat_skipped_details"), tr("diag_compat_skipped_solution")));
        }
        return out;
    }

    private void diagnoseEconomy(List<Diagnostic> out) {
        if (!getConfig().getBoolean("integrations.economy.enabled", true) || "none".equalsIgnoreCase(getConfig().getString("integrations.economy.provider", "auto"))) {
            out.add(new Diagnostic("economy.disabled", Severity.INFO, tr("diag_economy_disabled_title"), tr("diag_economy_disabled_details"), tr("diag_economy_disabled_solution")));
            return;
        }
        var vault = Bukkit.getPluginManager().getPlugin("Vault");
        if (vault == null || !vault.isEnabled()) {
            out.add(new Diagnostic("economy.vault-missing", Severity.WARNING, tr("diag_vault_missing_title"), tr("diag_vault_missing_details"), tr("diag_vault_missing_solution")));
        } else if (!economy.isAvailable()) {
            out.add(new Diagnostic("economy.provider-missing", Severity.WARNING, tr("diag_economy_provider_missing_title"), tr("diag_economy_provider_missing_details"), tr("diag_economy_provider_missing_solution")));
        } else {
            out.add(new Diagnostic("economy.ready", Severity.OK, tr("diag_economy_ready_title"), tr("diag_provider_details", "provider", economy.providerName()), tr("no_action_needed")));
        }
    }

    private void diagnosePlaceholders(List<Diagnostic> out) {
        if (!getConfig().getBoolean("integrations.placeholders.enabled", true) || "none".equalsIgnoreCase(getConfig().getString("integrations.placeholders.provider", "auto"))) {
            out.add(new Diagnostic("placeholders.disabled", Severity.INFO, tr("diag_placeholders_disabled_title"), tr("diag_placeholders_disabled_details"), tr("diag_placeholders_disabled_solution")));
            return;
        }
        var papi = Bukkit.getPluginManager().getPlugin("PlaceholderAPI");
        if (papi == null || !papi.isEnabled()) {
            out.add(new Diagnostic("placeholders.papi-missing", Severity.WARNING, tr("diag_papi_missing_title"), tr("diag_papi_missing_details"), tr("diag_papi_missing_solution")));
        } else if (!placeholders.isAvailable()) {
            out.add(new Diagnostic("placeholders.unavailable", Severity.WARNING, tr("diag_papi_unavailable_title"), tr("diag_papi_unavailable_details"), tr("diag_papi_unavailable_solution")));
        } else {
            out.add(new Diagnostic("placeholders.ready", Severity.OK, tr("diag_papi_ready_title"), tr("diag_provider_details", "provider", "PlaceholderAPI"), tr("no_action_needed")));
        }
    }

    private void diagnosePermissions(List<Diagnostic> out) {
        if (!permissions.isAvailable()) {
            out.add(new Diagnostic("permissions.disabled", Severity.INFO, tr("diag_permissions_disabled_title"), tr("diag_permissions_disabled_details"), tr("diag_permissions_disabled_solution")));
            return;
        }
        var lp = Bukkit.getPluginManager().getPlugin("LuckPerms");
        String details = tr("diag_permissions_details");
        if (lp != null && lp.isEnabled()) details += tr("diag_permissions_luckperms");
        out.add(new Diagnostic("permissions.ready", Severity.OK, tr("diag_permissions_ready_title"), details, tr("no_action_needed")));
    }

    private Map<String, ConfigExplanation> buildExplanations() {
        Map<String, ConfigExplanation> m = new LinkedHashMap<>();
        add(m, "profile", "explain_profile");
        add(m, "integrations.economy.enabled", "explain_economy_enabled");
        add(m, "integrations.economy.provider", "explain_economy_provider");
        add(m, "integrations.placeholders.enabled", "explain_placeholders_enabled");
        add(m, "integrations.placeholders.provider", "explain_placeholders_provider");
        add(m, "integrations.permissions.enabled", "explain_permissions_enabled");
        add(m, "diagnostics.level", "explain_diagnostics_level");
        add(m, "diagnostics.compatibility-check", "explain_compat");
        add(m, "diagnostics.write-report", "explain_write_report");
        add(m, "diagnostics.report-file", "explain_report_file");
        return Map.copyOf(m);
    }

    private void add(Map<String, ConfigExplanation> map, String key, String translationPrefix) {
        map.put(key, new ConfigExplanation(key, tr(translationPrefix + "_desc"), tr(translationPrefix + "_example"), tr(translationPrefix + "_note")));
    }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) { help(sender); return true; }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "doctor" -> doctor(sender);
            case "explain" -> explain(sender, args);
            case "test" -> test(sender, args);
            case "reload" -> reloadSafe(sender);
            case "setup" -> setup(sender, args);
            case "language", "lang" -> setLanguage(sender, args);
            default -> message(sender, tr("unknown_command"));
        }
        return true;
    }

    private void help(CommandSender sender) {
        for (String key : List.of("help_title", "help_setup", "help_doctor", "help_explain", "help_test", "help_reload", "help_language")) message(sender, tr(key));
    }

    private void doctor(CommandSender sender) {
        message(sender, tr("doctor_title", "version", getDescription().getVersion()));
        List<Diagnostic> diagnostics = api.diagnostics();
        String diagnosticLevel = getConfig().getString("diagnostics.level", "normal");
        for (Diagnostic d : diagnostics) {
            if ("info".equalsIgnoreCase(diagnosticLevel) && d.severity() != Severity.WARNING && d.severity() != Severity.ERROR) continue;
            String color = switch (d.severity()) { case OK -> "&a"; case INFO -> "&b"; case WARNING -> "&e"; case ERROR -> "&c"; };
            String severity = switch (d.severity()) {
                case OK -> tr("severity_ok"); case INFO -> tr("severity_info");
                case WARNING -> tr("severity_warning"); case ERROR -> tr("severity_error");
            };
            message(sender, color + "[" + severity + "] &f" + d.title());
            if ("debug".equalsIgnoreCase(getConfig().getString("diagnostics.level", "normal"))) message(sender, "&7  " + d.details());
            if (d.severity() == Severity.WARNING || d.severity() == Severity.ERROR) message(sender, tr("doctor_solution", "solution", d.solution()));
        }
        if (getConfig().getBoolean("diagnostics.write-report", true)) {
            try {
                Path folder = getDataFolder().toPath().normalize();
                Path file = folder.resolve(getConfig().getString("diagnostics.report-file", "diagnostics.json")).normalize();
                if (!folder.equals(file.getParent())) throw new IOException("Report file must stay inside the plugin data folder.");
                Files.writeString(file, toJson(diagnostics), StandardCharsets.UTF_8);
                message(sender, tr("report_written", "file", file.getFileName()));
            } catch (IOException | RuntimeException ex) {
                message(sender, tr("report_failed", "error", ex.getMessage()));
                getLogger().warning(tr("log_report_failure", "error", ex.getMessage()));
            }
        }
    }

    private String toJson(List<Diagnostic> diagnostics) {
        StringBuilder json = new StringBuilder("{\n  \"plugin\": \"HookForge\",\n  \"version\": \"")
                .append(escape(getDescription().getVersion())).append("\",\n  \"server\": \"")
                .append(escape(Bukkit.getVersion())).append("\",\n  \"diagnostics\": [\n");
        for (int i = 0; i < diagnostics.size(); i++) {
            Diagnostic d = diagnostics.get(i);
            json.append("    {\"id\":\"").append(escape(d.id())).append("\",\"severity\":\"")
                    .append(d.severity()).append("\",\"title\":\"").append(escape(d.title()))
                    .append("\",\"details\":\"").append(escape(d.details())).append("\",\"solution\":\"")
                    .append(escape(d.solution())).append("\"}");
            if (i + 1 < diagnostics.size()) json.append(',');
            json.append('\n');
        }
        return json.append("  ]\n}\n").toString();
    }

    private String escape(String input) {
        return input.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    private void explain(CommandSender sender, String[] args) {
        if (args.length < 2) {
            message(sender, tr("explain_usage"));
            message(sender, tr("explain_example_hint"));
            message(sender, tr("explain_keys", "keys", String.join(", ", explanations.keySet())));
            return;
        }
        String key = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length)).toLowerCase(Locale.ROOT);
        ConfigExplanation e = api.explain(key).orElse(null);
        if (e == null) { message(sender, tr("explain_unknown", "key", key)); message(sender, tr("explain_unknown_hint")); return; }
        message(sender, tr("explain_title", "key", e.key()));
        message(sender, tr("explain_description", "description", e.description()));
        message(sender, tr("explain_example", "example", e.example()));
        message(sender, tr("explain_note", "note", e.note()));
    }

    private void test(CommandSender sender, String[] args) {
        if (args.length < 2) { message(sender, tr("test_usage")); return; }
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "economy" -> {
                if (!economy.isAvailable()) message(sender, tr("test_economy_unavailable"));
                else if (sender instanceof Player player) {
                    var result = economy.balance(player);
                    if (result.isSuccess()) message(sender, tr("test_economy_ok", "balance", result.value(), "provider", economy.providerName()));
                    else message(sender, tr("test_economy_failed", "error", result.message()));
                } else message(sender, tr("test_economy_console", "provider", economy.providerName()));
            }
            case "placeholders" -> {
                if (!placeholders.isAvailable()) message(sender, tr("test_placeholders_unavailable"));
                else if (sender instanceof Player player) {
                    var result = placeholders.expand(player, "%player_name%");
                    if (result.isSuccess()) message(sender, tr("test_placeholders_ok", "value", result.value()));
                    else message(sender, tr("test_placeholders_failed", "error", result.message()));
                } else message(sender, tr("test_placeholders_console"));
            }
            case "permissions" -> {
                if (!permissions.isAvailable()) message(sender, tr("test_permissions_unavailable"));
                else if (sender instanceof Player player) {
                    var result = permissions.has(player, "hookforge.admin");
                    message(sender, result.isSuccess()
                            ? tr("test_permissions_ok", "value", result.value(), "provider", permissions.providerName())
                            : tr("test_permissions_failed", "error", result.message()));
                } else message(sender, tr("test_permissions_console", "provider", permissions.providerName()));
            }
            default -> message(sender, tr("test_unknown_module"));
        }
    }

    private void reloadSafe(CommandSender sender) {
        Path configPath = getDataFolder().toPath().resolve("config.yml");
        FileConfiguration candidate = YamlConfiguration.loadConfiguration(configPath.toFile());
        List<String> errors = ConfigValidator.validate(candidate, key -> tr(key));
        if (!errors.isEmpty()) {
            message(sender, tr("reload_invalid"));
            errors.forEach(e -> message(sender, "&c- " + e));
            return;
        }
        reloadConfig();
        languages.load(getConfig().getString("language", "vi"));
        configureAdapters();
        refreshApi();
        message(sender, tr("reload_success"));
    }

    private void setLanguage(CommandSender sender, String[] args) {
        if (args.length != 2) { message(sender, tr("language_usage")); return; }
        String language = args[1].trim().toLowerCase(Locale.ROOT);
        if (!languages.isSupported(language)) { message(sender, tr("language_invalid")); return; }
        getConfig().set("language", language);
        saveConfig();
        languages.load(language);
        refreshApi();
        message(sender, tr("language_changed", "language", languages.displayName()));
    }

    private void setup(CommandSender sender, String[] args) {
        if (args.length < 2 || !(args[1].equalsIgnoreCase("beginner") || args[1].equalsIgnoreCase("advanced"))) {
            message(sender, tr("setup_usage")); return;
        }
        String preset = args[1].toLowerCase(Locale.ROOT);
        String keepLanguage = languages.language();
        Path folder = getDataFolder().toPath();
        Path config = folder.resolve("config.yml");
        try {
            Files.createDirectories(folder);
            if (Files.exists(config)) Files.copy(config, folder.resolve("config.yml.bak"), StandardCopyOption.REPLACE_EXISTING);
            try (InputStream input = getResource("presets/" + preset + ".yml")) {
                if (input == null) throw new IOException(tr("setup_missing_resource", "preset", preset));
                Files.copy(input, config, StandardCopyOption.REPLACE_EXISTING);
            }
            FileConfiguration presetConfig = YamlConfiguration.loadConfiguration(config.toFile());
            presetConfig.set("language", keepLanguage);
            presetConfig.save(config.toFile());
            reloadConfig();
            languages.load(getConfig().getString("language", keepLanguage));
            List<String> errors = ConfigValidator.validate(getConfig(), key -> tr(key));
            if (!errors.isEmpty()) {
                message(sender, tr("setup_written_invalid", "errors", String.join(" | ", errors)));
                return;
            }
            configureAdapters();
            refreshApi();
            message(sender, tr("setup_applied", "preset", preset));
        } catch (IOException ex) {
            message(sender, tr("setup_failed", "error", ex.getMessage()));
            getLogger().warning(tr("log_setup_failure", "error", ex.getMessage()));
        }
    }

    private void message(CommandSender sender, String text) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&8[&bHookForge&8] &r" + text));
    }

    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> options = new ArrayList<>();
        if (args.length == 1) options.addAll(List.of("help", "setup", "doctor", "explain", "test", "reload", "language"));
        else if (args.length == 2 && args[0].equalsIgnoreCase("setup")) options.addAll(List.of("beginner", "advanced"));
        else if (args.length == 2 && args[0].equalsIgnoreCase("test")) options.addAll(List.of("economy", "placeholders", "permissions"));
        else if (args.length == 2 && args[0].equalsIgnoreCase("language")) options.addAll(List.of("vi", "en", "cn"));
        else if (args.length == 2 && args[0].equalsIgnoreCase("explain")) options.addAll(explanations.keySet());
        String partial = args[args.length - 1].toLowerCase(Locale.ROOT);
        return options.stream().filter(s -> s.toLowerCase(Locale.ROOT).startsWith(partial)).sorted().collect(Collectors.toList());
    }
}
