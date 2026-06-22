package com.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Core Bus entity.
 *
 * Stop sequencing and per-stop pricing live in BusStop.
 * Physical seat layout (rows, columns, deck) lives in SeatLayout → Seat.
 * Per-seat-type price adjustments live in SeatTypePricing.
 *
 * Admin sets everything; the service layer and FE consume the structured data.
 */
@Entity
@Table(name = "bus")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String uuid;

    @Column(unique = true, nullable = false)
    private String busNo;

    private String busName;

    private String coach;           // e.g. AC Seater, Sleeper, Semi-Sleeper

    private String busType;         // e.g. VOLVO, MERCEDES, ORDINARY

    /** True = runs daily; false = check specificDays */
    private boolean availableEveryDay;

    @ElementCollection
    @CollectionTable(name = "bus_specific_days", joinColumns = @JoinColumn(name = "bus_id"))
    @Column(name = "day")
    @Builder.Default
    private List<String> specificDays = new ArrayList<>();   // MONDAY, TUESDAY …

    /**
     * Ordered stop list for this bus.
     * First stop  = origin / startPlace.
     * Last stop   = destination.
     * Price at each stop = price from origin to that stop (admin-entered).
     */
    @OneToMany(mappedBy = "bus", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("stopSequence ASC")
    @Builder.Default
    private List<BusStop> stops = new ArrayList<>();


    /**
     * Defines the physical structure of the bus:
     * number of decks, rows per deck, column arrangement (2-2, 2-3, etc.)
     * The SeatLayout owns the individual Seat records.
     */
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_layout_id")
    private SeatLayout seatLayout;

    /**
     * Per-seat-type price adjustments configured by admin.
     * e.g. WINDOW → +₹50, UPPER_BERTH → -₹100
     */
    @OneToMany(mappedBy = "bus", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<SeatTypePricing> seatTypePricings = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    private Driver driver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conductor_id")
    private Conductor conductor;

    private Long busOperatorId;

    @Builder.Default
    private boolean isDeleted = false;

    @Builder.Default
    private boolean activeStatus = true;

    @CreationTimestamp
    private LocalDate createdDate;

    @UpdateTimestamp
    private LocalDate updatedDate;


    /** Returns the first stop (origin). Null-safe. */
    @Transient
    public BusStop getOriginStop() {
        return (stops == null || stops.isEmpty()) ? null : stops.get(0);
    }

    /** Returns the last stop (destination). Null-safe. */
    @Transient
    public BusStop getDestinationStop() {
        return (stops == null || stops.isEmpty()) ? null : stops.get(stops.size() - 1);
    }

    /** Total seats derived from SeatLayout (single source of truth). */
    @Transient
    public int getTotalSeats() {
        return (seatLayout == null || seatLayout.getSeats() == null)
                ? 0
                : seatLayout.getSeats().size();
    }
}