package com.bnk.domain.ai.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.bnk.domain.ai.dto.AiChatRequest;
import com.bnk.domain.ai.dto.AiChatResponse;
import com.bnk.domain.ai.mapper.AiChatLogMapper;
import com.bnk.domain.ai.model.AiChatLog;
import com.bnk.domain.spending.service.CardSearchService;
import com.bnk.global.log.annotation.Loggable;

import jakarta.validation.Valid;

import com.bnk.domain.ai.dto.AiChatHistoryResponse;

@Service
@Validated
@ConditionalOnProperty(name = "ai.enabled", havingValue = "true")
public class AiChatService {
	
	private final ChatClient chatClient;
    private final CardSearchService cardSearchService;
    private final AiChatLogMapper aiChatLogMapper;
    
    
    public AiChatService(@Autowired(required = false) ChatClient.Builder builder, CardSearchService cardSearchService, AiChatLogMapper aiChatLogMapper, @Autowired(required = false) ChatMemory chatMemory) {

        if (builder != null && chatMemory != null) {
            this.chatClient = builder
                .defaultSystem("""
                	    당신은 'BNK 부산은행 카드'의 전속 AI 상담원입니다.
                	    사용자의 질문에 답변할 때는 반드시 함께 제공된 [참고 정보]만을 바탕으로 답변해야 합니다.
                	    
                	    [절대 규칙: 위반 시 시스템 오류 발생]
                	    1. 당신은 오직 BNK 카드(부산은행/경남은행)에 대해서만 안내할 수 있습니다.
                	    2. 사용자가 타사 카드(예: 신한, 국민, 삼성, 현대, 롯데, 우리, 하나 등)에 대해 묻더라도, 타사 이름이나 관련 정보를 절대로 답변에 포함하지 마세요.
                	       -> 타사 카드를 물어볼 경우 무조건 이렇게만 답변하세요: "죄송합니다. 저는 BNK 카드 전속 상담원이므로 타사 카드 정보는 안내해 드릴 수 없습니다. BNK 카드 중에서 원하시는 혜택이 있다면 말씀해 주세요."
                	    3. 제공된 [참고 정보]에 내용이 없다면, 절대 당신의 사전 지식을 사용하여 답변을 지어내지 마세요.
                	    4. 혜택이나 연회비 등 수치 데이터는 [참고 정보]에 있는 그대로 정확하게 전달하세요.
                	    """)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
        } else {
            this.chatClient = null;
        }
        
        
        this.cardSearchService = cardSearchService;
        this.aiChatLogMapper = aiChatLogMapper;
    }
	
    @Transactional	
    @Loggable(eventType = "CHAT_QUERY", targetType = "CHAT", actionDetail = "챗봇질문")
	public AiChatResponse chat(@Valid AiChatRequest request, Long userId) {
        String userInput = request.getUserInput();

        
        List<Document> searchResults = cardSearchService.searchSimilarCards(userInput, 3);
        

        
        String context = searchResults.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n"));

        
        String result = chatClient.prompt()
                .user(u -> u.text("""
                        <context>
                        {context}
                        </context>
                        
                        <question>
                        {userInput}
                        </question>
                        """)
                            .param("context", context)
                            .param("userInput", userInput))
                .advisors(a -> a.param("chat_memory_conversation_id", request.getSessionId()))
                .call()
                .content();

		
        AiChatLog chatLog = AiChatLog.builder()
                .userId(userId)
                .sessionId(request.getSessionId())
                .userInput(userInput)
                .aiResponse(result)
                .build();

        
        aiChatLogMapper.insertChatLog(chatLog);

        return AiChatResponse.builder()
                .chatId(chatLog.getChatId())
                .sessionId(request.getSessionId())
                .response(result)
                .build();
    
	}
    
    @Transactional(readOnly = true)
    public List<AiChatHistoryResponse> getHistory(String sessionId) {
        return aiChatLogMapper.findBySessionId(sessionId)
                .stream()
                .map(log -> AiChatHistoryResponse.builder()
                        .chatId(log.getChatId())
                        .sessionId(log.getSessionId())
                        .userInput(log.getUserInput())
                        .aiResponse(log.getAiResponse())
                        .build())
                .toList();
    }
}
