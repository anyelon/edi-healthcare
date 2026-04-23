package com.example.edi.common.edi.ack;

public enum TransactionSetStatus {
    ACCEPTED("A"),
    ACCEPTED_WITH_ERRORS("E"),
    REJECTED("R");

    private final String code;

    TransactionSetStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static TransactionSetStatus fromCode(String code) {
        return switch (code) {
            case "A" -> ACCEPTED;
            case "E" -> ACCEPTED_WITH_ERRORS;
            case "R" -> REJECTED;
            default -> throw new IllegalArgumentException("Unknown transaction set status code: " + code);
        };
    }
}
