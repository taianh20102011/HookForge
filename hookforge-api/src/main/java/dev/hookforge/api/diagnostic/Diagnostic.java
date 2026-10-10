package dev.hookforge.api.diagnostic;

/** One actionable diagnostic item. */
public record Diagnostic(String id, Severity severity, String title, String details, String solution) {
    public enum Severity { OK, INFO, WARNING, ERROR }
}
