package com.crm.timetracking.scheduler;

import com.crm.domain.entity.User;
import com.crm.repository.UserRepository;
import com.crm.timetracking.entity.Attendance;
import com.crm.timetracking.repository.AttendanceRepository;
import com.crm.timetracking.service.ExcelReportService;
import com.crm.timetracking.service.ReportEmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class MonthlyReportJob {

    private static final Logger log = LoggerFactory.getLogger(MonthlyReportJob.class);
    private static final ZoneId IL_ZONE = ZoneId.of("Asia/Jerusalem");

    private final AttendanceRepository attendanceRepo;
    private final UserRepository       userRepository;
    private final ExcelReportService   excelService;
    private final ReportEmailService   emailService;

    public MonthlyReportJob(AttendanceRepository attendanceRepo,
                            UserRepository userRepository,
                            ExcelReportService excelService,
                            ReportEmailService emailService) {
        this.attendanceRepo = attendanceRepo;
        this.userRepository = userRepository;
        this.excelService   = excelService;
        this.emailService   = emailService;
    }

    // Fires at 06:00 AM Israel time on the 1st of every month; processes previous month
    @Scheduled(cron = "0 0 6 1 * *", zone = "Asia/Jerusalem")
    public void generateAndSendMonthlyReport() {
        YearMonth lastMonth = YearMonth.now(IL_ZONE).minusMonths(1);
        OffsetDateTime from = lastMonth.atDay(1).atStartOfDay(IL_ZONE).toOffsetDateTime();
        OffsetDateTime to   = from.plusMonths(1);
        String label = lastMonth.toString();

        log.info("Monthly report job started for period {}", label);

        try {
            List<Attendance> records = attendanceRepo.findAllByPeriod(from, to);
            if (records.isEmpty()) {
                log.warn("No attendance records for {}. Skipping report.", label);
                return;
            }

            List<Long> userIds = records.stream().map(Attendance::getUserId).distinct().toList();
            Map<Long, String> names = userRepository.findAllById(userIds).stream()
                    .collect(Collectors.toMap(User::getId, User::getUsername));

            byte[] excel = excelService.generateMonthlyReport(records, names, label);
            emailService.sendMonthlyReport(excel, label);
            log.info("Monthly attendance report for {} sent to accountant.", label);

        } catch (Exception ex) {
            log.error("Monthly report job FAILED for {}: {}", label, ex.getMessage(), ex);
        }
    }
}
