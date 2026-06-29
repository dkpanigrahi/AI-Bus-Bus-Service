package com.demo.service;

import com.demo.client.BookingServiceClient;
import com.demo.dto.*;
import com.demo.dto.response.*;
import com.demo.entity.Bus;
import com.demo.entity.Conductor;
import com.demo.entity.Driver;
import com.demo.exception.ResourceNotFoundException;
import com.demo.repository.BusRepository;
import com.demo.repository.ConductorRepository;
import com.demo.repository.DriverRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.demo.dto.*;
import com.demo.entity.*;
import com.demo.enums.DeckLevel;
import com.demo.enums.PriceAdjustmentType;
import com.demo.enums.SeatType;
import com.demo.repository.*;

import java.util.*;
import java.util.function.Function;

@Service
@Slf4j
@RequiredArgsConstructor
public class BusService {

    private final BusRepository busRepository;
    private final DriverRepository driverRepository;
    private final ConductorRepository conductorRepository;
    private final SeatLayoutService seatLayoutService;
    private final BookingServiceClient bookingServiceClient;

    // =========================================================================
    //  BUS — CREATE
    // =========================================================================

    /**
     * Creates a fully-configured bus in one transaction:
     * 1. Validates uniqueness of busNo.
     * 2. Resolves optional Driver and Conductor by ID.
     * 3. Maps stops from the request, enforcing sequence integrity.
     * 4. Builds the SeatLayout entity and auto-generates Seat records via SeatLayoutService.
     * 5. Applies any admin seat overrides (label/type/active flag).
     * 6. Maps SeatTypePricing rules.
     * 7. Saves the Bus (cascade saves everything else).
     */
    @Transactional
    public BusDto createBus(BusCreateRequest req) {
        log.info("Creating bus busNo={}", req.getBusNo());

        if (busRepository.existsByBusNo(req.getBusNo())) {
            throw new IllegalArgumentException("Bus with number " + req.getBusNo() + " already exists");
        }

        Bus bus = Bus.builder()
                .busNo(req.getBusNo())
                .busName(req.getBusName())
                .coach(req.getCoach())
                .busType(req.getBusType())
                .availableEveryDay(req.isAvailableEveryDay())
                .specificDays(req.getSpecificDays() != null ? req.getSpecificDays() : new ArrayList<>())
                .busOperatorId(req.getBusOperatorId())
                .build();

        // ── Staff ──────────────────────────────────────────────────────────────
        if (req.getDriverId() != null) {
            bus.setDriver(driverRepository.findById(req.getDriverId())
                    .orElseThrow(() -> new ResourceNotFoundException("Driver", "id", req.getDriverId())));
        }
        if (req.getConductorId() != null) {
            bus.setConductor(conductorRepository.findById(req.getConductorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Conductor", "id", req.getConductorId())));
        }

        // ── Stops ──────────────────────────────────────────────────────────────
        List<BusStop> stops = buildStops(req.getStops(), bus);
        bus.getStops().addAll(stops);

        // ── Seat Layout ────────────────────────────────────────────────────────
        SeatLayout layout = buildSeatLayout(req.getSeatLayout());
        seatLayoutService.generateSeats(layout);
        applySeatOverrides(layout, req.getSeatLayout().getSeatOverrides());
        bus.setSeatLayout(layout);

        // ── Seat Type Pricing ──────────────────────────────────────────────────
        if (req.getSeatTypePricings() != null) {
            List<SeatTypePricing> pricings = buildSeatTypePricings(req.getSeatTypePricings(), bus);
            bus.getSeatTypePricings().addAll(pricings);
        }

        Bus saved = busRepository.save(bus);
        log.info("Bus created id={} totalSeats={}", saved.getId(), saved.getTotalSeats());
        return mapToBusDto(saved);
    }

    // =========================================================================
    //  BUS — READ
    // =========================================================================

    @Transactional(readOnly = true)
    public BusDto getBusById(int id) {
        log.debug("Fetching bus id={}", id);
        return mapToBusDto(findActiveById(id));
    }

    @Transactional(readOnly = true)
    public BusDto getBusByBusNo(String busNo) {
        log.debug("Fetching bus busNo={}", busNo);
        Bus bus = busRepository.findByBusNoAndIsDeletedFalse(busNo)
                .orElseThrow(() -> new ResourceNotFoundException("Bus", "busNo", busNo));
        return mapToBusDto(bus);
    }

    @Transactional(readOnly = true)
    public List<BusDto> getAllBuses() {
        log.info("Fetching all active buses");
        return busRepository.findAllByIsDeletedFalse().stream()
                .map(this::mapToBusDto)
                .collect(Collectors.toList());
    }

    // =========================================================================
    //  BUS — UPDATE
    // =========================================================================

    /**
     * Full replacement update. Stops, seat layout, and seat-type pricing are
     * replaced entirely (orphanRemoval handles the deletes via cascade).
     */
    @Transactional
    public BusDto updateBus(int id, BusCreateRequest req) {
        log.info("Updating bus id={}", id);
        Bus bus = findActiveById(id);

        // ── Core fields ────────────────────────────────────────────────────────
        bus.setBusNo(req.getBusNo());
        bus.setBusName(req.getBusName());
        bus.setCoach(req.getCoach());
        bus.setBusType(req.getBusType());
        bus.setAvailableEveryDay(req.isAvailableEveryDay());
        bus.setSpecificDays(req.getSpecificDays() != null ? req.getSpecificDays() : new ArrayList<>());
        bus.setBusOperatorId(req.getBusOperatorId());

        // ── Staff ──────────────────────────────────────────────────────────────
        bus.setDriver(req.getDriverId() != null
                ? driverRepository.findById(req.getDriverId())
                .orElseThrow(() -> new ResourceNotFoundException("Driver", "id", req.getDriverId()))
                : null);
        bus.setConductor(req.getConductorId() != null
                ? conductorRepository.findById(req.getConductorId())
                .orElseThrow(() -> new ResourceNotFoundException("Conductor", "id", req.getConductorId()))
                : null);

        // ── Stops: replace all ─────────────────────────────────────────────────
        bus.getStops().clear();
        bus.getStops().addAll(buildStops(req.getStops(), bus));

        // ── Seat Layout: rebuild and re-generate seats ─────────────────────────
        SeatLayout newLayout = buildSeatLayout(req.getSeatLayout());
        seatLayoutService.generateSeats(newLayout);
        applySeatOverrides(newLayout, req.getSeatLayout().getSeatOverrides());
        bus.setSeatLayout(newLayout);

        // ── Seat Type Pricing: replace all ────────────────────────────────────
        bus.getSeatTypePricings().clear();
        if (req.getSeatTypePricings() != null) {
            bus.getSeatTypePricings().addAll(buildSeatTypePricings(req.getSeatTypePricings(), bus));
        }

        Bus saved = busRepository.save(bus);
        log.info("Bus updated id={}", saved.getId());
        return mapToBusDto(saved);
    }

    /**
     * Partial update — toggles activeStatus only (admin enables/disables a bus
     * without touching its configuration).
     */
    @Transactional
    public BusDto toggleBusStatus(int id, boolean active) {
        Bus bus = findActiveById(id);
        bus.setActiveStatus(active);
        Bus saved = busRepository.save(bus);
        log.info("Bus id={} activeStatus set to {}", id, active);
        return mapToBusDto(saved);
    }

    // =========================================================================
    //  BUS — DELETE (soft)
    // =========================================================================

    @Transactional
    public void deleteBus(int id) {
        log.info("Soft-deleting bus id={}", id);
        Bus bus = findActiveById(id);
        bus.setDeleted(true);
        bus.setActiveStatus(false);
        busRepository.save(bus);
    }

    // =========================================================================
    //  STOPS — PATCH (update a single stop without rebuilding everything)
    // =========================================================================

    /**
     * Updates a single stop's timing or price without touching the rest of the bus.
     * Useful when the admin adjusts one intermediate stop price post-creation.
     */
    @Transactional
    public BusDto updateStop(int busId, int stopSequence, BusStopRequest req) {
        log.info("Updating stop seq={} on busId={}", stopSequence, busId);
        Bus bus = findActiveById(busId);

        BusStop stop = bus.getStops().stream()
                .filter(s -> s.getStopSequence() == stopSequence)
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("BusStop", "stopSequence", stopSequence));

        stop.setStopName(req.getStopName());
        stop.setCity(req.getCity());
        stop.setArrivalTime(req.getArrivalTime());
        stop.setDepartureTime(req.getDepartureTime());
        stop.setPriceFromOrigin(req.getPriceFromOrigin());
        stop.setPlatformNo(req.getPlatformNo());

        return mapToBusDto(busRepository.save(bus));
    }

    // =========================================================================
    //  SEATS — OVERRIDE (admin disables / relabels individual seats post-creation)
    // =========================================================================

    @Transactional
    public BusDto applySeatsOverride(int busId, List<SeatOverrideRequest> overrides) {
        log.info("Applying {} seat overrides on busId={}", overrides.size(), busId);
        Bus bus = findActiveById(busId);

        if (bus.getSeatLayout() == null) {
            throw new IllegalStateException("Bus id=" + busId + " has no seat layout configured");
        }
        applySeatOverrides(bus.getSeatLayout(), overrides);
        return mapToBusDto(busRepository.save(bus));
    }

    // =========================================================================
    //  SEAT TYPE PRICING — UPSERT (admin updates pricing rules without full update)
    // =========================================================================

    @Transactional
    public BusDto upsertSeatTypePricings(int busId, List<SeatTypePricingRequest> pricingRequests) {
        log.info("Upserting {} seat-type pricing rules on busId={}", pricingRequests.size(), busId);
        Bus bus = findActiveById(busId);

        // Build a lookup map of existing rules: key = "seatType_deckLevel"
        Map<String, SeatTypePricing> existing = bus.getSeatTypePricings().stream()
                .collect(Collectors.toMap(
                        p -> pricingKey(p.getSeatType(), p.getDeckLevel()),
                        Function.identity()
                ));

        for (SeatTypePricingRequest req : pricingRequests) {
            String key = pricingKey(req.getSeatType(), req.getDeckLevel());
            if (existing.containsKey(key)) {
                // update in place
                SeatTypePricing rule = existing.get(key);
                rule.setAdjustmentType(req.getAdjustmentType());
                rule.setAdjustmentValue(req.getAdjustmentValue());
                rule.setLabel(req.getLabel());
            } else {
                // insert new rule
                bus.getSeatTypePricings().add(SeatTypePricing.builder()
                        .bus(bus)
                        .seatType(req.getSeatType())
                        .deckLevel(req.getDeckLevel())
                        .adjustmentType(req.getAdjustmentType())
                        .adjustmentValue(req.getAdjustmentValue())
                        .label(req.getLabel())
                        .build());
            }
        }

        return mapToBusDto(busRepository.save(bus));
    }

    // =========================================================================
    //  SEARCH
    // =========================================================================

    /**
     * Searches buses that have boarding city as an intermediate stop AND
     * alighting city as a later stop, available on the given date's day-of-week.
     * Returns lightweight search cards with base fare computed per result.
     */
    @Transactional(readOnly = true)
    public List<BusSearchResponseDto> searchBuses(String fromCity, String toCity, String date) {
        log.info("Search: {} → {} on {}", fromCity, toCity, date);

        String dayOfWeek = LocalDate.parse(date)
                .getDayOfWeek()
                .getDisplayName(TextStyle.FULL, java.util.Locale.ENGLISH)
                .toUpperCase();

        List<Bus> buses = busRepository.findBusesByStopCitiesAndDay(fromCity, toCity, dayOfWeek);
        log.info("Found {} buses for {} → {} on {}", buses.size(), fromCity, toCity, dayOfWeek);

        return buses.stream()
                .map(bus -> mapToSearchDto(bus, fromCity, toCity))
                .filter(Objects::nonNull)        // filter buses where sequence order is wrong
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BusSearchResponseDto> searchBusesByRoute(String fromCity, String toCity) {
        log.info("Route search: {} → {}", fromCity, toCity);
        List<Bus> buses = busRepository.findBusesByStopCities(fromCity, toCity);
        return buses.stream()
                .map(bus -> mapToSearchDto(bus, fromCity, toCity))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    // =========================================================================
    //  SEAT AVAILABILITY  (delegates to Booking Service via Feign)
    // =========================================================================

    @Transactional(readOnly = true)
    public SeatAvailabilityResponse getSeatAvailability(int busId, String date,
           int boardingStopSequence, int alightingStopSequence) {
        log.info("Seat availability for busId={} date={}", busId, date);
        Bus bus = findActiveById(busId);

        try {
            ResponseEntity<ApiResponse<SeatAvailabilityResponse>> resp =
                    bookingServiceClient.getSeatAvailability(busId, date,boardingStopSequence,alightingStopSequence);
            if (resp.getBody() != null && resp.getBody().isSuccess()) {
                return resp.getBody().getData();
            }
        } catch (Exception e) {
            log.warn("Booking service unavailable for busId={}: {}. Returning all-available fallback.",
                    busId, e.getMessage());
        }

        // Fallback: all active seats available
        Map<Integer, Boolean> seatMap = new LinkedHashMap<>();
        if (bus.getSeatLayout() != null) {
            bus.getSeatLayout().getSeats().stream()
                    .filter(Seat::isActive)
                    .forEach(s -> seatMap.put(s.getSeatNumber(), true));
        }
        return SeatAvailabilityResponse.builder()
                .busId(busId)
                .date(date)
                .seatMap(seatMap)
                .totalSeats(bus.getTotalSeats())
                .build();
    }

    // =========================================================================
    //  TICKET PRICE CALCULATION
    // =========================================================================

    /**
     * Computes the final ticket price for a specific seat type on a given bus,
     * boarding and alighting at specific stops.
     * <p>
     * Called by the FE seat picker when a passenger selects a seat so the
     * exact price can be shown before booking.
     */
    @Transactional(readOnly = true)
    public TicketPriceCalculationResponse calculateTicketPrice(
            int busId,
            int boardingStopSeq,
            int alightingStopSeq,
            SeatType seatType,
            DeckLevel deckLevel) {

        log.debug("Price calc busId={} stops={}->{} seatType={} deck={}",
                busId, boardingStopSeq, alightingStopSeq, seatType, deckLevel);

        Bus bus = findActiveById(busId);

        if (boardingStopSeq >= alightingStopSeq) {
            throw new IllegalArgumentException(
                    "Boarding stop sequence must be less than alighting stop sequence");
        }

        // Resolve stops
        Map<Integer, BusStop> stopsBySeq = bus.getStops().stream()
                .collect(Collectors.toMap(BusStop::getStopSequence, Function.identity()));

        BusStop boarding = Optional.ofNullable(stopsBySeq.get(boardingStopSeq))
                .orElseThrow(() -> new ResourceNotFoundException("BusStop", "sequence", boardingStopSeq));
        BusStop alighting = Optional.ofNullable(stopsBySeq.get(alightingStopSeq))
                .orElseThrow(() -> new ResourceNotFoundException("BusStop", "sequence", alightingStopSeq));

        // Base fare for this segment
        int basePrice = alighting.getPriceFromOrigin() - boarding.getPriceFromOrigin();

        // Find applicable seat-type pricing rule
        Optional<SeatTypePricing> rule = bus.getSeatTypePricings().stream()
                .filter(p -> p.getSeatType() == seatType && p.getDeckLevel() == deckLevel)
                .findFirst();

        int adjustmentValue = rule.map(SeatTypePricing::getAdjustmentValue).orElse(0);
        PriceAdjustmentType adjType = rule.map(SeatTypePricing::getAdjustmentType)
                .orElse(PriceAdjustmentType.FLAT);

        int seatAdjustment = adjType == PriceAdjustmentType.PERCENTAGE
                ? (int) Math.round(basePrice * adjustmentValue / 100.0)
                : adjustmentValue;

        int finalPrice = basePrice + seatAdjustment;

        String breakdown = buildPriceBreakdown(basePrice, seatType, deckLevel, adjType,
                adjustmentValue, seatAdjustment, finalPrice);

        return TicketPriceCalculationResponse.builder()
                .busId(busId)
                .boardingStopSequence(boardingStopSeq)
                .alightingStopSequence(alightingStopSeq)
                .boardingStopName(boarding.getStopName())
                .alightingStopName(alighting.getStopName())
                .basePrice(basePrice)
                .seatType(seatType)
                .deckLevel(deckLevel)
                .adjustmentType(adjType)
                .adjustmentValue(adjustmentValue)
                .finalPrice(finalPrice)
                .priceBreakdown(breakdown)
                .build();
    }

    // =========================================================================
    //  DRIVER
    // =========================================================================

    @Transactional
    public DriverDto addDriver(Driver driver) {
        log.info("Adding driver phone={}", driver.getPhoneNo());
        if (driverRepository.existsByPhoneNo(driver.getPhoneNo())) {
            throw new IllegalArgumentException("Driver with phone " + driver.getPhoneNo() + " already exists");
        }
        Driver saved = driverRepository.save(driver);
        return mapToDriverDto(saved);
    }

    @Transactional(readOnly = true)
    public List<DriverDto> getAllDrivers() {
        return driverRepository.findAll().stream()
                .map(this::mapToDriverDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DriverDto getDriverById(int id) {
        return mapToDriverDto(driverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Driver", "id", id)));
    }

    // =========================================================================
    //  CONDUCTOR
    // =========================================================================

    @Transactional
    public ConductorDto addConductor(Conductor conductor) {
        log.info("Adding conductor phone={}", conductor.getPhoneNo());
        if (conductorRepository.existsByPhoneNo(conductor.getPhoneNo())) {
            throw new IllegalArgumentException("Conductor with phone " + conductor.getPhoneNo() + " already exists");
        }
        Conductor saved = conductorRepository.save(conductor);
        return mapToConductorDto(saved);
    }

    @Transactional(readOnly = true)
    public List<ConductorDto> getAllConductors() {
        return conductorRepository.findAll().stream()
                .map(this::mapToConductorDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ConductorDto getConductorById(int id) {
        return mapToConductorDto(conductorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conductor", "id", id)));
    }

    // =========================================================================
    //  PRIVATE — ENTITY BUILDERS
    // =========================================================================

    private List<BusStop> buildStops(List<BusStopRequest> stopRequests, Bus bus) {
        if (stopRequests == null || stopRequests.isEmpty()) {
            throw new IllegalArgumentException("A bus must have at least 2 stops (origin and destination)");
        }
        if (stopRequests.size() < 2) {
            throw new IllegalArgumentException("A bus must have at least 2 stops");
        }

        // Auto-assign sequence from list order if FE sends 0
        List<BusStop> stops = new ArrayList<>();
        for (int i = 0; i < stopRequests.size(); i++) {
            BusStopRequest req = stopRequests.get(i);
            int seq = req.getStopSequence() > 0 ? req.getStopSequence() : (i + 1);

            stops.add(BusStop.builder()
                    .bus(bus)
                    .stopSequence(seq)
                    .stopName(req.getStopName())
                    .city(req.getCity())
                    .arrivalTime(req.getArrivalTime())
                    .departureTime(req.getDepartureTime())
                    .priceFromOrigin(req.getPriceFromOrigin())
                    .platformNo(req.getPlatformNo())
                    .build());
        }

        // Validate: first stop must have priceFromOrigin = 0
        stops.sort(Comparator.comparingInt(BusStop::getStopSequence));
        if (stops.get(0).getPriceFromOrigin() != 0) {
            throw new IllegalArgumentException("First stop (origin) must have priceFromOrigin = 0");
        }

        // Validate: prices must be non-decreasing
        for (int i = 1; i < stops.size(); i++) {
            if (stops.get(i).getPriceFromOrigin() < stops.get(i - 1).getPriceFromOrigin()) {
                throw new IllegalArgumentException(
                        "Stop prices must be non-decreasing along the route. " +
                                "Stop seq=" + stops.get(i).getStopSequence() + " has lower price than previous.");
            }
        }

        return stops;
    }

    private SeatLayout buildSeatLayout(SeatLayoutRequest req) {
        if (req == null) {
            throw new IllegalArgumentException("Seat layout configuration is required");
        }
        return SeatLayout.builder()
                .deckConfiguration(req.getDeckConfiguration())
                .lowerDeckArrangement(req.getLowerDeckArrangement())
                .lowerDeckRows(req.getLowerDeckRows())
                .upperDeckArrangement(req.getUpperDeckArrangement())
                .upperDeckRows(req.getUpperDeckRows())
                .hasRearBenchRow(req.isHasRearBenchRow())
                .rearBenchSeatCount(req.getRearBenchSeatCount())
                .build();
    }

    private void applySeatOverrides(SeatLayout layout, List<SeatOverrideRequest> overrides) {
        if (overrides == null || overrides.isEmpty()) return;

        Map<Integer, Seat> seatsBySeatNumber = layout.getSeats().stream()
                .collect(Collectors.toMap(Seat::getSeatNumber, Function.identity()));

        for (SeatOverrideRequest override : overrides) {
            Seat seat = seatsBySeatNumber.get(override.getSeatNumber());
            if (seat == null) {
                log.warn("Seat override ignored: seatNumber={} not found in layout", override.getSeatNumber());
                continue;
            }
            if (override.getSeatLabel() != null) seat.setSeatLabel(override.getSeatLabel());
            if (override.getSeatType() != null) seat.setSeatType(override.getSeatType());
            if (override.getAdminNote() != null) seat.setAdminNote(override.getAdminNote());
            seat.setActive(override.isActive());
        }
    }

    private List<SeatTypePricing> buildSeatTypePricings(List<SeatTypePricingRequest> requests, Bus bus) {
        return requests.stream()
                .map(req -> SeatTypePricing.builder()
                        .bus(bus)
                        .seatType(req.getSeatType())
                        .deckLevel(req.getDeckLevel())
                        .adjustmentType(req.getAdjustmentType())
                        .adjustmentValue(req.getAdjustmentValue())
                        .label(req.getLabel())
                        .build())
                .collect(Collectors.toList());
    }

    // =========================================================================
    //  PRIVATE — MAPPING HELPERS
    // =========================================================================

    public BusDto mapToBusDto(Bus bus) {
        return BusDto.builder()
                .id(bus.getId())
                .busNo(bus.getBusNo())
                .busName(bus.getBusName())
                .coach(bus.getCoach())
                .busType(bus.getBusType())
                .availableEveryDay(bus.isAvailableEveryDay())
                .specificDays(bus.getSpecificDays())
                .stops(bus.getStops().stream().map(this::mapToStopDto).collect(Collectors.toList()))
                .seatLayout(bus.getSeatLayout() != null ? mapToSeatLayoutDto(bus, bus.getSeatLayout()) : null)
                .seatTypePricings(bus.getSeatTypePricings().stream()
                        .map(this::mapToSeatTypePricingDto)
                        .collect(Collectors.toList()))
                .driver(bus.getDriver() != null ? mapToDriverDto(bus.getDriver()) : null)
                .conductor(bus.getConductor() != null ? mapToConductorDto(bus.getConductor()) : null)
                .activeStatus(bus.isActiveStatus())
                .build();
    }

    private BusStopDto mapToStopDto(BusStop stop) {
        return BusStopDto.builder()
                .id(stop.getId())
                .stopSequence(stop.getStopSequence())
                .stopName(stop.getStopName())
                .city(stop.getCity())
                .arrivalTime(stop.getArrivalTime())
                .departureTime(stop.getDepartureTime())
                .priceFromOrigin(stop.getPriceFromOrigin())
                .platformNo(stop.getPlatformNo())
                .build();
    }

    private SeatLayoutDto mapToSeatLayoutDto(Bus bus, SeatLayout layout) {
        // Build a pricing lookup map: "seatType_deckLevel" → adjustmentValue
        Map<String, Integer> adjustmentMap = bus.getSeatTypePricings().stream()
                .collect(Collectors.toMap(
                        p -> pricingKey(p.getSeatType(), p.getDeckLevel()),
                        p -> resolveAdjustmentFlat(p, 0)   // base=0 for display; FE resolves final price
                ));

        List<SeatDto> seatDtos = layout.getSeats().stream()
                .map(seat -> {
                    int adj = adjustmentMap.getOrDefault(
                            pricingKey(seat.getSeatType(), seat.getDeckLevel()), 0);
                    return SeatDto.builder()
                            .id(seat.getId())
                            .seatNumber(seat.getSeatNumber())
                            .seatLabel(seat.getSeatLabel())
                            .rowNumber(seat.getRowNumber())
                            .columnPosition(seat.getColumnPosition())
                            .deckLevel(seat.getDeckLevel())
                            .seatType(seat.getSeatType())
                            .isSleeper(seat.isSleeper())
                            .isActive(seat.isActive())
                            .seatPriceAdjustment(adj)
                            .isBooked(false)   // booking service enriches this separately
                            .build();
                })
                .collect(Collectors.toList());

        return SeatLayoutDto.builder()
                .id(layout.getId())
                .deckConfiguration(layout.getDeckConfiguration())
                .lowerDeckArrangement(layout.getLowerDeckArrangement())
                .lowerDeckRows(layout.getLowerDeckRows())
                .upperDeckArrangement(layout.getUpperDeckArrangement())
                .upperDeckRows(layout.getUpperDeckRows())
                .hasRearBenchRow(layout.isHasRearBenchRow())
                .rearBenchSeatCount(layout.getRearBenchSeatCount())
                .seats(seatDtos)
                .build();
    }

    private SeatTypePricingDto mapToSeatTypePricingDto(SeatTypePricing p) {
        return SeatTypePricingDto.builder()
                .id(p.getId())
                .seatType(p.getSeatType())
                .deckLevel(p.getDeckLevel())
                .adjustmentType(p.getAdjustmentType())
                .adjustmentValue(p.getAdjustmentValue())
                .label(p.getLabel())
                .build();
    }

    private DriverDto mapToDriverDto(Driver d) {
        return DriverDto.builder()
                .id(d.getId())
                .name(d.getName())
                .phoneNo(d.getPhoneNo())
                .salary(d.getSalary())
                .build();
    }

    private ConductorDto mapToConductorDto(Conductor c) {
        return ConductorDto.builder()
                .id(c.getId())
                .name(c.getName())
                .phoneNo(c.getPhoneNo())
                .salary(c.getSalary())
                .build();
    }

    /**
     * Maps a Bus to a search-result card for a specific from-city → to-city query.
     * Returns null if the bus does not serve the route in the correct direction
     * (boarding stop must come before alighting stop in sequence).
     */
    private BusSearchResponseDto mapToSearchDto(Bus bus, String fromCity, String toCity) {
        List<BusStop> stops = bus.getStops();

        Optional<BusStop> boardingOpt = stops.stream()
                .filter(s -> cityMatches(s, fromCity))
                .min(Comparator.comparingInt(BusStop::getStopSequence));
        Optional<BusStop> alightingOpt = stops.stream()
                .filter(s -> cityMatches(s, toCity))
                .max(Comparator.comparingInt(BusStop::getStopSequence));

        if (boardingOpt.isEmpty() || alightingOpt.isEmpty()) return null;

        BusStop boarding = boardingOpt.get();
        BusStop alighting = alightingOpt.get();

        if (boarding.getStopSequence() >= alighting.getStopSequence()) return null;

        int basePrice = alighting.getPriceFromOrigin() - boarding.getPriceFromOrigin();

        // Try to get available seat count from booking service; fallback to totalSeats
        int availableSeats = fetchAvailableSeatCount(bus.getId());

        return BusSearchResponseDto.builder()
                .id(bus.getId())
                .busNo(bus.getBusNo())
                .busName(bus.getBusName())
                .coach(bus.getCoach())
                .busType(bus.getBusType())
                .originCity(boarding.getCity())
                .destinationCity(alighting.getCity())
                .originStopName(boarding.getStopName())
                .destinationStopName(alighting.getStopName())
                .departureTime(boarding.getDepartureTime())
                .arrivalTime(alighting.getArrivalTime())
                .basePrice(basePrice)
                .totalSeats(bus.getTotalSeats())
                .availableSeats(availableSeats)
                .driverName(bus.getDriver() != null ? bus.getDriver().getName() : "N/A")
                .conductorName(bus.getConductor() != null ? bus.getConductor().getName() : "N/A")
                .build();
    }

    // =========================================================================
    //  PRIVATE — UTILITY
    // =========================================================================

    private Bus findActiveById(int id) {
        return busRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bus", "id", id));
    }

    private boolean cityMatches(BusStop stop, String city) {
        return city.equalsIgnoreCase(stop.getCity())
                || city.equalsIgnoreCase(stop.getStopName());
    }

    private String pricingKey(SeatType seatType, DeckLevel deckLevel) {
        return seatType.name() + "_" + deckLevel.name();
    }

    /**
     * Resolves a FLAT-equivalent adjustment value for display purposes in the seat grid.
     * For PERCENTAGE rules the FE needs the raw % value and will compute at booking time
     * once the route segment is known, so we return 0 here for the layout snapshot.
     */
    private int resolveAdjustmentFlat(SeatTypePricing pricing, int basePrice) {
        if (pricing.getAdjustmentType() == PriceAdjustmentType.FLAT) {
            return pricing.getAdjustmentValue();
        }
        // For PERCENTAGE: return 0 in layout snapshot (FE resolves after stop selection)
        return 0;
    }

    private int fetchAvailableSeatCount(int busId) {
        try {
            ResponseEntity<ApiResponse<Integer>> resp =
                    bookingServiceClient.getBookedSeatCount(busId,
                            LocalDate.now().toString());
            if (resp.getBody() != null && resp.getBody().isSuccess()
                    && resp.getBody().getData() != null) {
                Bus bus = busRepository.findById(busId).orElse(null);
                if (bus != null) {
                    return bus.getTotalSeats() - resp.getBody().getData();
                }
            }
        } catch (Exception e) {
            log.debug("Could not fetch booked seat count for busId={}: {}", busId, e.getMessage());
        }
        // Fallback: return total seats (optimistic)
        return busRepository.findById(busId).map(Bus::getTotalSeats).orElse(0);
    }

    private String buildPriceBreakdown(int basePrice, SeatType seatType, DeckLevel deckLevel,
                                       PriceAdjustmentType adjType, int adjValue,
                                       int seatAdjustment, int finalPrice) {
        if (adjValue == 0) {
            return "Base ₹" + basePrice + " (no seat adjustment) = ₹" + finalPrice;
        }
        String adjLabel = seatType.name() + " " + deckLevel.name().toLowerCase() + " berth";
        String adjStr = adjType == PriceAdjustmentType.PERCENTAGE
                ? adjValue + "% = ₹" + seatAdjustment
                : "₹" + adjValue;
        String sign = adjValue >= 0 ? "+" : "";
        return "Base ₹" + basePrice + " " + sign + adjStr + " (" + adjLabel + ") = ₹" + finalPrice;
    }
}