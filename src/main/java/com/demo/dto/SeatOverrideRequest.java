package com.demo.dto;

import com.demo.enums.SeatType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatOverrideRequest {
    int seatNumber;          // which seat to override (by sequential number)
    String seatLabel;        // custom label, e.g. "VIP-1"
    SeatType seatType;       // override derived type
    boolean isActive;        // false = disabled seat
    String adminNote;
}