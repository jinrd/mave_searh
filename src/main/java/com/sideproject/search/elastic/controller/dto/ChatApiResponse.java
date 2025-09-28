package com.sideproject.search.elastic.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatApiResponse {
    private String answer;
    private String conversationId;
}
