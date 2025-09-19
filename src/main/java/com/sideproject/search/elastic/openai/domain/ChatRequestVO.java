package com.sideproject.search.elastic.openai.domain;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ChatRequestVO {
	public String model;
	public List<ChatMessageVO> messages;
}
