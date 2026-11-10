package dev.characteratlas.archipelago.shared.application;

public final class UseCaseException extends RuntimeException {
    public enum Kind { INVALID_INPUT, NOT_FOUND }
    private final Kind kind;

    private UseCaseException(Kind kind, String message) {
        super(message);
        this.kind = kind;
    }

    public Kind kind() { return kind; }

    public static UseCaseException invalid(String message) {
        return new UseCaseException(Kind.INVALID_INPUT, message);
    }

    public static UseCaseException notFound() {
        return new UseCaseException(Kind.NOT_FOUND, null);
    }
}
