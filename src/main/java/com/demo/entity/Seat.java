package com.demo.entity;

import com.demo.enums.DeckLevel;
import com.demo.enums.SeatType;
import jakarta.persistence.*;
import lombok.*;

/**
 * Represents a single physical seat / berth in a bus.
 *
 * Auto-generated from SeatLayout config by SeatLayoutService.
 * Admin can override individual fields after generation
 * (e.g. disable a broken seat, relabel a seat number).
 *
 * ─── Coordinate System ────────────────────────────────────────────────────────
 *
 *  rowNumber      : 1-based row from front of the bus (or front of that deck)
 *  columnPosition : 1-based column left-to-right
 *
 *  For TWO_TWO arrangement (4 seats per row):
 *    col 1 → WINDOW (left)
 *    col 2 → AISLE  (left)
 *    col 3 → AISLE  (right)
 *    col 4 → WINDOW (right)
 *
 *  For TWO_THREE arrangement (5 seats per row):
 *    col 1 → WINDOW (left)
 *    col 2 → AISLE  (left)
 *    col 3 → AISLE  (right)
 *    col 4 → MIDDLE (right)
 *    col 5 → WINDOW (right)
 *
 *  For DOUBLE_DECK sleeper buses:
 *    deckLevel = LOWER or UPPER differentiates berths sharing the same row/col.
 *
 * ─── UI rendering ─────────────────────────────────────────────────────────────
 *
 *  Frontend receives the full seat list and reconstructs the grid using
 *  (deckLevel, rowNumber, columnPosition). seatLabel is the display tag
 *  shown on the seat box (e.g. "L1", "U1", "A1").
 */
@Entity
@Table(
    name = "seat",
    uniqueConstraints = @UniqueConstraint(columnNames = {"seat_layout_id", "seat_number"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seat_layout_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private SeatLayout seatLayout;

    /** Sequential seat number shown to passengers (1, 2, 3 … N) */
    @Column(nullable = false)
    private int seatNumber;

    /**
     * Short display label shown on the UI seat grid.
     * Auto-generated: "L1"/"U1" for sleeper, "A1"/"B1" for seater.
     * Admin can override (e.g. "W1" for window branding).
     */
    private String seatLabel;


    @Column(nullable = false)
    private int rowNumber;

    @Column(nullable = false)
    private int columnPosition;

    /**
     * LOWER = lower berth / ground floor seat.
     * UPPER = upper berth (sleeper buses only).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeckLevel deckLevel;


    /**
     * WINDOW, AISLE, or MIDDLE.
     * Derived from columnPosition + LayoutArrangement on generation;
     * admin can override.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeatType seatType;

    /**
     * True = this is a sleeper berth (horizontal).
     * False = upright seat.
     * Derived from deckConfiguration; admin can override.
     */
    private boolean isSleeper;


    /**
     * False = seat is out-of-service (broken, reserved for staff, etc.)
     * Booking service must respect this flag before allowing reservation.
     */
    @Builder.Default
    private boolean isActive = true;

    /**
     * Optional note visible to admin only, e.g. "recliner", "blocked for driver".
     */
    private String adminNote;
}