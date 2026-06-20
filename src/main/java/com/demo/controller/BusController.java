package com.demo.controller;

import com.demo.dto.*;
import com.demo.entity.Bus;
import com.demo.entity.Conductor;
import com.demo.entity.Driver;
import com.demo.service.BusService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/buses")
@Slf4j
public class BusController {

    @Autowired
    private BusService busService;

    // ─── Bus Endpoints ─────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<ApiResponse<BusDto>> addBus(@RequestBody Bus bus) {
        log.info("Request to add new bus: {}", bus.getBusNo());
        Bus savedBus = busService.addBus(bus);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Bus added successfully", busService.mapToBusDto(savedBus)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BusDto>> getBusById(@PathVariable int id) {
        log.info("Request to fetch bus by ID: {}", id);
        Bus bus = busService.getBusById(id);
        return ResponseEntity.ok(ApiResponse.success(busService.mapToBusDto(bus)));
    }

    @GetMapping("/busno/{busNo}")
    public ResponseEntity<ApiResponse<BusDto>> getBusByBusNo(@PathVariable String busNo) {
        log.info("Request to fetch bus by busNo: {}", busNo);
        Bus bus = busService.getBusByBusNo(busNo);
        return ResponseEntity.ok(ApiResponse.success(busService.mapToBusDto(bus)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BusDto>>> getAllBuses() {
        log.info("Request to fetch all buses");
        List<BusDto> buses = busService.getAllBuses().stream()
                .map(busService::mapToBusDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(buses));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BusDto>> updateBus(
            @PathVariable int id,
            @RequestBody Bus bus) {
        log.info("Request to update bus with ID: {}", id);
        Bus updatedBus = busService.updateBus(id, bus);
        return ResponseEntity.ok(ApiResponse.success("Bus updated successfully", busService.mapToBusDto(updatedBus)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBus(@PathVariable int id) {
        log.info("Request to delete bus with ID: {}", id);
        busService.deleteBus(id);
        return ResponseEntity.ok(ApiResponse.success("Bus deleted successfully", null));
    }

    // ─── Search Endpoints ──────────────────────────────────────────

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<BusSearchResponseDto>>> searchBuses(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam String date) {
        log.info("Search request: {} -> {} on {}", from, to, date);
        List<BusSearchResponseDto> results = busService.searchBuses(from, to, date);
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    @GetMapping("/search/route")
    public ResponseEntity<ApiResponse<List<BusSearchResponseDto>>> searchByRoute(
            @RequestParam String from,
            @RequestParam String to) {
        log.info("Route search request: {} -> {}", from, to);
        List<BusSearchResponseDto> results = busService.searchBusesByRoute(from, to);
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    // ─── Seat Availability ─────────────────────────────────────────

    @GetMapping("/{id}/seats")
    public ResponseEntity<ApiResponse<SeatAvailabilityResponse>> getSeatAvailability(
            @PathVariable int id,
            @RequestParam String date) {
        log.info("Seat availability request for bus: {}, date: {}", id, date);
        SeatAvailabilityResponse availability = busService.getSeatAvailability(id, date);
        return ResponseEntity.ok(ApiResponse.success(availability));
    }

    // ─── Driver Endpoints ──────────────────────────────────────────

    @PostMapping("/drivers")
    public ResponseEntity<ApiResponse<DriverDto>> addDriver(@RequestBody Driver driver) {
        log.info("Request to add driver: {}", driver.getName());
        Driver saved = busService.addDriver(driver);
        DriverDto dto = DriverDto.builder()
                .id(saved.getId())
                .name(saved.getName())
                .phoneNo(saved.getPhoneNo())
                .salary(saved.getSalary())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Driver added successfully", dto));
    }

    @GetMapping("/drivers")
    public ResponseEntity<ApiResponse<List<DriverDto>>> getAllDrivers() {
        List<DriverDto> drivers = busService.getAllDrivers().stream()
                .map(d -> DriverDto.builder()
                        .id(d.getId()).name(d.getName())
                        .phoneNo(d.getPhoneNo()).salary(d.getSalary()).build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(drivers));
    }

    // ─── Conductor Endpoints ───────────────────────────────────────

    @PostMapping("/conductors")
    public ResponseEntity<ApiResponse<ConductorDto>> addConductor(@RequestBody Conductor conductor) {
        log.info("Request to add conductor: {}", conductor.getName());
        Conductor saved = busService.addConductor(conductor);
        ConductorDto dto = ConductorDto.builder()
                .id(saved.getId())
                .name(saved.getName())
                .phoneNo(saved.getPhoneNo())
                .salary(saved.getSalary())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Conductor added successfully", dto));
    }

    @GetMapping("/conductors")
    public ResponseEntity<ApiResponse<List<ConductorDto>>> getAllConductors() {
        List<ConductorDto> conductors = busService.getAllConductors().stream()
                .map(c -> ConductorDto.builder()
                        .id(c.getId()).name(c.getName())
                        .phoneNo(c.getPhoneNo()).salary(c.getSalary()).build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(conductors));
    }
}