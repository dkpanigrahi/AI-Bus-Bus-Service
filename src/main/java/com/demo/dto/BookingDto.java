package com.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingDto {
    private int id;
    private Integer seatNo;
    private String passengerName;
    private LocalDate bookingDate;
    private boolean booked;
    private boolean inProcess;
    private LocalDateTime expirationTime;
    private UserDto user;
    private BusDto bus;
}