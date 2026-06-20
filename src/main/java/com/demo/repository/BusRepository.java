package com.demo.repository;

import com.demo.entity.Bus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BusRepository extends JpaRepository<com.demo.entity.Bus, Integer> {

    Optional<Bus> findByBusNo(String busNo);

    boolean existsByBusNo(String busNo);

    @Query("SELECT b FROM Bus b WHERE LOWER(b.startPlace) = LOWER(:startPlace) " +
           "AND LOWER(b.destination) = LOWER(:destination)")
    List<Bus> findByStartPlaceAndDestination(
            @Param("startPlace") String startPlace,
            @Param("destination") String destination);

    @Query("SELECT b FROM Bus b WHERE b.availableEveryDay = true OR :day MEMBER OF b.specificDays")
    List<Bus> findAvailableBusesOnDay(@Param("day") String day);

    @Query("SELECT b FROM Bus b WHERE LOWER(b.startPlace) = LOWER(:startPlace) " +
           "AND LOWER(b.destination) = LOWER(:destination) " +
           "AND (b.availableEveryDay = true OR :day MEMBER OF b.specificDays)")
    List<Bus> findAvailableBusesByRouteAndDay(
            @Param("startPlace") String startPlace,
            @Param("destination") String destination,
            @Param("day") String day);

    long countByCoach(String coach);
}