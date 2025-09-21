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
import com.sideproject.search.elastic.rag.service.RAGService;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;

@RestController
@RequestMapping("/api/manuals")
public class CarManualController {

    private final CarManualRepository carManualRepository;
    private final RAGService ragService;
    
    public CarManualController(CarManualRepository carManualRepository, RAGService ragService) {
        this.carManualRepository = carManualRepository;
        this.ragService = ragService;
    }

    @GetMapping("/search")
    public List<CarManual> searchManuals(@RequestParam("keyword") String keyword) {
        return carManualRepository.findByTitleContainsOrContentContains(keyword, keyword);
    }


    @GetMapping("/semantic-search")
    public String semanticSearch(@RequestParam("question") String question) throws IOException {
        return ragService.getanswer(question);
    }
}
