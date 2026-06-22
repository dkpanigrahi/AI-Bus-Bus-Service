package com.demo.dto;

import lombok.*;
 
import java.util.List;
 
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusCreateRequest {
 
    // ── Core bus fields ───────────────────────────────────────────────────────
    String busNo;
    String busName;
    String coach;        // AC Seater, Sleeper, etc.
    String busType;      // VOLVO, MERCEDES, ORDINARY
 
    boolean availableEveryDay;
    List<String> specificDays;   // ["MONDAY","WEDNESDAY"] if not every day
 
    Integer driverId;
    Integer conductorId;
    Long busOperatorId;
 
    // ── Stop list (admin enters all stops in order) ───────────────────────────
    List<BusStopRequest> stops;
 
    // ── Seat layout config (admin picks arrangement and row counts) ───────────
    SeatLayoutRequest seatLayout;
 
    // ── Per-seat-type price adjustments ──────────────────────────────────────
    List<SeatTypePricingRequest> seatTypePricings;
}