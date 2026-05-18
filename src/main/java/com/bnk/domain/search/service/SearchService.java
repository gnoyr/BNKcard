package com.bnk.domain.search.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.bnk.domain.search.dto.response.PopularKeywordResponse;
import com.bnk.domain.search.dto.response.SearchResponse;
import com.bnk.domain.search.model.SearchKeyword;
import com.bnk.global.response.PageResponse;

@Service
public class SearchService {

	public List<PopularKeywordResponse> getPopularKeywords() {
		// TODO Auto-generated method stub
		return null;
	}

	public List<SearchKeyword> getSuggestKeywords() {
		// TODO Auto-generated method stub
		return null;
	}

	public PageResponse<SearchResponse> search(String q, Long userId, int page, int size) {
		// TODO Auto-generated method stub
		return null;
	}

}
