package com.sideproject.search.elastic.util;

import java.io.InputStream;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sideproject.search.elastic.domain.model.CarManual;
import com.sideproject.search.elastic.domain.repository.CarManualRepository;

@Component
public class DataLoader implements ApplicationRunner{

	private final CarManualRepository carManualRepository;
	private final ObjectMapper objectMapper;
	private final RestTemplate restTemplate;
	
	public DataLoader(CarManualRepository carManualRepository, ObjectMapper objectMapper, RestTemplate restTemplate) {
		this.carManualRepository = carManualRepository;
		this.objectMapper = objectMapper;
		this.restTemplate = restTemplate;
	}
	
	// AI 서버와 통신하기 위한 내부 클래스
	private static class EmbeddingRequest {
		public String text;
		public EmbeddingRequest(String text) {
			this.text = text;
		}
	}
	
	private static class EmbeddingResponse {
		public float[] embedding;
	}
	
	//ApplicationRunner: Spring Boot 애플리케이션이 시작된 후 run 메소드를 자동으로 실행해 줍니다.
	@Override
	public void run(ApplicationArguments args) throws Exception {
		// 테스트를 위해 매번 새로 인덱싱, 기존 데이터 삭제
		carManualRepository.deleteAll();
		
		// JSON 파일 읽기
		ClassPathResource resource = new ClassPathResource("data/sample-manuals.json");
		InputStream inputStream = resource.getInputStream();
		List<CarManual> carManuals = objectMapper.readValue(inputStream, new TypeReference<>() {});
		
		System.out.println("샘플 데이터의 벡터 임베딩을 생성한다.");
		
		// 각 메뉴얼에 대해 AI 서버를 호출하여 벡터를 받아옴
		for(CarManual manual : carManuals) {
			System.out.println("임베딩 생성 중 : " + manual.getTitle());
			
			// 1. 요청 본문 생성
			EmbeddingRequest request = new EmbeddingRequest(manual.getContent());
			
			// 2. Python AI 서버의 /embed 엔드포인트 호출
			EmbeddingResponse response = restTemplate.postForObject("http://localhost:8000/embed", request, EmbeddingResponse.class);
			
			// 3. 반환된 벡터를 메뉴얼 객체에 설정
			if(response != null && response.embedding != null) {
				manual.setContentVector(response.embedding);
			}
			
		}
		
		// 이제 벡터가 포함된 메뉴얼 데이터를 저장
		carManualRepository.saveAll(carManuals);
		
		// ElasticSearch 에 저장
		System.out.println("벡터 임베딩이 포함된 샘플 데이터가 Elasticsearch에 저장되었습니다.");		
	}

}
