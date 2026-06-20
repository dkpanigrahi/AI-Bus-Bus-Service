package com.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "bus")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Bus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(unique = true)
    private String busNo;

    private String startPlace;

    private String destination;

    private String departureTime;

    private boolean availableEveryDay;

    private String coach;

    @ElementCollection
    @CollectionTable(name = "bus_specific_days", joinColumns = @JoinColumn(name = "bus_id"))
    @Column(name = "day")
    private List<String> specificDays;

    private int totalSeats;

    private int ticketPrice;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "driver_id")
    private Driver driver;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "conductor_id")
    private Conductor conductor;
}