package com.demo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BusSearchResponseDto {
    int id;
    String busNo;
    String busName;
    String coach;
    String busType;
 
    // Origin and destination derived from first/last stops
    String originCity;
    String destinationCity;
    String originStopName;
    String destinationStopName;
 
    // Departure time from origin stop
    LocalDateTime departureTime;
 
    // Arrival time at destination stop
    LocalDateTime arrivalTime;
 
    // Base fare for the searched route segment (computed from stop prices)
    int basePrice;
 
    int totalSeats;
    int availableSeats;   // populated by BookingServiceClient
 
    String driverName;
    String conductorName;

    int originCityStopSequencyId;
    int destCityStopSequencyId;
}