package com.example.edi.common.dto;

public record ErrorResponse(
        String error,
        String message,
        String entityType,
        String entityId
) {

    public static ErrorResponse notFound(String entityType, String entityId) {
        return new ErrorResponse("NOT_FOUND", entityType + " not found: " + entityId, entityType, entityId);
    }

    public static ErrorResponse badRequest(String message) {
        return new ErrorResponse("BAD_REQUEST", message, null, null);
    }
}
