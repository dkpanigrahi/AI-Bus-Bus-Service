package com.demo.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Slim bus projection exposed ONLY to Booking Service (internal, service-to-service).
 * Do NOT add fields here unless Booking Service actually needs them —
 * this is intentionally a subset of BusDto.
 */
@Data
@Builder
public class BusDetailDto {

    private Integer id;
    private String busNo;
    private Integer totalSeats;

    /** Ordered stop list. First = origin, last = destination. */
    private List<StopDto> stops;

    private SeatLayoutDto seatLayout;

    private List<SeatTypePricingDto> seatTypePricings;

    @Data
    @Builder
    public static class StopDto {
        private Integer stopSequence;
        private String stopName;
        /** Cumulative price from origin to this stop. */
        private Integer priceFromOrigin;
    }

    @Data
    @Builder
    public static class SeatLayoutDto {
        private List<SeatDto> seats;
    }

    @Data
    @Builder
    public static class SeatDto {
        private Integer seatNumber;
        private String seatLabel;
        private String seatType;
        private String deckLevel;
        private boolean isSleeper;
        private boolean isActive;
    }

    @Data
    @Builder
    public static class SeatTypePricingDto {
        private String seatType;
        private String deckLevel;
        private String adjustmentType;
        private int adjustmentValue;
    }
}
