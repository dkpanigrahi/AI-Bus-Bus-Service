package com.demo.dto;

import com.demo.enums.DeckLevel;
import com.demo.enums.PriceAdjustmentType;
import com.demo.enums.SeatType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatTypePricingRequest {
    SeatType seatType;                  // WINDOW | AISLE | MIDDLE
    DeckLevel deckLevel;                // LOWER | UPPER
    PriceAdjustmentType adjustmentType; // FLAT | PERCENTAGE
    int adjustmentValue;                // e.g. 50 or -100
    String label;
}