package com.sideproject.search.elastic.domain.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Document(indexName = "car-manuals")
public class CarManual {
	
	@Id
	private String id;
	
	@Field(type = FieldType.Keyword)
	private String carModel;
	
	@Field(type = FieldType.Integer)
	private int year;

	@Field(type = FieldType.Keyword)
	private String category;
	
	@Field(type = FieldType.Text, analyzer = "nori")
	private String title;
	
	@Field(type = FieldType.Text, analyzer = "nori")
	private String content;

	// 하위 어노테이션은 Elasticsearch 에게 이 필드가 768 차원의 벡터 데이터라는 것을 알려준다.
	@Field(type = FieldType.Dense_Vector, dims = 768)
	private float[] contentVector;
	
	/*
	 FieldType.Text, analyzer = "nori": 전문(Full-text) 검색을 위한 필드입니다. nori 분석기를
     사용해 한국어 형태소를 분석하여 '주행'으로 검색해도 '주행하도록'이 포함된 결과를 찾을 수 있게 합니다.
	 */
}
