package com.demo.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingCreatedEvent {
    private String eventId;
    private Integer bookingId;
    private Integer userId;
    private String userEmail;
    private Integer busId;
    private LocalDate journeyDate;
    private List<Integer> seatNumbers;
    private List<String> passengerNames;
    private Double totalAmount;
    private String status;
}