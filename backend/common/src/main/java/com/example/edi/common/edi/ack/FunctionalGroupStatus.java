package com.example.edi.common.edi.ack;

public enum FunctionalGroupStatus {
    ACCEPTED("A"),
    ACCEPTED_WITH_ERRORS("E"),
    REJECTED("R"),
    PARTIALLY_ACCEPTED("P");

    private final String code;

    FunctionalGroupStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static FunctionalGroupStatus fromCode(String code) {
        return switch (code) {
            case "A" -> ACCEPTED;
            case "E" -> ACCEPTED_WITH_ERRORS;
            case "R" -> REJECTED;
            case "P" -> PARTIALLY_ACCEPTED;
            default -> throw new IllegalArgumentException("Unknown functional group status code: " + code);
        };
    }
}
