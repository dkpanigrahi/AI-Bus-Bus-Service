package com.demo.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingCancelledEvent {
    private String eventId;
    private String reason;
    private List<Integer> bookingIds;
    private Integer userId;
}