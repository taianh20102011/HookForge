package dev.hookforge.paper;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

/** Loads editable language files and falls back to Vietnamese for missing keys. */
final class LanguageManager {
    private static final String[] LANGUAGES = {"vi", "en", "cn"};
    private final JavaPlugin plugin;
    private YamlConfiguration selected;
    private YamlConfiguration fallback;
    private String language = "vi";

    LanguageManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    void load(String requested) {
        String normalized = requested == null ? "vi" : requested.trim().toLowerCase(Locale.ROOT);
        if (!isSupported(normalized)) normalized = "vi";
        this.language = normalized;
        Path directory = plugin.getDataFolder().toPath().resolve("lang");
        try {
            Files.createDirectories(directory);
            for (String code : LANGUAGES) {
                Path file = directory.resolve("messages_" + code + ".yml");
                if (Files.notExists(file)) {
                    try (InputStream input = plugin.getResource("lang/messages_" + code + ".yml")) {
                        if (input == null) throw new IOException("Missing bundled language resource: " + code);
                        Files.copy(input, file, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
            this.selected = YamlConfiguration.loadConfiguration(directory.resolve("messages_" + language + ".yml").toFile());
            this.fallback = YamlConfiguration.loadConfiguration(directory.resolve("messages_vi.yml").toFile());
        } catch (IOException ex) {
            plugin.getLogger().severe("Could not load language files: " + ex.getMessage());
            this.selected = new YamlConfiguration();
            this.fallback = new YamlConfiguration();
        }
    }

    boolean isSupported(String value) {
        if (value == null) return false;
        for (String language : LANGUAGES) if (language.equalsIgnoreCase(value)) return true;
        return false;
    }

    String language() { return language; }

    String displayName() {
        return switch (language) {
            case "en" -> "English";
            case "cn" -> "简体中文";
            default -> "Tiếng Việt";
        };
    }

    String text(String key, Object... replacements) {
        String value = selected == null ? null : selected.getString(key);
        if (value == null && fallback != null) value = fallback.getString(key);
        if (value == null) value = key;
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            String placeholder = "{" + replacements[i] + "}";
            value = value.replace(placeholder, String.valueOf(replacements[i + 1]));
        }
        return value;
    }
}
