package com.sideproject.search.history;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // findByConverstaionId... -> findByConversationId... 오타 수정
    List<ChatMessage> findByConversationIdOrderByCreatedAtAsc(String conversationId);
}
