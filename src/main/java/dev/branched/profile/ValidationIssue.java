package dev.branched.profile;

/** One issue found while loading a species profile. */
public record ValidationIssue(Severity severity, String code, String path, String message) {
    public enum Severity {
        ERROR,
        WARNING
    }

    public static ValidationIssue error(String code, String path, String message) {
        return new ValidationIssue(Severity.ERROR, code, path, message);
    }

    public static ValidationIssue warning(String code, String path, String message) {
        return new ValidationIssue(Severity.WARNING, code, path, message);
    }

    @Override
    public String toString() {
        return severity + " [" + code + "] " + path + ": " + message;
    }
}
