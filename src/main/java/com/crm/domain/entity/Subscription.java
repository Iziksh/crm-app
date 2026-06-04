package com.crm.domain.entity;

import com.crm.domain.enums.SubscriptionEventType;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "subscriptions")
@EntityListeners(AuditingEntityListener.class)
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    // Empty = match all event types (matches OpenCRX behaviour)
    @ElementCollection
    @CollectionTable(name = "subscription_event_types", joinColumns = @JoinColumn(name = "subscription_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type")
    private List<SubscriptionEventType> eventTypes = new ArrayList<>();

    // Up to 5 named attribute filters. Multiple values within a slot: OR. Multiple slots: AND.
    // Value prefixed with "!" means negation.
    private String filterName0; private String filterValue0;
    private String filterName1; private String filterValue1;
    private String filterName2; private String filterValue2;
    private String filterName3; private String filterValue3;
    private String filterName4; private String filterValue4;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Topic getTopic() { return topic; }
    public void setTopic(Topic topic) { this.topic = topic; }
    public List<SubscriptionEventType> getEventTypes() { return eventTypes; }
    public void setEventTypes(List<SubscriptionEventType> eventTypes) { this.eventTypes = eventTypes; }
    public String getFilterName0() { return filterName0; }
    public void setFilterName0(String filterName0) { this.filterName0 = filterName0; }
    public String getFilterValue0() { return filterValue0; }
    public void setFilterValue0(String filterValue0) { this.filterValue0 = filterValue0; }
    public String getFilterName1() { return filterName1; }
    public void setFilterName1(String filterName1) { this.filterName1 = filterName1; }
    public String getFilterValue1() { return filterValue1; }
    public void setFilterValue1(String filterValue1) { this.filterValue1 = filterValue1; }
    public String getFilterName2() { return filterName2; }
    public void setFilterName2(String filterName2) { this.filterName2 = filterName2; }
    public String getFilterValue2() { return filterValue2; }
    public void setFilterValue2(String filterValue2) { this.filterValue2 = filterValue2; }
    public String getFilterName3() { return filterName3; }
    public void setFilterName3(String filterName3) { this.filterName3 = filterName3; }
    public String getFilterValue3() { return filterValue3; }
    public void setFilterValue3(String filterValue3) { this.filterValue3 = filterValue3; }
    public String getFilterName4() { return filterName4; }
    public void setFilterName4(String filterName4) { this.filterName4 = filterName4; }
    public String getFilterValue4() { return filterValue4; }
    public void setFilterValue4(String filterValue4) { this.filterValue4 = filterValue4; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
