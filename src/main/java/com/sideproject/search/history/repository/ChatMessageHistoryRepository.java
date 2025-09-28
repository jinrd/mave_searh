package com.sideproject.search.history.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sideproject.search.history.vo.ChatMessageHistoryVO;

@Repository
public interface ChatMessageHistoryRepository extends JpaRepository<ChatMessageHistoryVO, Long>{
	    List<ChatMessageHistoryVO> findByConversationIdOrderByCreatedAtAsc(String conversationId);
}
