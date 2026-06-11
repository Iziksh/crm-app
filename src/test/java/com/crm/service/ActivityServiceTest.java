package com.crm.service;

import com.crm.domain.entity.Activity;
import com.crm.domain.entity.Contact;
import com.crm.domain.entity.User;
import com.crm.domain.enums.ActivityPriority;
import com.crm.domain.enums.ActivityStatus;
import com.crm.domain.enums.ActivityType;
import com.crm.dto.request.ActivityRequest;
import com.crm.dto.response.ActivityResponse;
import com.crm.exception.ResourceNotFoundException;
import com.crm.repository.AccountRepository;
import com.crm.repository.ActivityNoteRepository;
import com.crm.repository.ActivityRepository;
import com.crm.repository.ContactRepository;
import com.crm.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivityServiceTest {

    @Mock ActivityRepository activityRepository;
    @Mock ActivityNoteRepository noteRepository;
    @Mock UserRepository userRepository;
    @Mock AccountRepository accountRepository;
    @Mock ContactRepository contactRepository;
    @Mock NotificationService notificationService;
    @Mock CrmEventPublisher eventPublisher;
    @Mock EmailService emailService;
    @Mock EmailLocaleResolver emailLocaleResolver;

    @InjectMocks ActivityService activityService;

    // ── create ───────────────────────────────────────────────────────────────

    @Test
    void create_savesActivity_andReturnsResponse() {
        ActivityRequest req = new ActivityRequest(
                "Call client", null, ActivityType.CALL,
                ActivityStatus.OPEN, ActivityPriority.MEDIUM,
                LocalDate.now(), null, null, null);
        Activity saved = new Activity();
        saved.setTitle("Call client");
        saved.setType(ActivityType.CALL);
        when(activityRepository.save(any())).thenReturn(saved);

        ActivityResponse response = activityService.create(req, "admin");

        assertThat(response.title()).isEqualTo("Call client");
        verify(activityRepository).save(any());
    }

    @Test
    void create_sendsInAppNotification_whenAssigned() {
        User assignee = new User();
        assignee.setId(5L);
        assignee.setUsername("bob");
        assignee.setEmail(null); // no email — only in-app

        Activity saved = new Activity();
        saved.setTitle("Task");
        saved.setType(ActivityType.TASK);
        saved.setAssignedTo(assignee);

        when(activityRepository.save(any())).thenReturn(saved);

        activityService.create(
                new ActivityRequest("Task", null, ActivityType.TASK, ActivityStatus.OPEN,
                        ActivityPriority.LOW, null, null, null, null),
                "admin");

        verify(notificationService).notify(eq(5L), contains("Task"), eq("ACTIVITY"), any());
        verifyNoInteractions(emailService);
    }

    @Test
    void create_sendsEmailNotification_whenAssigneeHasEmail() {
        User assignee = new User();
        assignee.setId(5L);
        assignee.setEmail("bob@example.com");

        Activity saved = new Activity();
        saved.setTitle("Meeting prep");
        saved.setType(ActivityType.MEETING);
        saved.setAssignedTo(assignee);

        when(activityRepository.save(any())).thenReturn(saved);
        when(emailLocaleResolver.resolveForUser(assignee)).thenReturn(Locale.ENGLISH);

        activityService.create(
                new ActivityRequest("Meeting prep", null, ActivityType.MEETING,
                        ActivityStatus.OPEN, ActivityPriority.HIGH,
                        null, null, null, null),
                "admin");

        verify(emailService).sendActivityAssigned("bob@example.com", "Meeting prep", Locale.ENGLISH);
    }

    @Test
    void create_sendsEmailToContact_whenTypeIsEmail() {
        Contact contact = new Contact();
        contact.setEmail("customer@client.com");

        Activity saved = new Activity();
        saved.setTitle("Product update");
        saved.setDescription("Here are the details.");
        saved.setType(ActivityType.EMAIL);
        saved.setContact(contact);

        when(activityRepository.save(any())).thenReturn(saved);

        activityService.create(
                new ActivityRequest("Product update", "Here are the details.",
                        ActivityType.EMAIL, ActivityStatus.OPEN, ActivityPriority.MEDIUM,
                        null, null, null, null),
                "admin");

        verify(emailService).sendEmailActivity(
                "customer@client.com", "Product update", "Here are the details.");
    }

    @Test
    void create_doesNotSendEmail_forEmailType_whenContactHasNoEmail() {
        Contact contact = new Contact();
        contact.setEmail(null);

        Activity saved = new Activity();
        saved.setType(ActivityType.EMAIL);
        saved.setContact(contact);
        saved.setTitle("Msg");

        when(activityRepository.save(any())).thenReturn(saved);

        activityService.create(
                new ActivityRequest("Msg", null, ActivityType.EMAIL, ActivityStatus.OPEN,
                        ActivityPriority.MEDIUM, null, null, null, null),
                "admin");

        verify(emailService, never()).sendEmailActivity(any(), any(), any());
    }

    // ── findForCalendar ───────────────────────────────────────────────────────

    @Test
    void findForCalendar_delegatesToRepository() {
        LocalDate from = LocalDate.of(2026, 6, 1);
        LocalDate to = LocalDate.of(2026, 6, 30);
        when(activityRepository.findByDueDateBetweenAndTypeInOrderByDueDate(any(), any(), any()))
                .thenReturn(List.of());

        List<ActivityResponse> result = activityService.findForCalendar(from, to);

        assertThat(result).isEmpty();
        verify(activityRepository).findByDueDateBetweenAndTypeInOrderByDueDate(
                eq(from), eq(to), any());
    }

    // ── buildIcal ─────────────────────────────────────────────────────────────

    @Test
    void buildIcal_returnsValidIcalStructure() {
        when(activityRepository.findByDueDateBetweenAndTypeInOrderByDueDate(any(), any(), any()))
                .thenReturn(List.of());

        String ical = activityService.buildIcal();

        assertThat(ical).startsWith("BEGIN:VCALENDAR");
        assertThat(ical).endsWith("END:VCALENDAR");
        assertThat(ical).contains("VERSION:2.0");
    }

    @Test
    void buildIcal_includesActivityAsVevent() {
        Activity a = new Activity();
        a.setId(42L);
        a.setTitle("Team Meeting");
        a.setDueDate(LocalDate.of(2026, 7, 15));
        a.setStatus(ActivityStatus.OPEN);
        when(activityRepository.findByDueDateBetweenAndTypeInOrderByDueDate(any(), any(), any()))
                .thenReturn(List.of(a));

        String ical = activityService.buildIcal();

        assertThat(ical).contains("BEGIN:VEVENT");
        assertThat(ical).contains("SUMMARY:Team Meeting");
        assertThat(ical).contains("DTSTART;VALUE=DATE:20260715");
        assertThat(ical).contains("END:VEVENT");
    }

    // ── findById ─────────────────────────────────────────────────────────────

    @Test
    void findById_throwsNotFound_whenMissing() {
        when(activityRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> activityService.findById(99L));
    }
}
