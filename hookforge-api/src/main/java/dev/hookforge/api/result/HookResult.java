package dev.hookforge.api.result;

import java.util.Optional;

/** Explicit success/failure value used to avoid hiding integration errors. */
public final class HookResult<T> {
    private final boolean success;
    private final T value;
    private final String message;

    private HookResult(boolean success, T value, String message) {
        this.success = success;
        this.value = value;
        this.message = message == null ? "" : message;
    }

    public static <T> HookResult<T> success(T value, String message) {
        return new HookResult<>(true, value, message);
    }

    public static <T> HookResult<T> failure(String message) {
        return new HookResult<>(false, null, message);
    }

    public boolean isSuccess() { return success; }
    /** Returns null on failure; prefer optionalValue() when null-safety matters. */
    public T value() { return value; }
    public Optional<T> optionalValue() { return Optional.ofNullable(value); }
    /** Human-readable explanation suitable for logs or admin feedback. */
    public String message() { return message; }

    @Override public String toString() {
        return "HookResult{success=" + success + ", value=" + value + ", message='" + message + "'}";
    }
}
