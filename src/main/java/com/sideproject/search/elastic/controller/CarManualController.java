package com.sideproject.search.elastic.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.sideproject.search.elastic.domain.model.CarManual;
import com.sideproject.search.elastic.domain.repository.CarManualRepository;
import com.sideproject.search.elastic.openai.domain.ChatMessageVO;
import com.sideproject.search.elastic.openai.domain.ChatRequestVO;
import com.sideproject.search.elastic.openai.domain.ChatResponseVO;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;

@RestController
@RequestMapping("/api/manuals")
public class CarManualController {

    private final CarManualRepository carManualRepository;
    private final RestTemplate restTemplate;
    private final ElasticsearchClient elasticsearchClient;
    
    @Value("${llm.api.url}")
    private String llmApiUrl;
    
    @Value("${llm.api.key}")
    private String llmApiKey;
    
    public CarManualController(CarManualRepository carManualRepository, RestTemplate restTemplate, ElasticsearchClient elasticsearchClient) {
        this.carManualRepository = carManualRepository;
        this.restTemplate = restTemplate;
        this.elasticsearchClient = elasticsearchClient;
    }

    @GetMapping("/search")
    public List<CarManual> searchManuals(@RequestParam("keyword") String keyword) {
        return carManualRepository.findByTitleContainsOrContentContains(keyword, keyword);
    }

    private static class EmbeddingRequest {
        public String text;
        public EmbeddingRequest(String text) { this.text = text; }
    }

    private static class EmbeddingResponse {
        public float[] embedding;
    }

    @GetMapping("/semantic-search")
    public String semanticSearch(@RequestParam("question") String question) throws IOException {
        System.out.println("의미 기반 검색 질문: " + question);

        EmbeddingRequest request = new EmbeddingRequest(question);
        EmbeddingResponse response = restTemplate.postForObject("http://localhost:8000/embed", request, EmbeddingResponse.class);

        if (response == null || response.embedding == null) {
//            return Collections.emptyList();
        	return "죄송합니다, 질문을 벡터로 변환하는 데 실패했습니다.";
        }
        float[] queryVector = response.embedding;

        // --- 에러 수정: float[]를 List<Float>으로 변환 ---
        List<Float> queryVectorList = new ArrayList<>();
        for (float f : queryVector) {
        queryVectorList.add(f);
        }

     // 2. Perform k-NN search in Elasticsearch

        SearchRequest searchRequest = new SearchRequest.Builder()
                .index("car-manuals")
                .knn(k -> k
                        .field("contentVector")
                        .queryVector(queryVectorList) // 수정된 리스트 사용
                        .k(2L)
                        .numCandidates(10L)
                )
                .build();

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

        // System prompt + User Prompt
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
        
//        return searchResponse.hits().hits().stream()
//                .map(Hit::source)
//                .collect(Collectors.toList());
    }
}
