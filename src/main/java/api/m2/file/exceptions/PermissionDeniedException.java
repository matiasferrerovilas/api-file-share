package api.m2.file.exceptions;

public final class PermissionDeniedException extends DomainException {
    public PermissionDeniedException(String message) {
        super(message);
    }
}
