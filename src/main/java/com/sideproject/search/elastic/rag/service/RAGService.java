package com.sideproject.search.elastic.rag.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.sideproject.search.elastic.domain.model.CarManual;
import com.sideproject.search.elastic.openai.domain.ChatMessageVO;
import com.sideproject.search.elastic.openai.domain.ChatRequestVO;
import com.sideproject.search.elastic.openai.domain.ChatResponseVO;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;

@Service
public class RAGService {

    @Value("${llm.api.url}")
    private String llmApiUrl;
    
    @Value("${llm.api.key}")
    private String llmApiKey;
    
    private final String EMBEDDING_URL = "http://localhost:8000/embed";
    
    private final RestTemplate restTemplate;
    private final ElasticsearchClient elasticsearchClient;
    
    private static class EmbeddingRequest {
        public String text;
        public EmbeddingRequest(String text) { this.text = text; }
    }

    private static class EmbeddingResponse {
        public float[] embedding;
    }
    
    public RAGService(RestTemplate restTemplate, ElasticsearchClient elasticsearchClient) {
    	this.restTemplate = restTemplate;
    	this.elasticsearchClient = elasticsearchClient;
	}
    
    public String getanswer(String question) throws IOException {
    	System.out.println("의미 기반 검색 질문 : " + question);
    	
        // 1. Get query vector from embedding model
    	EmbeddingRequest request = new EmbeddingRequest(question);
    	EmbeddingResponse response = restTemplate.postForObject(EMBEDDING_URL, request, EmbeddingResponse.class);
    	
    	if(response == null || response.embedding == null ) {
    		return "죄송합니다, 질문을 벡터로 변환하는 데 실패했습니다.";
    	}
    	
    	float[] queryVector = response.embedding;
    	
    	List<Float> queryVectorList = new ArrayList<>();
    	for(float f : queryVector) {
    		queryVectorList.add(f);
    	}
    	
    	// 2. Perform k-NN search in Elasticsearch
    	SearchRequest searchRequest = new SearchRequest.Builder()
    			.index("car-manuals")
    			.knn(k -> k.field("contentVector")
    					.queryVector(queryVectorList)
    					.k(2L)
    					.numCandidates(10L)).build();
    	
    	
    	SearchResponse<CarManual> searchResponse = elasticsearchClient.search(searchRequest, CarManual.class);
    	List<Hit<CarManual>> hits = searchResponse.hits().hits();
    	
    	if(hits.isEmpty()) {
    		return "죄송합니다. 관련 메뉴얼을 찾을 수 없습니다.";
    	}
    	
    	// 3. Augment: Create context from search results
    	String context = hits.stream().map(hit -> hit.source().getContent()).collect(Collectors.joining("\\n\\n---\\n\\n"));
    	
    	// 4. Generate: Build prompt and call LLM
    	String prompt = String.format(
        		"너는 자동차 전문가야. 아래의 매뉴얼 내용을 참고해서, 내 질문에 대해 친절하게 답변해줘.\n\n" +
		        "--- 참고 매뉴얼 ---\n%s\n\n" +
		        "--- 내 질문 ---\n%s",
		        context, question
        );
    	
    	List<ChatMessageVO> messages = List.of(new ChatMessageVO("user", prompt));
    	ChatRequestVO chatRequest = new ChatRequestVO("qwen2:1.5b", messages);
    	
    	HttpHeaders headers = new HttpHeaders();
    	headers.setContentType(MediaType.APPLICATION_JSON);
    	headers.setBearerAuth(llmApiKey);
    	
    	HttpEntity<ChatRequestVO> httpEntity = new HttpEntity<ChatRequestVO>(chatRequest, headers);
    	
    	try {
			ChatResponseVO chatResponse = restTemplate.postForObject(llmApiUrl, httpEntity, ChatResponseVO.class);
			
			if(chatResponse != null && !chatResponse.choices.isEmpty()) {
				return chatResponse.choices.get(0).message.content;
			} else {
				return "죄송합니다, AI로부터 답변을 생성하지 못했습니다.";
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			return "죄송합니다, AI 서비스 호출 중 오류가 발생했습니다.";
		}
    }
}
