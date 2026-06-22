package com.demo.service;

import com.demo.entity.Seat;
import com.demo.entity.SeatLayout;
import com.demo.enums.DeckLevel;
import com.demo.enums.DeckType;
import com.demo.enums.LayoutArrangement;
import com.demo.enums.SeatType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates the list of Seat records that map to a given SeatLayout.
 *
 * Called once when admin saves a new bus / updates its layout config.
 * The generated seats are persisted and then used by the booking service
 * for availability checks and by the FE for rendering the seat grid.
 *
 * ─── Generation rules ─────────────────────────────────────────────────────────
 *
 *  1. Iterate rows 1..lowerDeckRows, columns derived from lowerDeckArrangement.
 *     Assign deckLevel = LOWER.  isSleeper depends on deckConfiguration.
 *
 *  2. If DOUBLE_DECK, repeat for rows 1..upperDeckRows with deckLevel = UPPER.
 *
 *  3. If hasRearBenchRow, append a special full-width row at the end of LOWER deck.
 *     Seats in that row all get seatType = AISLE (centre bench convention).
 *
 *  4. seatLabel: LOWER → "L{n}", UPPER → "U{n}", rear bench → "R{n}".
 *
 *  5. seatNumber is globally sequential (1..totalSeats) across the whole bus.
 */
@Service
@Slf4j
public class SeatLayoutService {

    /**
     * Generates and attaches seats to the given SeatLayout.
     * Clears any previously generated seats first (idempotent on re-generation).
     *
     * @param layout SeatLayout with config fields already set by admin
     * @return the same layout with populated seats list (not yet persisted)
     */
    public SeatLayout generateSeats(SeatLayout layout) {
        log.info("Generating seats for layout id={} config={}/{}",
                layout.getId(), layout.getDeckConfiguration(), layout.getLowerDeckArrangement());

        List<Seat> seats = new ArrayList<>();
        int seatNumber = 1;
        boolean isSleeper = layout.getDeckConfiguration() == DeckType.DOUBLE_DECK;

        // ── Lower deck (or only deck for SINGLE_DECK) ──────────────────────────
        seatNumber = generateDeckSeats(
                seats, layout, DeckLevel.LOWER, "L",
                layout.getLowerDeckRows(), layout.getLowerDeckArrangement(),
                isSleeper, seatNumber
        );

        // ── Upper deck (sleeper buses only) ────────────────────────────────────
        if (isSleeper
                && layout.getUpperDeckArrangement() != null
                && layout.getUpperDeckRows() != null) {
            seatNumber = generateDeckSeats(
                    seats, layout, DeckLevel.UPPER, "U",
                    layout.getUpperDeckRows(), layout.getUpperDeckArrangement(),
                    true, seatNumber
            );
        }

        // ── Optional rear bench row ────────────────────────────────────────────
        if (layout.isHasRearBenchRow() && layout.getRearBenchSeatCount() != null) {
            seatNumber = generateRearBench(seats, layout, seatNumber, layout.getRearBenchSeatCount());
        }

        layout.getSeats().clear();
        layout.getSeats().addAll(seats);

        log.info("Generated {} seats total", seats.size());
        return layout;
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private int generateDeckSeats(
            List<Seat> seats,
            SeatLayout layout,
            DeckLevel deckLevel,
            String labelPrefix,
            int rowCount,
            LayoutArrangement arrangement,
            boolean isSleeper,
            int startSeatNumber) {

        int seatNumber = startSeatNumber;
        int[] columnSeatTypes = columnSeatTypesFor(arrangement);  // SeatType ordinal per column

        for (int row = 1; row <= rowCount; row++) {
            for (int col = 1; col <= columnSeatTypes.length; col++) {
                SeatType seatType = SeatType.values()[columnSeatTypes[col - 1]];

                Seat seat = Seat.builder()
                        .seatLayout(layout)
                        .seatNumber(seatNumber)
                        .seatLabel(labelPrefix + seatNumber)
                        .rowNumber(row)
                        .columnPosition(col)
                        .deckLevel(deckLevel)
                        .seatType(seatType)
                        .isSleeper(isSleeper)
                        .isActive(true)
                        .build();

                seats.add(seat);
                seatNumber++;
            }
        }
        return seatNumber;
    }

    private int generateRearBench(List<Seat> seats, SeatLayout layout, int startSeatNumber, int count) {
        int seatNumber = startSeatNumber;
        // Rear bench seats sit after the last normal row; rowNumber = lowerDeckRows + 1
        int rearRow = layout.getLowerDeckRows() + 1;

        for (int col = 1; col <= count; col++) {
            Seat seat = Seat.builder()
                    .seatLayout(layout)
                    .seatNumber(seatNumber)
                    .seatLabel("R" + seatNumber)
                    .rowNumber(rearRow)
                    .columnPosition(col)
                    .deckLevel(DeckLevel.LOWER)
                    .seatType(col == 1 || col == count ? SeatType.WINDOW : SeatType.AISLE)
                    .isSleeper(false)
                    .isActive(true)
                    .build();

            seats.add(seat);
            seatNumber++;
        }
        return seatNumber;
    }

    /**
     * Returns an int[] where each element is the SeatType ordinal for that column position.
     *
     * SeatType ordinals: 0=WINDOW, 1=AISLE, 2=MIDDLE
     *
     * TWO_TWO   (4 cols):  W  A  |  A  W         → [0,1,1,0]
     * TWO_THREE (5 cols):  W  A  |  A  M  W      → [0,1,1,2,0]
     * TWO_ONE   (3 cols):  W  A  |  W             → [0,1,0]
     * ONE_ONE   (2 cols):  W  |  W                → [0,0]
     */
    private int[] columnSeatTypesFor(LayoutArrangement arrangement) {
        return switch (arrangement) {
            case TWO_TWO   -> new int[]{0, 1, 1, 0};
            case TWO_THREE -> new int[]{0, 1, 1, 2, 0};
            case TWO_ONE   -> new int[]{0, 1, 0};
            case ONE_ONE   -> new int[]{0, 0};
        };
    }
}