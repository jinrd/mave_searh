package com.sideproject.search.elastic.domain.repository;

import java.util.List;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import com.sideproject.search.elastic.domain.model.CarManual;

public interface CarManualRepository extends ElasticsearchRepository<CarManual, String>{
/*
 * Spring Data Elasticsearch 를 통해 Elasticsearch 와 상호작용(CRUD)을 쉽게 할 수 있도록 도와준다.
 * 
 * ElasticsearchRepository 를 상속받는 것만으로도 save, findById, findAll, delete 등
 * 기본적인 데이터 처리 메소드를 자동으로 사용할 수 있게 된다.
 * 인터페이스를 통해 Elasticsearch 에 데이터를 저장하거나 검색할 것이다.
 */
	
	// title 또는 content 필드에 keyword 가 포함된 데이터를 검색하는 메소드
	/*
	 Spring Data Elasticsearch는 메소드 이름을 분석해서 자동으로 쿼리를 생성해 줍니다.
   	 findByTitleContainsOrContentContains는 title 필드 또는 content 필드에 특정 문자열이
     포함(CONTAINS)된 문서를 찾아달라는 의미의 쿼리로 자동 변환됩니다.
	 */
	List<CarManual> findByTitleContainsOrContentContains(String title, String content);

}
