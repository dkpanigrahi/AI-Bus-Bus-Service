package com.demo.service;


import com.demo.client.BookingServiceClient;
import com.demo.dto.*;
import com.demo.entity.Bus;
import com.demo.entity.Conductor;
import com.demo.entity.Driver;
import com.demo.exception.ResourceNotFoundException;
import com.demo.repository.BusRepository;
import com.demo.repository.ConductorRepository;
import com.demo.repository.DriverRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BusService {

    @Autowired
    private BusRepository busRepository;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private ConductorRepository conductorRepository;

    @Autowired
    private BookingServiceClient bookingServiceClient;

    // ─── Bus CRUD ──────────────────────────────────────────────────

    @Transactional
    public Bus addBus(Bus bus) {
        log.info("Adding new bus with busNo: {}", bus.getBusNo());

        if (busRepository.existsByBusNo(bus.getBusNo())) {
            log.warn("Bus already exists with busNo: {}", bus.getBusNo());
            throw new RuntimeException("Bus with number " + bus.getBusNo() + " already exists");
        }

        Bus savedBus = busRepository.save(bus);
        log.info("Bus added successfully with ID: {}", savedBus.getId());
        return savedBus;
    }

    public Bus getBusById(int id) {
        log.debug("Fetching bus by ID: {}", id);
        return busRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bus", "id", id));
    }

    public Bus getBusByBusNo(String busNo) {
        log.debug("Fetching bus by busNo: {}", busNo);
        return busRepository.findByBusNo(busNo)
                .orElseThrow(() -> new ResourceNotFoundException("Bus", "busNo", busNo));
    }

    public List<Bus> getAllBuses() {
        log.info("Fetching all buses");
        return busRepository.findAll();
    }

    @Transactional
    public Bus updateBus(int id, Bus updatedBus) {
        log.info("Updating bus with ID: {}", id);
        Bus existingBus = getBusById(id);

        existingBus.setBusNo(updatedBus.getBusNo());
        existingBus.setStartPlace(updatedBus.getStartPlace());
        existingBus.setDestination(updatedBus.getDestination());
        existingBus.setDepartureTime(updatedBus.getDepartureTime());
        existingBus.setAvailableEveryDay(updatedBus.isAvailableEveryDay());
        existingBus.setCoach(updatedBus.getCoach());
        existingBus.setSpecificDays(updatedBus.getSpecificDays());
        existingBus.setTotalSeats(updatedBus.getTotalSeats());
        existingBus.setTicketPrice(updatedBus.getTicketPrice());
        existingBus.setDriver(updatedBus.getDriver());
        existingBus.setConductor(updatedBus.getConductor());

        Bus saved = busRepository.save(existingBus);
        log.info("Bus updated successfully: {}", id);
        return saved;
    }

    @Transactional
    public void deleteBus(int id) {
        log.info("Deleting bus with ID: {}", id);
        Bus bus = getBusById(id);
        busRepository.delete(bus);
        log.info("Bus deleted successfully: {}", id);
    }

    // ─── Search ────────────────────────────────────────────────────

    public List<BusSearchResponseDto> searchBuses(String from, String to, String date) {
        log.info("Searching buses from: {} to: {} on date: {}", from, to, date);

        String dayOfWeek = LocalDate.parse(date)
                .getDayOfWeek()
                .getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                .toUpperCase();

        List<Bus> buses = busRepository.findAvailableBusesByRouteAndDay(from, to, dayOfWeek);
        log.info("Found {} buses for route {} -> {} on {}", buses.size(), from, to, dayOfWeek);

        return buses.stream()
                .map(this::mapToBusSearchResponse)
                .collect(Collectors.toList());
    }

    public List<BusSearchResponseDto> searchBusesByRoute(String from, String to) {
        log.info("Searching buses by route from: {} to: {}", from, to);
        List<Bus> buses = busRepository.findByStartPlaceAndDestination(from, to);
        return buses.stream()
                .map(this::mapToBusSearchResponse)
                .collect(Collectors.toList());
    }

    // ─── Seat Availability (via FeignClient to Booking Service) ───

    public SeatAvailabilityResponse getSeatAvailability(int busId, String date) {
        log.info("Fetching seat availability for busId: {}, date: {}", busId, date);

        Bus bus = getBusById(busId);

        try {
            ResponseEntity<ApiResponse<SeatAvailabilityResponse>> response =
                    bookingServiceClient.getSeatAvailability(busId, date);

            if (response.getBody() != null && response.getBody().isSuccess()) {
                log.debug("Seat availability fetched from booking service for busId: {}", busId);
                return response.getBody().getData();
            }
        } catch (Exception e) {
            log.warn("Could not fetch seat availability from booking service for busId: {}. " +
                     "Returning default (all available). Error: {}", busId, e.getMessage());
        }

        // Fallback: return all seats as available
        Map<Integer, Boolean> defaultSeatMap = new HashMap<>();
        for (int i = 1; i <= bus.getTotalSeats(); i++) {
            defaultSeatMap.put(i, true);
        }
        return SeatAvailabilityResponse.builder()
                .busId(busId)
                .date(date)
                .seatMap(defaultSeatMap)
                .totalSeats(bus.getTotalSeats())
                .build();
    }

    // ─── Driver / Conductor ────────────────────────────────────────

    @Transactional
    public Driver addDriver(Driver driver) {
        log.info("Adding driver with phone: {}", driver.getPhoneNo());
        if (driverRepository.existsByPhoneNo(driver.getPhoneNo())) {
            throw new RuntimeException("Driver with phone " + driver.getPhoneNo() + " already exists");
        }
        return driverRepository.save(driver);
    }

    @Transactional
    public Conductor addConductor(Conductor conductor) {
        log.info("Adding conductor with phone: {}", conductor.getPhoneNo());
        if (conductorRepository.existsByPhoneNo(conductor.getPhoneNo())) {
            throw new RuntimeException("Conductor with phone " + conductor.getPhoneNo() + " already exists");
        }
        return conductorRepository.save(conductor);
    }

    public List<Driver> getAllDrivers() {
        return driverRepository.findAll();
    }

    public List<Conductor> getAllConductors() {
        return conductorRepository.findAll();
    }

    // ─── Mapping Helpers ───────────────────────────────────────────

    public BusDto mapToBusDto(Bus bus) {
        return BusDto.builder()
                .id(bus.getId())
                .busNo(bus.getBusNo())
                .startPlace(bus.getStartPlace())
                .destination(bus.getDestination())
                .departureTime(bus.getDepartureTime())
                .availableEveryDay(bus.isAvailableEveryDay())
                .coach(bus.getCoach())
                .specificDays(bus.getSpecificDays())
                .totalSeats(bus.getTotalSeats())
                .ticketPrice(bus.getTicketPrice())
                .driver(bus.getDriver() != null ? mapToDriverDto(bus.getDriver()) : null)
                .conductor(bus.getConductor() != null ? mapToConductorDto(bus.getConductor()) : null)
                .build();
    }

    public BusSearchResponseDto mapToBusSearchResponse(Bus bus) {
        return BusSearchResponseDto.builder()
                .id(bus.getId())
                .busNo(bus.getBusNo())
                .startPlace(bus.getStartPlace())
                .destination(bus.getDestination())
                .departureTime(bus.getDepartureTime())
                .coach(bus.getCoach())
                .totalSeats(bus.getTotalSeats())
                .ticketPrice(bus.getTicketPrice())
                .driverName(bus.getDriver() != null ? bus.getDriver().getName() : "N/A")
                .conductorName(bus.getConductor() != null ? bus.getConductor().getName() : "N/A")
                .build();
    }

    private DriverDto mapToDriverDto(Driver driver) {
        return DriverDto.builder()
                .id(driver.getId())
                .name(driver.getName())
                .phoneNo(driver.getPhoneNo())
                .salary(driver.getSalary())
                .build();
    }

    private ConductorDto mapToConductorDto(Conductor conductor) {
        return ConductorDto.builder()
                .id(conductor.getId())
                .name(conductor.getName())
                .phoneNo(conductor.getPhoneNo())
                .salary(conductor.getSalary())
                .build();
    }
}