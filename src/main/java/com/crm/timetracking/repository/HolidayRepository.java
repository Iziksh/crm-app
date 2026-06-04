package com.crm.timetracking.repository;

import com.crm.timetracking.entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface HolidayRepository extends JpaRepository<Holiday, Long> {

    List<Holiday> findByYearAndCountry(Short year, String country);

    boolean existsByDateAndNameAndCountry(LocalDate date, String name, String country);

    void deleteByYearAndCountry(Short year, String country);

    List<Holiday> findByDateBetween(LocalDate from, LocalDate to);
}
