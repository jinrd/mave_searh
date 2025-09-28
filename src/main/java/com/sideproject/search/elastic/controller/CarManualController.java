package com.sideproject.search.elastic.controller;

import com.sideproject.search.elastic.controller.dto.ChatApiResponse;
import com.sideproject.search.elastic.domain.model.CarManual;
import com.sideproject.search.elastic.domain.repository.CarManualRepository;
import com.sideproject.search.elastic.rag.service.RAGService;
import java.io.IOException;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<ChatApiResponse> semanticSearch(
        @RequestParam("question") String question,
        @RequestParam(value = "conversationId", required = false) String conversationId
    ) throws IOException {
        ChatApiResponse response = ragService.getAnswer(question, conversationId);
        return ResponseEntity.ok(response);
    }
}
