package com.bnk.domain.ai.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.bnk.domain.ai.dto.AiChatRequest;
import com.bnk.domain.ai.dto.AiChatResponse;
import com.bnk.domain.ai.mapper.AiChatLogMapper;
import com.bnk.domain.ai.model.AiChatLog;
import com.bnk.domain.spending.service.CardSearchService;

import jakarta.validation.Valid;

@Service
@Validated
public class AiChatService {
	
	private final ChatClient chatClient;
    private final CardSearchService cardSearchService; // 1. 검색 서비스 주입 추가
    private final AiChatLogMapper aiChatLogMapper;
    
    // 2. 생성자를 통해 두 서비스 모두 주입받도록 수정
    public AiChatService(ChatClient.Builder builder, CardSearchService cardSearchService, AiChatLogMapper aiChatLogMapper) {
        this.chatClient = builder
            .defaultSystem("너는 한국어로 답변하는 친절한 어시스턴트다. 제공된 참고 정보를 바탕으로 정확하게 답변해줘.")
            .build();
        this.cardSearchService = cardSearchService;
        this.aiChatLogMapper = aiChatLogMapper;
    }
	
    @Transactional	
	public AiChatResponse chat(@Valid AiChatRequest request, Long userId) {
        String userInput = request.getUserInput();

        // 3. 사용자의 질문으로 관련 카드 정보 검색 (유사도 높은 상위 3개 추출 예시)
        List<Document> searchResults = cardSearchService.searchSimilarCards(userInput, 3);

        // 4. 검색된 문서들을 하나의 텍스트(Context)로 합치기
        String context = searchResults.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n"));

        // 5. 검색된 컨텍스트와 사용자의 원래 질문을 결합하여 프롬프트 생성
        String promptWithContext = String.format(
                "사용자 질문: %s\n\n[참고 정보]\n%s", 
                userInput, 
                context
        );

        // 6. 컨텍스트가 포함된 프롬프트로 AI에게 요청
		String result = chatClient.prompt()
				        .user(promptWithContext)
				        .call()
				        .content();

		// 2. 로그 엔티티 생성
        AiChatLog chatLog = AiChatLog.builder()
                .userId(userId)
                .sessionId(request.getSessionId())
                .userInput(userInput)
                .aiResponse(result)
                .build();

        // 3. DB 저장
        aiChatLogMapper.insertChatLog(chatLog);

        return AiChatResponse.builder()
                .chatId(chatLog.getChatId()) // 저장 후 생성된 ID 반환 가능
                .sessionId(request.getSessionId())
                .response(result)
                .build();
    
	}
}
