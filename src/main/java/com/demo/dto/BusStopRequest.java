package com.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusStopRequest {
 
    /** 1-based order — FE sends this, or service derives from list index */
    int stopSequence;
 
    String stopName;
    String city;
 
    /** HH:mm  —  null for first stop */
    String arrivalTime;
 
    /** HH:mm  —  null for last stop */
    String departureTime;
 
    /**
     * Cumulative base price from origin to this stop, in ₹.
     * Must be 0 for first stop.
     */
    int priceFromOrigin;
 
    String platformNo;
}