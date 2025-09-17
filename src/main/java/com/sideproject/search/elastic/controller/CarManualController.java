package com.sideproject.search.elastic.controller;

import com.sideproject.search.elastic.domain.model.CarManual;
import com.sideproject.search.elastic.domain.repository.CarManualRepository;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.query.StringQuery; // StringQuery를 import
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/manuals")
public class CarManualController {

    private final CarManualRepository carManualRepository;
    private final RestTemplate restTemplate;
    private final ElasticsearchOperations elasticsearchOperations;

    public CarManualController(CarManualRepository carManualRepository, RestTemplate restTemplate, ElasticsearchOperations elasticsearchOperations) {
        this.carManualRepository = carManualRepository;
        this.restTemplate = restTemplate;
        this.elasticsearchOperations = elasticsearchOperations;
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
    public List<CarManual> semanticSearch(@RequestParam("question") String question) {
        System.out.println("의미 기반 검색 질문: " + question);

        // 1. 질문 벡터 변환
        EmbeddingRequest request = new EmbeddingRequest(question);
        EmbeddingResponse response = restTemplate.postForObject("http://localhost:8000/embed", request, EmbeddingResponse.class);

        if (response == null || response.embedding == null) {
            return Collections.emptyList();
        }
        float[] queryVector = response.embedding;

        // 2. k-NN 쿼리를 JSON 문자열로 직접 생성
        String knnQueryJson = """
                {
                  "knn": {
                    "field": "contentVector",
                    "query_vector": %s,
                    "k": 3,
                    "num_candidates": 10
                  }
                }
                """.formatted(Arrays.toString(queryVector));

        // 3. StringQuery를 사용하여 검색 실행
        StringQuery stringQuery = new StringQuery(knnQueryJson);

        List<SearchHit<CarManual>> searchHits = elasticsearchOperations.search(stringQuery, CarManual.class).getSearchHits();

        return searchHits.stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());
    }
}
