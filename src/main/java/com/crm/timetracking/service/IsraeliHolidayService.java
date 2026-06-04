package com.crm.timetracking.service;

import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar;
import com.crm.timetracking.entity.Holiday;
import com.crm.timetracking.repository.HolidayRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class IsraeliHolidayService {

    private final HolidayRepository holidayRepo;
    private static final BigDecimal FULL_DAY = new BigDecimal("8.00");
    private static final BigDecimal HALF_DAY = new BigDecimal("4.00");

    public IsraeliHolidayService(HolidayRepository holidayRepo) {
        this.holidayRepo = holidayRepo;
    }

    @Transactional
    public List<Holiday> generateHolidaysForYear(int gregorianYear) {
        holidayRepo.deleteByYearAndCountry((short) gregorianYear, "IL");
        holidayRepo.flush();

        List<Holiday> holidays = new ArrayList<>();
        LocalDate cursor = LocalDate.of(gregorianYear, 1, 1);
        LocalDate end    = LocalDate.of(gregorianYear + 1, 1, 1);

        while (cursor.isBefore(end)) {
            JewishCalendar jCal = new JewishCalendar();
            jCal.setGregorianDate(cursor.getYear(), cursor.getMonthValue() - 1, cursor.getDayOfMonth());
            jCal.setInIsrael(true);

            String name = resolveHolidayName(jCal);
            if (name != null) {
                BigDecimal credit = isErev(jCal) ? HALF_DAY : FULL_DAY;
                holidays.add(new Holiday(cursor, name, "PUBLIC", "IL", credit));
            }
            cursor = cursor.plusDays(1);
        }
        return holidayRepo.saveAll(holidays);
    }

    private String resolveHolidayName(JewishCalendar jCal) {
        return switch (jCal.getYomTovIndex()) {
            case JewishCalendar.ROSH_HASHANA       -> "Rosh Hashanah";
            case JewishCalendar.YOM_KIPPUR         -> "Yom Kippur";
            case JewishCalendar.SUCCOS             -> "Sukkot";
            case JewishCalendar.SHEMINI_ATZERES    -> "Shemini Atzeret / Simchat Torah";
            case JewishCalendar.PESACH             -> "Pesach";
            case JewishCalendar.SHAVUOS            -> "Shavuot";
            case JewishCalendar.YOM_HAATZMAUT      -> "Yom Ha'atzmaut";
            case JewishCalendar.YOM_HAZIKARON      -> "Yom HaZikaron";
            case JewishCalendar.EREV_ROSH_HASHANA  -> "Erev Rosh Hashanah";
            case JewishCalendar.EREV_YOM_KIPPUR    -> "Erev Yom Kippur";
            case JewishCalendar.EREV_PESACH        -> "Erev Pesach";
            case JewishCalendar.EREV_SUCCOS        -> "Erev Sukkot";
            case JewishCalendar.CHOL_HAMOED_PESACH -> "Chol HaMoed Pesach";
            case JewishCalendar.CHOL_HAMOED_SUCCOS -> "Chol HaMoed Sukkot";
            default -> null;
        };
    }

    private boolean isErev(JewishCalendar jCal) {
        int i = jCal.getYomTovIndex();
        return i == JewishCalendar.EREV_ROSH_HASHANA || i == JewishCalendar.EREV_YOM_KIPPUR
            || i == JewishCalendar.EREV_PESACH       || i == JewishCalendar.EREV_SUCCOS;
    }
}
