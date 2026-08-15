package com.example.bai3;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class BookingController {
    private final BookingService bookingService;

    @PostMapping
    public BookingExtraction extraction(@RequestBody EmailBookingRequest request) {
        return bookingService.extraction(request.emailContent());
    }
}
