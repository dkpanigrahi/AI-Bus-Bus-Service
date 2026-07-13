package com.demo.controller;

import com.demo.dto.*;
import com.demo.dto.response.BusDto;
import com.demo.dto.response.BusSearchResponseDto;
import com.demo.dto.response.TicketPriceCalculationResponse;
import com.demo.entity.Conductor;
import com.demo.entity.Driver;
import com.demo.enums.DeckLevel;
import com.demo.enums.SeatType;
import com.demo.service.BusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/buses")
@RequiredArgsConstructor
@Slf4j
public class BusController {

    private final BusService busService;

    // =========================================================================
    //  BUS — CRUD
    // =========================================================================

    /**
     * Create a fully-configured bus.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<BusDto>> createBus(@RequestBody BusCreateRequest request,
                                                         @RequestHeader("X-Operator-Id") Long busOperatorId) {
        log.info("POST /api/buses  busNo={}", request.getBusNo());
        request.setBusOperatorId(busOperatorId);
        BusDto created = busService.createBus(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Bus created successfully", created));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BusDto>> getBusById(@PathVariable int id) {
        log.info("GET /api/buses/{}", id);
        return ResponseEntity.ok(ApiResponse.success(busService.getBusById(id)));
    }

    @GetMapping("/busno/{busNo}")
    public ResponseEntity<ApiResponse<BusDto>> getBusByBusNo(@PathVariable String busNo) {
        log.info("GET /api/buses/busno/{}", busNo);
        return ResponseEntity.ok(ApiResponse.success(busService.getBusByBusNo(busNo)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BusDto>>> getAllBuses() {
        log.info("GET /api/buses");
        return ResponseEntity.ok(ApiResponse.success(busService.getAllBuses()));
    }

    /**
     * Full replacement update — replaces stops, layout, and pricing in one shot.
     * Same request shape as createBus.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BusDto>> updateBus(
            @PathVariable int id,
            @RequestBody BusCreateRequest request) {
        log.info("PUT /api/buses/{}", id);
        BusDto updated = busService.updateBus(id, request);
        return ResponseEntity.ok(ApiResponse.success("Bus updated successfully", updated));
    }

    /**
     * Toggle active/inactive status without a full update.
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<BusDto>> toggleBusStatus(
            @PathVariable int id,
            @RequestParam boolean active) {
        log.info("PATCH /api/buses/{}/status  active={}", id, active);
        BusDto updated = busService.toggleBusStatus(id, active);
        String message = active ? "Bus activated" : "Bus deactivated";
        return ResponseEntity.ok(ApiResponse.success(message, updated));
    }

    /**
     * Soft-delete: sets isDeleted=true and activeStatus=false.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBus(@PathVariable int id) {
        log.info("DELETE /api/buses/{}", id);
        busService.deleteBus(id);
        return ResponseEntity.ok(ApiResponse.success("Bus deleted successfully", null));
    }

    // =========================================================================
    //  STOPS — PATCH
    // =========================================================================

    /**
     * Patch a single stop's timing or price without touching other stops.
     */
    @PatchMapping("/{id}/stops/{stopSequence}")
    public ResponseEntity<ApiResponse<BusDto>> updateStop(
            @PathVariable int id,
            @PathVariable int stopSequence,
            @RequestBody BusStopRequest request) {
        log.info("PATCH /api/buses/{}/stops/{}", id, stopSequence);
        BusDto updated = busService.updateStop(id, stopSequence, request);
        return ResponseEntity.ok(ApiResponse.success("Stop updated successfully", updated));
    }

    // =========================================================================
    //  SEATS
    // =========================================================================

    /**
     * Returns seat grid for a bus on a given date, with booked/available status
     */
    @GetMapping("/available-seats")
    public ResponseEntity<ApiResponse<BusDto>> getSeatAvailability(
            @RequestParam int busId,
            @RequestParam String date,
            @RequestParam("boardingStopSequence") int boardingStopSequence,
            @RequestParam("alightingStopSequence") int alightingStopSequence) {
        log.info("GET /api/buses/{}/seats  date={}", busId, date);
        BusDto availability = busService.getBusInformationByDateAndRoute(busId, date,
                boardingStopSequence,alightingStopSequence);
        return ResponseEntity.ok(ApiResponse.success(availability));
    }

    /**
     * Admin overrides individual seat properties (disable a broken seat,
     */
    @PatchMapping("/{id}/seats/overrides")
    public ResponseEntity<ApiResponse<BusDto>> applySeatsOverride(
            @PathVariable int id,
            @RequestBody List<SeatOverrideRequest> overrides) {
        log.info("PATCH /api/buses/{}/seats/overrides  count={}", id, overrides.size());
        BusDto updated = busService.applySeatsOverride(id, overrides);
        return ResponseEntity.ok(ApiResponse.success("Seat overrides applied", updated));
    }

    // =========================================================================
    //  SEAT-TYPE PRICING
    // =========================================================================

    /**
     * Upserts seat-type pricing rules for a bus.
     */
    @PutMapping("/{id}/pricing")
    public ResponseEntity<ApiResponse<BusDto>> upsertPricing(
            @PathVariable int id,
            @RequestBody List<SeatTypePricingRequest> pricingRequests) {
        log.info("PUT /api/buses/{}/pricing  rules={}", id, pricingRequests.size());
        BusDto updated = busService.upsertSeatTypePricings(id, pricingRequests);
        return ResponseEntity.ok(ApiResponse.success("Pricing rules updated", updated));
    }

    // =========================================================================
    //  PRICE CALCULATION
    // =========================================================================

    /**
     * Computes exact ticket price for a given seat type and boarding→alighting segment.
     */
    @GetMapping("/{id}/price")
    public ResponseEntity<ApiResponse<TicketPriceCalculationResponse>> calculatePrice(
            @PathVariable int id,
            @RequestParam int boardingStop,
            @RequestParam int alightingStop,
            @RequestParam SeatType seatType,
            @RequestParam(defaultValue = "LOWER") DeckLevel deckLevel) {

        log.info("GET /api/buses/{}/price  stops={}->{} seat={}/{}", id,
                boardingStop, alightingStop, seatType, deckLevel);

        TicketPriceCalculationResponse result =
                busService.calculateTicketPrice(id, boardingStop, alightingStop, seatType, deckLevel);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // =========================================================================
    //  SEARCH
    // =========================================================================

    /**
     * Search buses for a specific travel date.
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<BusSearchResponseDto>>> searchBuses(
            @RequestParam String startPlace,
            @RequestParam String destination,
            @RequestParam String date) {
        log.info("GET /api/buses/search  from={} to={} date={}", startPlace, destination, date);
        List<BusSearchResponseDto> results = busService.searchBuses(startPlace, destination, date);
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    /**
     * Search buses by route only — no date filter.
     */
//    @GetMapping("/search/route")
//    public ResponseEntity<ApiResponse<List<BusSearchResponseDto>>> searchByRoute(
//            @RequestParam String from,
//            @RequestParam String to) {
//        log.info("GET /api/buses/search/route  from={} to={}", from, to);
//        List<BusSearchResponseDto> results = busService.searchBusesByRoute(from, to);
//        return ResponseEntity.ok(ApiResponse.success(results));
//    }

    // =========================================================================
    //  DRIVER
    // =========================================================================

    @PostMapping("/drivers")
    public ResponseEntity<ApiResponse<DriverDto>> addDriver(@RequestBody Driver driver) {
        log.info("POST /api/buses/drivers  name={}", driver.getName());
        DriverDto saved = busService.addDriver(driver);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Driver added successfully", saved));
    }

    @GetMapping("/drivers")
    public ResponseEntity<ApiResponse<List<DriverDto>>> getAllDrivers() {
        log.info("GET /api/buses/drivers");
        return ResponseEntity.ok(ApiResponse.success(busService.getAllDrivers()));
    }

    @GetMapping("/drivers/{id}")
    public ResponseEntity<ApiResponse<DriverDto>> getDriverById(@PathVariable int id) {
        log.info("GET /api/buses/drivers/{}", id);
        return ResponseEntity.ok(ApiResponse.success(busService.getDriverById(id)));
    }

    // =========================================================================
    //  CONDUCTOR
    // =========================================================================

    @PostMapping("/conductors")
    public ResponseEntity<ApiResponse<ConductorDto>> addConductor(@RequestBody Conductor conductor) {
        log.info("POST /api/buses/conductors  name={}", conductor.getName());
        ConductorDto saved = busService.addConductor(conductor);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Conductor added successfully", saved));
    }

    @GetMapping("/conductors")
    public ResponseEntity<ApiResponse<List<ConductorDto>>> getAllConductors() {
        log.info("GET /api/buses/conductors");
        return ResponseEntity.ok(ApiResponse.success(busService.getAllConductors()));
    }

    @GetMapping("/conductors/{id}")
    public ResponseEntity<ApiResponse<ConductorDto>> getConductorById(@PathVariable int id) {
        log.info("GET /api/buses/conductors/{}", id);
        return ResponseEntity.ok(ApiResponse.success(busService.getConductorById(id)));
    }
}