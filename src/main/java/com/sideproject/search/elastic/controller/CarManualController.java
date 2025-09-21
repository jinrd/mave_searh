package com.sideproject.search.elastic.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sideproject.search.elastic.domain.model.CarManual;
import com.sideproject.search.elastic.domain.repository.CarManualRepository;
import com.sideproject.search.elastic.rag.service.RAGService;

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
        return ragService.getAnswer(question);
    }
}
