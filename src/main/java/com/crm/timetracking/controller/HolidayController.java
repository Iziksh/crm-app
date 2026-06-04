package com.crm.timetracking.controller;

import com.crm.timetracking.dto.HolidayResponse;
import com.crm.timetracking.repository.HolidayRepository;
import com.crm.timetracking.service.IsraeliHolidayService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/holidays")
public class HolidayController {

    private final HolidayRepository     holidayRepository;
    private final IsraeliHolidayService holidayService;

    public HolidayController(HolidayRepository holidayRepository,
                              IsraeliHolidayService holidayService) {
        this.holidayRepository = holidayRepository;
        this.holidayService    = holidayService;
    }

    @GetMapping
    public ResponseEntity<List<HolidayResponse>> list(
            @RequestParam(required = false) Short year,
            @RequestParam(defaultValue = "IL") String country) {
        short y = (year != null) ? year : (short) LocalDate.now().getYear();
        return ResponseEntity.ok(
                holidayRepository.findByYearAndCountry(y, country).stream()
                        .map(HolidayResponse::from).toList());
    }

    @PostMapping("/regenerate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> regenerate(@RequestParam int year) {
        int count = holidayService.generateHolidaysForYear(year).size();
        return ResponseEntity.ok("Regenerated " + count + " holidays for year " + year);
    }
}
