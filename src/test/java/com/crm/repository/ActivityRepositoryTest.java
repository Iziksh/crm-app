package com.crm.repository;

import com.crm.domain.entity.Activity;
import com.crm.domain.enums.ActivityType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ActivityRepositoryTest {

    @Autowired ActivityRepository activityRepository;

    @Test
    void findForCalendar_returnsActivitiesInDateRangeOfCorrectTypes() {
        Activity meeting = activity("Team sync", ActivityType.MEETING, LocalDate.of(2026, 7, 10));
        Activity task    = activity("Send report", ActivityType.TASK, LocalDate.of(2026, 7, 20));
        Activity bug     = activity("Fix bug", ActivityType.BUG, LocalDate.of(2026, 7, 15));
        activityRepository.saveAll(List.of(meeting, task, bug));

        List<Activity> result = activityRepository.findByDueDateBetweenAndTypeInOrderByDueDate(
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31),
                List.of(ActivityType.MEETING, ActivityType.TASK, ActivityType.SALES_VISIT));

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Activity::getTitle)
                .containsExactly("Team sync", "Send report"); // ordered by dueDate
    }

    @Test
    void findForCalendar_excludesActivitiesOutsideDateRange() {
        Activity early = activity("Before range", ActivityType.MEETING, LocalDate.of(2026, 6, 30));
        Activity late  = activity("After range",  ActivityType.MEETING, LocalDate.of(2026, 8, 1));
        Activity inRange = activity("In range",   ActivityType.MEETING, LocalDate.of(2026, 7, 15));
        activityRepository.saveAll(List.of(early, late, inRange));

        List<Activity> result = activityRepository.findByDueDateBetweenAndTypeInOrderByDueDate(
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31),
                List.of(ActivityType.MEETING));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("In range");
    }

    @Test
    void findForCalendar_returnsEmptyList_whenNoMatchingActivities() {
        List<Activity> result = activityRepository.findByDueDateBetweenAndTypeInOrderByDueDate(
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31),
                List.of(ActivityType.MEETING, ActivityType.TASK));

        assertThat(result).isEmpty();
    }

    @Test
    void findForCalendar_resultsAreOrderedByDueDate() {
        activityRepository.saveAll(List.of(
                activity("Third",  ActivityType.TASK, LocalDate.of(2026, 7, 20)),
                activity("First",  ActivityType.TASK, LocalDate.of(2026, 7, 5)),
                activity("Second", ActivityType.TASK, LocalDate.of(2026, 7, 12))
        ));

        List<Activity> result = activityRepository.findByDueDateBetweenAndTypeInOrderByDueDate(
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31),
                List.of(ActivityType.TASK));

        assertThat(result).extracting(Activity::getTitle)
                .containsExactly("First", "Second", "Third");
    }

    private Activity activity(String title, ActivityType type, LocalDate dueDate) {
        Activity a = new Activity();
        a.setTitle(title);
        a.setType(type);
        a.setDueDate(dueDate);
        return a;
    }
}
