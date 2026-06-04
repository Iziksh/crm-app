package com.crm.service;

import com.crm.domain.entity.Topic;
import com.crm.dto.response.TopicResponse;
import java.util.NoSuchElementException;
import com.crm.repository.TopicRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TopicServiceTest {

    @Mock TopicRepository topicRepository;

    @InjectMocks TopicService topicService;

    @Test
    void findAll_returnsAllTopics() {
        Topic t = new Topic();
        t.setName("New Lead Arrived");
        t.setTopicKey("L-01");
        when(topicRepository.findAll()).thenReturn(List.of(t));

        List<TopicResponse> result = topicService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("New Lead Arrived");
        assertThat(result.get(0).topicKey()).isEqualTo("L-01");
    }

    @Test
    void findAll_returnsEmptyList_whenNoTopics() {
        when(topicRepository.findAll()).thenReturn(List.of());

        assertThat(topicService.findAll()).isEmpty();
    }

    @Test
    void findById_throwsNotFound_whenMissing() {
        when(topicRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> topicService.findById(99L));
    }

    @Test
    void updateFlags_updatesTopicAndSaves() {
        Topic topic = new Topic();
        topic.setName("Alert Modifications");
        topic.setSendAlertEnabled(false);
        topic.setSendMailEnabled(false);
        when(topicRepository.findById(1L)).thenReturn(Optional.of(topic));
        when(topicRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        TopicResponse response = topicService.updateFlags(1L, true, true);

        assertThat(response.sendAlertEnabled()).isTrue();
        assertThat(response.sendMailEnabled()).isTrue();
        verify(topicRepository).save(topic);
    }
}
