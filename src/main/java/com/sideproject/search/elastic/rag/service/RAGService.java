package com.sideproject.search.elastic.rag.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.sideproject.search.elastic.controller.dto.ChatApiResponse;
import com.sideproject.search.elastic.domain.model.CarManual;
import com.sideproject.search.elastic.openai.domain.ChatMessageVO;
import com.sideproject.search.elastic.openai.domain.ChatRequestVO;
import com.sideproject.search.elastic.openai.domain.ChatResponseVO;
import com.sideproject.search.history.ChatMessage;
import com.sideproject.search.history.repository.ChatMessageHistoryRepository;
import com.sideproject.search.history.vo.ChatMessageHistoryVO;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.KnnSearch;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;

// RAG(검색 증강 생성) 파이프라인의 핵심 로직을 처리하는 서비스
@Service
public class RAGService {

    @Value("${llm.api.url}")
    private String llmApiUrl;

    @Value("${llm.api.key}")
    private String llmApiKey;

    @Value("${llm.model.version}")
    private String llmModelVersion;

    @Value("${embedding.api.url}")
    private String embeddingApiUrl;

    @Value("${elasticsearch.index.name}")
    private String esIndexName;

    private final RestTemplate restTemplate;
    private final ElasticsearchClient elasticsearchClient;
    private final ChatMessageHistoryRepository chatMessageHistoryRepository;

    public RAGService(RestTemplate restTemplate, ElasticsearchClient elasticsearchClient, ChatMessageHistoryRepository chatMessageHistoryRepository) {
        this.restTemplate = restTemplate;
        this.elasticsearchClient = elasticsearchClient;
        this.chatMessageHistoryRepository = chatMessageHistoryRepository;
    }
    private static class EmbeddingRequest {
        public String text;

        public EmbeddingRequest(String text) {
            this.text = text;
        }
    }

    private static class EmbeddingResponse {
        public float[] embedding;
    }
    /*
     *  RAG 파이프라인을 실행하여 사용자의 질문에 대한 답변을 생성한다.
     *  @param question 사용자 질문
     *  @param conversationId 대화 ID
     *  @return API 응답 (답변 + 대화 ID)
     */
    @Transactional
    public ChatApiResponse getAnswer(String question, String conversationId) throws IOException {
//    	 1. 대화 ID 가 없으면 새로 생김
    	final String currentConversationId = (conversationId == null || conversationId.isBlank()) ? UUID.randomUUID().toString() : conversationId;
    	
    	// 2. 사용자 질문을 DB 에 저장
    	saveMessage(currentConversationId, "user", question);
    	
    	// 3. 질문을 벡터로 변환 (임베딩)
    	EmbeddingResponse embeddingResponse = getEmbedding(question);
    	if(embeddingResponse == null || embeddingResponse.embedding == null) {
    		return new ChatApiResponse("죄송합니다, 질문을 벡터로 변환하는데 실패했습니다.", currentConversationId);
    	}
    	List<Float> queryVector = toFloatList(embeddingResponse.embedding);
    	
    	// 4. 하이브리드 검색으로 관련 문서 찾기 (Retrieval)
    	String retrievedDocs = hybridSearch(question, queryVector);
    	
    	// 5. 이전 대화 기록 조회
    	String history = getFormattedHistory(currentConversationId);
    	
    	// 6. LLM 에게 보낼 프롬프트 생성
    	String prompt = buildPrompt(history, retrievedDocs, question);
    	
    	// 7. LLM API 호출하여 답변 생성(Generation)
    	String answer = callLlm(prompt);
    	
    	saveMessage(currentConversationId, "bot", answer);
    	
    	return new ChatApiResponse(answer, currentConversationId);
    	
    }
    
    /**
     * 하이브리드 검색(키워드 + 벡터)을 수행하여 관련 문서 내용을 반환합니다.
     * @param question 사용자 질문 (키워드 검색용)
     * @param queryVector 질문의 벡터 (벡터 검색용)
     * @return 검색된 문서 내용들을 합친 문자열
    */
    private String hybridSearch(String question, List<Float> queryVector) throws IOException{
    	// 1. 키워드 검색을 위한 match 쿼리 정의
    	Query keywordQuery = Query.of(q -> q.match(m -> m.field("content").query(question)));
    	
    	// 2. 벡터 검색을 위한 'knn' wjddml
    	KnnSearch knnQuery = KnnSearch.of(k -> k.field("contentVector").queryVector(queryVector).k(5L).numCandidates(20L));
    	
    	// 3. 하이브리드 검색 요청 생성
    	SearchRequest searchRequest = new SearchRequest.Builder()
    											.index(esIndexName)
    											.query(keywordQuery) // 키워드 쿼리 적용
    											.knn(knnQuery)		// 벡터 쿼리 적용
    											.size(3)			// 최종적으로 받을 결과 문서의 수
    											.build();
    	
    	SearchResponse<CarManual> searchResponse = elasticsearchClient.search(searchRequest, CarManual.class);
    	return searchResponse.hits().hits().stream()
    			   .map(hit -> hit.source() != null ? hit.source().getContent() : "")
    			   .collect(Collectors.joining("\n\n---\n\n"));
    }
    
    /**
     * LLM API를 호출하여 답변을 생성합니다.
     * @param prompt LLM에게 전달할 전체 프롬프트
     * @return LLM이 생성한 답변 문자열
    */
    private String callLlm(String prompt) {
    	List<ChatMessageVO> message = List.of(new ChatMessageVO("user", prompt));
    	ChatRequestVO chatRequest = new ChatRequestVO(llmModelVersion, message);
    	
    	HttpHeaders headers = new HttpHeaders();
    	headers.setContentType(MediaType.APPLICATION_JSON);
    	headers.setBearerAuth(llmApiKey);
    	HttpEntity<ChatRequestVO> httpEntity = new HttpEntity<ChatRequestVO>(chatRequest, headers);
    	try {
    		ChatResponseVO chatResponse = restTemplate.postForObject(llmApiUrl, httpEntity, ChatResponseVO.class);
    		if(chatResponse != null && !chatResponse.choices.isEmpty()) {
    			return chatResponse.choices.get(0).message.content;
    		}
    	} catch (Exception e) {
    		e.printStackTrace();
		}
    	return "죄송합니다, AI로부터 답변을 생성하지 못했습니다.";
    	
    }
    
    private List<Float> toFloatList(float[] floatArray) {
    	List<Float> floatList = new ArrayList<>();
    	for(float f : floatArray) {
    		floatList.add(f);
    	}
    	return floatList;
    }

    
    private EmbeddingResponse getEmbedding(String text) {
    	EmbeddingRequest request = new EmbeddingRequest(text);
    	return restTemplate.postForObject(embeddingApiUrl, request, EmbeddingResponse.class);
    }
    
    private void saveMessage(String conversationId, String role, String content) {
    	ChatMessageHistoryVO message = ChatMessageHistoryVO.builder().conversationId(conversationId).role(role).content(content).build();
    	chatMessageHistoryRepository.save(message);
    }
    
    private String getFormattedHistory(String conversationId) {
    	List<ChatMessageHistoryVO> history = chatMessageHistoryRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
    	if(history.isEmpty()) {
    		return "아직 대화 기록이 없습니다.";
    	}
    	
    	// 마지막 질문은 제외하고 히스토리를 만듭니다 (이미 프롬프트에 포함되므로)
    	return history.stream().limit(history.size() - 1).map(msg -> (msg.getRole().equals("user") ? "사용자" : "챗봇")+ msg.getContent()).collect(Collectors.joining("\n"));
    }
    
    private String buildPrompt(String history, String context, String question) {
    	return String.format(
            "너는 자동차 전문가야. 아래의 매뉴얼 내용을 참고해서, 내 질문에 대해 친절하게 답변해줘.\n\n" +
    		"--- 이전 대 ---\n%s\n\n" +
            "--- 참고 매뉴얼 ---\n%s\n\n" +
            "--- 마지막 질문 ---\n%s", history, context, question
    	);
    }
    
}
