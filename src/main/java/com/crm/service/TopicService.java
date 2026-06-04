package com.crm.service;

import com.crm.domain.entity.Topic;
import com.crm.dto.response.TopicResponse;
import com.crm.repository.TopicRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class TopicService {

    private final TopicRepository topicRepository;

    public TopicService(TopicRepository topicRepository) {
        this.topicRepository = topicRepository;
    }

    @Transactional(readOnly = true)
    public List<TopicResponse> findAll() {
        return topicRepository.findAll().stream().map(TopicResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public TopicResponse findById(Long id) {
        return TopicResponse.from(getOrThrow(id));
    }

    public TopicResponse updateFlags(Long id, boolean sendAlertEnabled, boolean sendMailEnabled) {
        Topic topic = getOrThrow(id);
        topic.setSendAlertEnabled(sendAlertEnabled);
        topic.setSendMailEnabled(sendMailEnabled);
        return TopicResponse.from(topicRepository.save(topic));
    }

    private Topic getOrThrow(Long id) {
        return topicRepository.findById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException("Topic not found: " + id));
    }
}
