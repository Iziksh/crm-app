package com.crm.service;

import com.crm.domain.entity.Activity;
import com.crm.domain.entity.ActivityNote;
import com.crm.domain.enums.ActivityStatus;
import com.crm.domain.enums.ActivityType;
import com.crm.dto.request.ActivityNoteRequest;
import com.crm.dto.request.ActivityRequest;
import com.crm.dto.response.ActivityNoteResponse;
import com.crm.dto.response.ActivityResponse;
import com.crm.exception.ResourceNotFoundException;
import com.crm.repository.AccountRepository;
import com.crm.repository.ActivityNoteRepository;
import com.crm.repository.ActivityRepository;
import com.crm.repository.ContactRepository;
import com.crm.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final ActivityNoteRepository noteRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final ContactRepository contactRepository;
    private final NotificationService notificationService;
    private final CrmEventPublisher eventPublisher;
    private final EmailService emailService;

    public ActivityService(ActivityRepository activityRepository,
                           ActivityNoteRepository noteRepository,
                           UserRepository userRepository,
                           AccountRepository accountRepository,
                           ContactRepository contactRepository,
                           NotificationService notificationService,
                           CrmEventPublisher eventPublisher,
                           EmailService emailService) {
        this.activityRepository = activityRepository;
        this.noteRepository = noteRepository;
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.contactRepository = contactRepository;
        this.notificationService = notificationService;
        this.eventPublisher = eventPublisher;
        this.emailService = emailService;
    }

    public ActivityResponse create(ActivityRequest request, String createdByUsername) {
        Activity activity = mapToEntity(new Activity(), request);
        userRepository.findByUsername(createdByUsername).ifPresent(activity::setCreatedBy);
        Activity saved = activityRepository.save(activity);
        if (saved.getAssignedTo() != null) {
            notificationService.notify(saved.getAssignedTo().getId(),
                    "New activity assigned: " + saved.getTitle(), "ACTIVITY", saved.getId());
            if (saved.getAssignedTo().getEmail() != null) {
                emailService.sendActivityAssigned(saved.getAssignedTo().getEmail(), saved.getTitle());
            }
        }
        // Phase 18: if EMAIL type, send actual email to the linked contact
        if (saved.getType() == ActivityType.EMAIL
                && saved.getContact() != null
                && saved.getContact().getEmail() != null) {
            emailService.sendEmailActivity(
                    saved.getContact().getEmail(),
                    saved.getTitle(),
                    saved.getDescription() != null ? saved.getDescription() : "");
        }
        return ActivityResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public ActivityResponse findById(Long id) {
        return ActivityResponse.from(getOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Page<ActivityResponse> findAll(Pageable pageable) {
        return activityRepository.findAll(pageable).map(ActivityResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<ActivityResponse> findAll(Pageable pageable, ActivityType type, ActivityStatus status, String search) {
        return activityRepository.findAll(buildSpec(type, status, search), pageable).map(ActivityResponse::from);
    }

    @Transactional(readOnly = true)
    public long count(ActivityType type, ActivityStatus status, String search) {
        return activityRepository.count(buildSpec(type, status, search));
    }

    private Specification<Activity> buildSpec(ActivityType type, ActivityStatus status, String search) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (type != null) predicates.add(cb.equal(root.get("type"), type));
            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            if (search != null && !search.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("title")), "%" + search.toLowerCase() + "%"));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    @Transactional(readOnly = true)
    public List<ActivityResponse> findAllForExport(ActivityType type, ActivityStatus status, String search) {
        return activityRepository.findAll(buildSpec(type, status, search)).stream().map(ActivityResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<ActivityResponse> findByType(ActivityType type) {
        return activityRepository.findByType(type).stream().map(ActivityResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<ActivityResponse> findByStatus(ActivityStatus status) {
        return activityRepository.findByStatus(status).stream().map(ActivityResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public long countByStatus(ActivityStatus status) {
        return activityRepository.countByStatus(status);
    }

    public ActivityResponse update(Long id, ActivityRequest request) {
        Activity activity = getOrThrow(id);
        ActivityResponse response = ActivityResponse.from(activityRepository.save(mapToEntity(activity, request)));
        eventPublisher.publishUpdated("ACTIVITY", id);
        return response;
    }

    public void delete(Long id) {
        activityRepository.delete(getOrThrow(id));
        eventPublisher.publishDeleted("ACTIVITY", id);
    }

    public ActivityResponse resolve(Long id) {
        Activity activity = getOrThrow(id);
        activity.setStatus(ActivityStatus.RESOLVED);
        activity.setResolvedAt(LocalDateTime.now());
        Activity saved = activityRepository.save(activity);
        if (saved.getCreatedBy() != null) {
            notificationService.notify(saved.getCreatedBy().getId(),
                    "Activity resolved: " + saved.getTitle(), "ACTIVITY", saved.getId());
        }
        return ActivityResponse.from(saved);
    }

    public ActivityResponse close(Long id) {
        Activity activity = getOrThrow(id);
        activity.setStatus(ActivityStatus.CLOSED);
        return ActivityResponse.from(activityRepository.save(activity));
    }

    public ActivityNoteResponse addNote(Long activityId, ActivityNoteRequest request, String authorUsername) {
        Activity activity = getOrThrow(activityId);
        ActivityNote note = new ActivityNote();
        note.setText(request.text());
        note.setActivity(activity);
        userRepository.findByUsername(authorUsername).ifPresent(note::setAuthor);
        return ActivityNoteResponse.from(noteRepository.save(note));
    }

    public void deleteNote(Long noteId) {
        noteRepository.delete(noteRepository.findById(noteId)
                .orElseThrow(() -> new ResourceNotFoundException("ActivityNote", "id", noteId)));
    }

    public ActivityResponse assign(Long activityId, String assignedToUsername) {
        Activity activity = getOrThrow(activityId);
        userRepository.findByUsername(assignedToUsername).ifPresent(activity::setAssignedTo);
        activity.setStatus(ActivityStatus.IN_PROGRESS);
        return ActivityResponse.from(activityRepository.save(activity));
    }

    public ActivityResponse reopen(Long activityId) {
        Activity activity = getOrThrow(activityId);
        activity.setStatus(ActivityStatus.IN_PROGRESS);
        activity.setResolvedAt(null);
        return ActivityResponse.from(activityRepository.save(activity));
    }

    @Transactional(readOnly = true)
    public List<ActivityResponse> findForCalendar(LocalDate from, LocalDate to) {
        return activityRepository.findByDueDateBetweenAndTypeInOrderByDueDate(
                from, to,
                List.of(ActivityType.MEETING, ActivityType.TASK, ActivityType.SALES_VISIT,
                        ActivityType.CALL, ActivityType.EMAIL))
                .stream().map(ActivityResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public String buildIcal() {
        LocalDate from = LocalDate.now();
        LocalDate to = from.plusDays(90);
        List<Activity> activities = activityRepository.findByDueDateBetweenAndTypeInOrderByDueDate(
                from, to,
                List.of(ActivityType.MEETING, ActivityType.TASK, ActivityType.SALES_VISIT));
        StringBuilder sb = new StringBuilder();
        sb.append("BEGIN:VCALENDAR\r\nVERSION:2.0\r\nPRODID:-//CRM//EN\r\nCALSCALE:GREGORIAN\r\n");
        for (Activity a : activities) {
            sb.append("BEGIN:VEVENT\r\n");
            sb.append("UID:crm-").append(a.getId()).append("@crm\r\n");
            if (a.getDueDate() != null) {
                String d = a.getDueDate().toString().replace("-", "");
                sb.append("DTSTART;VALUE=DATE:").append(d).append("\r\n");
                sb.append("DTEND;VALUE=DATE:").append(d).append("\r\n");
            }
            sb.append("SUMMARY:").append(ical(a.getTitle())).append("\r\n");
            if (a.getDescription() != null) sb.append("DESCRIPTION:").append(ical(a.getDescription())).append("\r\n");
            sb.append("STATUS:").append(a.getStatus() == ActivityStatus.RESOLVED ? "COMPLETED" : "CONFIRMED").append("\r\n");
            sb.append("END:VEVENT\r\n");
        }
        sb.append("END:VCALENDAR");
        return sb.toString();
    }

    private String ical(String text) {
        if (text == null) return "";
        return text.replace("\r\n", " ").replace("\n", " ").replace(",", "\\,").replace(";", "\\;");
    }

    private Activity getOrThrow(Long id) {
        return activityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Activity", "id", id));
    }

    private Activity mapToEntity(Activity activity, ActivityRequest request) {
        activity.setTitle(request.title());
        activity.setDescription(request.description());
        activity.setType(request.type());
        activity.setDueDate(request.dueDate());
        if (request.status() != null) activity.setStatus(request.status());
        if (request.priority() != null) activity.setPriority(request.priority());
        if (request.assignedToId() != null) {
            activity.setAssignedTo(userRepository.findById(request.assignedToId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.assignedToId())));
        } else {
            activity.setAssignedTo(null);
        }
        if (request.accountId() != null) {
            activity.setAccount(accountRepository.findById(request.accountId())
                    .orElseThrow(() -> new ResourceNotFoundException("Account", "id", request.accountId())));
        } else {
            activity.setAccount(null);
        }
        if (request.contactId() != null) {
            activity.setContact(contactRepository.findById(request.contactId())
                    .orElseThrow(() -> new ResourceNotFoundException("Contact", "id", request.contactId())));
        } else {
            activity.setContact(null);
        }
        return activity;
    }
}
