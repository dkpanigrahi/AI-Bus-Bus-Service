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

    boolean existsByBusNo(String busNo);

    Optional<Bus> findByIdAndIsDeletedFalse(int id);

    Optional<Bus> findByBusNoAndIsDeletedFalse(String busNo);

    List<Bus> findAllByIsDeletedFalse();

    /**
     * Finds active buses where:
     *   - at least one stop's city matches fromCity
     *   - at least one stop's city matches toCity
     *   - the bus runs on the given day (availableEveryDay=true OR specificDays contains dayOfWeek)
     *
     * The sequence order check (boarding before alighting) is enforced in the
     * service layer after fetching, since JPQL ordering comparison on two joined
     * rows requires a correlated subquery which is cleaner in Java.
     */
    @Query("""
            SELECT DISTINCT b FROM Bus b
            JOIN b.stops fromStop
            JOIN b.stops toStop
            WHERE b.isDeleted = false
              AND b.activeStatus = true
              AND LOWER(fromStop.city) = LOWER(:fromCity)
              AND LOWER(toStop.city)   = LOWER(:toCity)
              AND (
                    b.availableEveryDay = true
                    OR :dayOfWeek MEMBER OF b.specificDays
                  )
            """)
    List<Bus> findBusesByStopCitiesAndDay(
            @Param("fromCity")  String fromCity,
            @Param("toCity")    String toCity,
            @Param("dayOfWeek") String dayOfWeek);

    /**
     * Same as above but without day-of-week filter — used for admin route view.
     */
    @Query("""
            SELECT DISTINCT b FROM Bus b
            JOIN b.stops fromStop
            JOIN b.stops toStop
            WHERE b.isDeleted = false
              AND LOWER(fromStop.city) = LOWER(:fromCity)
              AND LOWER(toStop.city)   = LOWER(:toCity)
            """)
    List<Bus> findBusesByStopCities(
            @Param("fromCity") String fromCity,
            @Param("toCity")   String toCity);
}