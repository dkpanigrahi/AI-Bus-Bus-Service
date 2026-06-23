package com.demo.dto;

import lombok.*;
 
import java.util.List;
 
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusCreateRequest {
 
    String busNo;
    String busName;
    String coach;        // AC Seater, Sleeper, etc.
    String busType;      // VOLVO, MERCEDES, ORDINARY
 
    boolean availableEveryDay;
    List<String> specificDays;   // ["MONDAY","WEDNESDAY"] if not every day
 
    Integer driverId;
    Integer conductorId;
    Long busOperatorId;
 
    List<BusStopRequest> stops;
 
    SeatLayoutRequest seatLayout;
 
    List<SeatTypePricingRequest> seatTypePricings;
}