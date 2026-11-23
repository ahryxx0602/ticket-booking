package com.ticket.shared.exception;

public class EntityNotFoundException extends BusinessException {
    public EntityNotFoundException(String entityName, Object id) {
        super("NOT_FOUND", String.format("%s with id '%s' not found", entityName, id));
    }
}
