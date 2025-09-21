package com.sideproject.search.elastic.openai.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ChatMessageVO {
	public String role;
	public String content;
}