package com.demo.dto.response;// ─── BusDto.java ──────────────────────────────────────────────────────────────
// Full bus detail — used on admin view and individual bus detail page.

import com.demo.dto.ConductorDto;
import com.demo.dto.DriverDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusDto {
    int id;
    String busNo;
    String busName;
    String coach;
    String busType;
    boolean availableEveryDay;
    List<String> specificDays;
    List<BusStopDto> stops;
    SeatLayoutDto seatLayout;
    List<SeatTypePricingDto> seatTypePricings;
    DriverDto driver;
    ConductorDto conductor;
    boolean activeStatus;
}