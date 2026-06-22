package com.demo.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Represents one stop on a bus route.
 *
 * stopSequence  : 1-based ordering (1 = origin, N = destination)
 * priceFromOrigin : admin-entered base fare from stop-1 to THIS stop.
 *                   Final ticket price = priceFromOrigin + seat-type adjustment.
 *
 * Example for a bus Chennai → Vellore → Bangalore:
 *   seq=1  Chennai       arrivalTime=null  priceFromOrigin=0
 *   seq=2  Vellore       arrivalTime=10:30  priceFromOrigin=150
 *   seq=3  Bangalore     arrivalTime=14:00  priceFromOrigin=350
 *
 * A passenger boarding at Chennai and alighting at Vellore pays ₹150 (+ seat adj).
 * A passenger boarding at Vellore and alighting at Bangalore pays
 *   ₹350 - ₹150 = ₹200 (+ seat adj).  Compute in service layer.
 */
@Entity
@Table(
    name = "bus_stop",
    uniqueConstraints = @UniqueConstraint(columnNames = {"bus_id", "stop_sequence"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusStop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bus_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Bus bus;

    @Column(nullable = false)
    private int stopSequence;

    @Column(nullable = false)
    private String stopName;

    /**
     * City or locality — useful for search (user types "Bangalore",
     * matches multiple stop names in that city).
     */
    private String city;

    /** Scheduled arrival time at this stop, e.g. "14:00". Null for the first stop. */
    private String arrivalTime;

    /** Scheduled departure time from this stop, e.g. "14:10". Null for last stop. */
    private String departureTime;

    /**
     * Admin-entered cumulative base price (in ₹) from origin to this stop.
     * Always 0 for the first stop (origin).
     */
    @Column(nullable = false)
    private int priceFromOrigin;

    /**
     * Optional platform/bay number at this stop.
     */
    private String platformNo;
}