package com.demo.client;

import com.demo.dto.ApiResponse;
import com.demo.dto.SeatAvailabilityResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "booking-service",
        fallback = BookingServiceClient.BookingServiceFallback.class
)
public interface BookingServiceClient {

    @GetMapping("/api/bookings/seats/availability")
    ResponseEntity<ApiResponse<SeatAvailabilityResponse>> getSeatAvailability(
            @RequestParam("busId") int busId,
            @RequestParam("date") String date,
            @RequestParam("boardingStopSequence") int boardingStopSequence,
            @RequestParam("alightingStopSequence") int alightingStopSequence);


    @Component
    @Slf4j
    class BookingServiceFallback implements BookingServiceClient {

        @Override
        public ResponseEntity<ApiResponse<SeatAvailabilityResponse>> getSeatAvailability(int busId, String date,
                                      int boardingStopSequence, int alightingStopSequence) {
            log.warn("Fallback triggered for getSeatAvailability - busId: {}, date: {}", busId, date);
            return ResponseEntity.ok(ApiResponse.error("Seat availability service is temporarily unavailable"));
        }

    }
}