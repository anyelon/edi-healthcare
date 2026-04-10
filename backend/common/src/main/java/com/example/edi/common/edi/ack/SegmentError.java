package com.example.edi.common.edi.ack;

public record SegmentError(
    String segmentId,
    int segmentPosition,
    String segmentErrorCode,
    int elementPosition,
    String elementErrorCode,
    String errorDescription
) {}
