package com.example.bai3;

public record BookingExtraction(
        String guestName,
        String checkInDate,
        int durationNights,
        String roomType
) {

}
