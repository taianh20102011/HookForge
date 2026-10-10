package dev.hookforge.paper;

import org.bukkit.configuration.file.FileConfiguration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/** Small explicit validator; never silently accepts unsupported values. */
final class ConfigValidator {
    private ConfigValidator() { }

    static List<String> validate(FileConfiguration config, Function<String, String> translate) {
        List<String> errors = new ArrayList<>();
        check(config.getString("language"), List.of("vi", "en", "cn"), "language", errors, translate);
        check(config.getString("profile"), List.of("beginner", "advanced"), "profile", errors, translate);
        check(config.getString("integrations.economy.provider"), List.of("auto", "vault", "none"), "integrations.economy.provider", errors, translate);
        check(config.getString("integrations.placeholders.provider"), List.of("auto", "placeholderapi", "none"), "integrations.placeholders.provider", errors, translate);
        check(config.getString("diagnostics.level"), List.of("info", "normal", "debug"), "diagnostics.level", errors, translate);
        String reportFile = config.getString("diagnostics.report-file", "diagnostics.json");
        if (reportFile == null || reportFile.isBlank() || reportFile.contains("/") || reportFile.contains("\\") || reportFile.equals(".") || reportFile.equals("..")) {
            errors.add(translate.apply("config_error_report_file"));
        }
        return errors;
    }

    private static void check(String value, List<String> allowed, String key, List<String> errors,
                              Function<String, String> translate) {
        if (value == null || !allowed.contains(value.toLowerCase(Locale.ROOT))) {
            String template = translate.apply("config_error_invalid");
            errors.add(template.replace("{key}", key).replace("{allowed}", String.join(", ", allowed)));
        }
    }
}
