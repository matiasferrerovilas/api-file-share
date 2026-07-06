package com.api.file.share.exceptions;

public final class PermissionDeniedException extends DomainException {
    public PermissionDeniedException(String message) {
        super(message);
    }
}
