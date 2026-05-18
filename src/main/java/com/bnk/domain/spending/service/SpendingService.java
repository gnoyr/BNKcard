package com.bnk.domain.spending.service;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.bnk.domain.spending.dto.request.AiChatRequest;
import com.bnk.domain.spending.dto.request.SpendingPatternRequest;
import com.bnk.domain.spending.dto.response.AiChatResponse;
import com.bnk.domain.spending.dto.response.SpendingChartResponse;

import jakarta.validation.Valid;

@Service
@Validated
public class SpendingService {
	private final ChatClient chatClient;

	public SpendingService(ChatClient.Builder builder) {
        this.chatClient = builder
            .defaultSystem("너는 한국어로 답변하는 친절한 어시스턴트다.")
            .build();
    }
	
	public AiChatResponse chat(@Valid AiChatRequest request, Long userId) {
		String result = chatClient.prompt()
				        .user(request.getUserInput())
				        .call()
				        .content();
		AiChatResponse done = AiChatResponse.builder().chatId(null).sessionId(null).response(result).build();
		
        return done;
	}

	public List<SpendingChartResponse> getMySpendingPatterns(Long userId) {
		// TODO Auto-generated method stub
		return null;
	}

	public int updateSpendingPatterns(Long userId, @Valid SpendingPatternRequest request) {
		// TODO Auto-generated method stub
		return 0;
	}
	
}
