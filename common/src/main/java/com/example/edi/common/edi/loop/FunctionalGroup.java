package com.example.edi.common.edi.loop;

public record FunctionalGroup(
    String senderId,
    String receiverId,
    String date,
    String time,
    String controlNumber
) {}
