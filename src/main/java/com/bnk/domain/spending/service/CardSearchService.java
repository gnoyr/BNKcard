package com.bnk.domain.spending.service;

import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CardSearchService {

    private final VectorStore vectorStore;

    // 유사 카드 검색
    public List<Document> searchSimilarCards(
            String query, int topK) {

        return vectorStore.similaritySearch(
            SearchRequest.builder()
            	.query(query)
                .topK(topK)
                .similarityThreshold(0.7)
                .build()
        );
    }
}
