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

    public ActivityService(ActivityRepository activityRepository,
                           ActivityNoteRepository noteRepository,
                           UserRepository userRepository,
                           AccountRepository accountRepository,
                           ContactRepository contactRepository) {
        this.activityRepository = activityRepository;
        this.noteRepository = noteRepository;
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.contactRepository = contactRepository;
    }

    public ActivityResponse create(ActivityRequest request, String createdByUsername) {
        Activity activity = mapToEntity(new Activity(), request);
        userRepository.findByUsername(createdByUsername)
                .ifPresent(activity::setCreatedBy);
        return ActivityResponse.from(activityRepository.save(activity));
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
        return ActivityResponse.from(activityRepository.save(mapToEntity(activity, request)));
    }

    public void delete(Long id) {
        activityRepository.delete(getOrThrow(id));
    }

    public ActivityResponse resolve(Long id) {
        Activity activity = getOrThrow(id);
        activity.setStatus(ActivityStatus.RESOLVED);
        activity.setResolvedAt(LocalDateTime.now());
        return ActivityResponse.from(activityRepository.save(activity));
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
