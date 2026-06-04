package com.crm.dto.response;

import com.crm.domain.entity.Topic;

public record TopicResponse(
        Long id,
        String topicKey,
        String name,
        String entityType,
        boolean sendAlertEnabled,
        boolean sendMailEnabled
) {
    public static TopicResponse from(Topic t) {
        return new TopicResponse(t.getId(), t.getTopicKey(), t.getName(), t.getEntityType(),
                t.isSendAlertEnabled(), t.isSendMailEnabled());
    }
}
