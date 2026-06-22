package com.demo.entity;

import com.demo.enums.DeckLevel;
import com.demo.enums.PriceAdjustmentType;
import com.demo.enums.SeatType;
import jakarta.persistence.*;
import lombok.*;

/**
 * Admin-configured price adjustment for a specific seat type on a specific bus.
 *
 * ─── How final ticket price is calculated ────────────────────────────────────
 *
 *  basePrice       = BusStop.priceFromOrigin(destination) - BusStop.priceFromOrigin(boarding)
 *  seatAdjustment  = resolved from SeatTypePricing for this bus + seatType + deckLevel
 *
 *  If adjustmentType = FLAT:
 *      finalPrice = basePrice + adjustmentValue      (can be negative for discounts)
 *
 *  If adjustmentType = PERCENTAGE:
 *      finalPrice = basePrice + (basePrice * adjustmentValue / 100)
 *
 * ─── Example ─────────────────────────────────────────────────────────────────
 *
 *  Bus KA-01-AB-1234, WINDOW seat, LOWER deck → FLAT +₹50
 *  Bus KA-01-AB-1234, UPPER berth, UPPER deck → FLAT -₹100   (upper berth cheaper)
 *  Bus KA-01-AB-1234, AISLE seat, LOWER deck  → FLAT +₹0     (no adjustment)
 *
 * A null / missing entry for a seat type means no adjustment (use base price as-is).
 */
@Entity
@Table(
    name = "seat_type_pricing",
    uniqueConstraints = @UniqueConstraint(columnNames = {"bus_id", "seat_type", "deck_level"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatTypePricing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bus_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Bus bus;

    /** Which seat type this pricing rule applies to */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeatType seatType;          // WINDOW | AISLE | MIDDLE

    /** Which deck this pricing rule applies to (allows different pricing per deck) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeckLevel deckLevel;        // LOWER | UPPER

    /** Whether adjustmentValue is a flat ₹ amount or a percentage */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PriceAdjustmentType adjustmentType;   // FLAT | PERCENTAGE

    /**
     * The adjustment magnitude.
     * Positive = surcharge, negative = discount.
     * For FLAT: in ₹ (e.g. 50 means +₹50, -100 means -₹100)
     * For PERCENTAGE: e.g. 10 means +10%, -15 means -15%
     */
    @Column(nullable = false)
    private int adjustmentValue;

    /** Optional label for admin UI, e.g. "Window Premium", "Upper Berth Discount" */
    private String label;
}