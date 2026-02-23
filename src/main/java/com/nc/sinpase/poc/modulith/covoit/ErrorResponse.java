package com.nc.sinpase.poc.modulith.covoit;

import java.util.Map;

public record ErrorResponse(
        String errorCode,
        String message,
        Map<String, Object> details,
        String traceId
) {
    public static ErrorResponse of(String errorCode, String message, String traceId) {
        return new ErrorResponse(errorCode, message, Map.of(), traceId);
    }

    public static ErrorResponse of(String errorCode, String message, Map<String, Object> details, String traceId) {
        return new ErrorResponse(errorCode, message, details, traceId);
    }
}
