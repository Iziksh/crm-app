package com.crm.service;

import com.crm.domain.entity.Subscription;
import com.crm.dto.request.SubscriptionRequest;
import com.crm.dto.response.SubscriptionResponse;
import com.crm.repository.SubscriptionRepository;
import com.crm.repository.TopicRepository;
import com.crm.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final TopicRepository topicRepository;
    private final UserRepository userRepository;

    public SubscriptionService(SubscriptionRepository subscriptionRepository,
                                TopicRepository topicRepository,
                                UserRepository userRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.topicRepository = topicRepository;
        this.userRepository = userRepository;
    }

    public SubscriptionResponse create(SubscriptionRequest request, String username) {
        Subscription sub = new Subscription();
        mapToEntity(sub, request);
        sub.setUser(userRepository.findByUsername(username)
                .orElseThrow(() -> new java.util.NoSuchElementException("User not found: " + username)));
        return SubscriptionResponse.from(subscriptionRepository.save(sub));
    }

    @Transactional(readOnly = true)
    public List<SubscriptionResponse> findAllForUser(String username) {
        return userRepository.findByUsername(username).map(user ->
                subscriptionRepository.findByUser_IdOrderByCreatedAtDesc(user.getId())
                        .stream().map(SubscriptionResponse::from).toList()
        ).orElse(List.of());
    }

    @Transactional(readOnly = true)
    public SubscriptionResponse findById(Long id) {
        return SubscriptionResponse.from(getOrThrow(id));
    }

    public SubscriptionResponse update(Long id, SubscriptionRequest request, String username) {
        Subscription sub = getOrThrow(id);
        mapToEntity(sub, request);
        return SubscriptionResponse.from(subscriptionRepository.save(sub));
    }

    public void delete(Long id) {
        subscriptionRepository.delete(getOrThrow(id));
    }

    private Subscription getOrThrow(Long id) {
        return subscriptionRepository.findById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException("Subscription not found: " + id));
    }

    private void mapToEntity(Subscription sub, SubscriptionRequest request) {
        sub.setName(request.name());
        sub.setDescription(request.description());
        sub.setActive(request.active());
        sub.setTopic(topicRepository.findById(request.topicId())
                .orElseThrow(() -> new java.util.NoSuchElementException("Topic not found: " + request.topicId())));
        sub.getEventTypes().clear();
        if (request.eventTypes() != null) sub.getEventTypes().addAll(request.eventTypes());
        sub.setFilterName0(request.filterName0()); sub.setFilterValue0(request.filterValue0());
        sub.setFilterName1(request.filterName1()); sub.setFilterValue1(request.filterValue1());
        sub.setFilterName2(request.filterName2()); sub.setFilterValue2(request.filterValue2());
        sub.setFilterName3(request.filterName3()); sub.setFilterValue3(request.filterValue3());
        sub.setFilterName4(request.filterName4()); sub.setFilterValue4(request.filterValue4());
    }
}
