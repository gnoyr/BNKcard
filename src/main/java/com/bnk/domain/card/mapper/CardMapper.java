package com.bnk.domain.card.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bnk.domain.card.dto.request.AdminCardSearchRequest;
import com.bnk.domain.card.dto.request.CardSearchRequest;
import com.bnk.domain.card.model.Card;

/**
 * CARDS 테이블 단일 Mapper (구 CardMapper + CardMapper2 통합)
 *
 * 변경 이력:
 *  - CardMapper2의 모든 메서드를 이 인터페이스로 흡수
 *  - 메서드명 네이밍 통일: get* → find*, expireCardVersion → expireCardVersions
 *  - 모든 파라미터 타입: com.bnk.domain.card.model.Card (단일 모델)
 *  - 삭제 대상: com.bnk.domain.card.mapper.CardMapper2
 */
@Mapper
public interface CardMapper {

    // ──────────────────────────────────────────────────────────────────
    // 사용자 / 관리자 조회 (기존 CardMapper 영역)
    // ──────────────────────────────────────────────────────────────────

    /** 사용자 카드 목록 + 동적 검색 (PUBLISHED, visible_yn='Y') */
    List<Card> findAll(CardSearchRequest request);

    long countAll(CardSearchRequest request);

    Card findById(@Param("cardId") Long cardId);

    List<Card> findTop3ByViewCount();

    List<Card> findTop3ByCategoryId(@Param("categoryId") Long categoryId);

    /** 관리자 다중 조건 동적 검색 */
    List<Card> findAdminCards(AdminCardSearchRequest request);

    long countAdminCards(AdminCardSearchRequest request);

    // ──────────────────────────────────────────────────────────────────
    // CUD (기존 CardMapper 영역)
    // ──────────────────────────────────────────────────────────────────

    int insertCard(Card card);

    int updateCard(Card card);

    /** 결재 승인 후 스냅샷 JSON으로 카드 상태만 APPROVED로 전환 */
    int updateCardFromSnapshot(@Param("cardId") Long cardId,
                               @Param("snapshotJson") String snapshotJson);

    int updateCardStatus(@Param("cardId") Long cardId,
                         @Param("cardStatus") String cardStatus);

    int incrementViewCount(@Param("cardId") Long cardId);

    int incrementApplicationCount(@Param("cardId") Long cardId);

    // ──────────────────────────────────────────────────────────────────
    // 스케줄러 / 관리자 상태 관리 (구 CardMapper2 영역 → 흡수)
    // ──────────────────────────────────────────────────────────────────

    /** 전체 카드 목록 (관리자용, deleted_yn='N') */
    List<Card> findAllCards();

    /** 카드 논리 삭제 (card_status=STOPPED, deleted_yn='Y') */
    void deleteCard(@Param("cardId") Long cardId);

    /** publish_end_at 초과 && PUBLISHED 상태 카드 — 만료 스케줄러용 */
    List<Card> findExpiredCards();

    /** publish_start_at 도달 && APPROVED 상태 카드 — 게시 스케줄러용 */
    List<Card> findApprovedReadyCards();

    /** APPROVED → PUBLISHED 일괄 전환 */
    void publishCards(@Param("cardIds") List<Long> cardIds);

    /** PUBLISHED 전환 시 CARD_VERSIONS도 PUBLISHED로 일괄 전환 */
    void publishCardVersions(@Param("cardIds") List<Long> cardIds);

    /** PUBLISHED → EXPIRED 일괄 전환 */
    void expireCards(@Param("cardIds") List<Long> cardIds);

    /** EXPIRED 전환 시 CARD_VERSIONS도 ARCHIVED로 일괄 전환 */
    void expireCardVersions(@Param("cardIds") List<Long> cardIds);
}
