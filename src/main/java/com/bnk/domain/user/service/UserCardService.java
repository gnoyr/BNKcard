package com.bnk.domain.user.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.bnk.domain.application.mapper.UserCardMapper;
import com.bnk.domain.application.model.UserCard;
import com.bnk.domain.card.dto.CardSnapshot;
import com.bnk.domain.card.mapper.CardVersionMapper;
import com.bnk.domain.card.model.CardVersion;
import com.bnk.domain.user.dto.request.UserCardUpdateRequest;
import com.bnk.domain.user.dto.response.OwnedCardDetailResponse;
import com.bnk.global.exception.BusinessException;
import com.bnk.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 보유 카드(USER_CARDS) 관리 서비스.
 *
 * 마이페이지 > 내 카드 > 카드 관리 화면의 부분 수정을 담당한다.
 * 모든 작업은 소유권(userId) 검증을 선행한다.
 */
@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class UserCardService {

    private final UserCardMapper   userCardMapper;
    private final CardVersionMapper cardVersionMapper;
    private final ObjectMapper     objectMapper;

    // ================================================================
    // 보유 카드 단건 조회
    // ================================================================
    @Transactional(readOnly = true)
    public OwnedCardDetailResponse getOwnedCard(Long userId, Long userCardId) {
        UserCard card = userCardMapper.findByIdAndUserId(userCardId, userId);
        if (card == null) {
            throw new BusinessException(ErrorCode.USER_CARD_NOT_FOUND);
        }
        return OwnedCardDetailResponse.from(card);
    }

    // ================================================================
    // 보유 카드 부분 수정
    // ================================================================
    @Transactional
    public OwnedCardDetailResponse updateOwnedCard(
            Long userId, Long userCardId, @Valid UserCardUpdateRequest req) {

        // 1) 소유권 검증
        UserCard card = userCardMapper.findByIdAndUserId(userCardId, userId);
        if (card == null) {
            throw new BusinessException(ErrorCode.USER_CARD_NOT_FOUND);
        }

        // 2) 변경 불가 상태 차단 (만료·재발급·분실 카드는 수정 불가)
        String cur = card.getCardStatus();
        if ("EXPIRED".equals(cur) || "REISSUED".equals(cur) || "LOST".equals(cur)) {
            throw new BusinessException(ErrorCode.USER_CARD_NOT_MODIFIABLE);
        }

        // 3) 일일 한도는 카드 버전의 최대한도를 초과할 수 없음
        if (req.getDailyLimitAmount() != null) {
            Long maxDaily = resolveMaxDailyLimit(card.getVersionId());
            if (maxDaily != null && req.getDailyLimitAmount() > maxDaily) {
                throw new BusinessException(ErrorCode.DAILY_LIMIT_EXCEEDED,
                        "최대 " + maxDaily + "원까지 설정할 수 있습니다.");
            }
        }

        // 4) 변경분만 담은 패치 객체 구성 (null 필드는 매퍼 <if> 에서 제외됨)
        UserCard patch = new UserCard();
        patch.setUserCardId(userCardId);
        patch.setUserId(userId);
        patch.setDailyLimitAmount(req.getDailyLimitAmount());
        patch.setMonthlyLimitAmount(req.getMonthlyLimitAmount());
        patch.setOverseasEnabledYn(req.getOverseasEnabledYn());
        patch.setContactlessEnabledYn(req.getContactlessEnabledYn());
        patch.setCardNickname(req.getCardNickname());
        patch.setPaymentDay(req.getPaymentDay());
        patch.setTxAlertType(req.getTxAlertType());
        patch.setStatementMethod(req.getStatementMethod());

        // 5) 상태 변경 시 usable_yn 동기화 (ACTIVE 만 사용 가능)
        if (req.getCardStatus() != null) {
            patch.setCardStatus(req.getCardStatus());
            patch.setUsableYn("ACTIVE".equals(req.getCardStatus()) ? "Y" : "N");
        }

        userCardMapper.updateOwnedCard(patch);

        // 6) 갱신된 최신 상태 반환
        UserCard updated = userCardMapper.findByIdAndUserId(userCardId, userId);
        return OwnedCardDetailResponse.from(updated);
    }

    // ================================================================
    // 내부 헬퍼
    // ================================================================

    /**
     * 카드 버전 스냅샷에서 최대 한도(creditLimitMax)를 추출한다.
     * 버전/스냅샷이 없거나 파싱 실패 시 null (= 한도 검증 생략).
     */
    private Long resolveMaxDailyLimit(Long versionId) {
        if (versionId == null) return null;
        try {
            CardVersion version = cardVersionMapper.getCardVersion(versionId);
            if (version == null || version.getSnapshotJson() == null) return null;
            CardSnapshot snapshot =
                    objectMapper.readValue(version.getSnapshotJson(), CardSnapshot.class);
            if (snapshot.getCard() == null) return null;
            return snapshot.getCard().getCreditLimitMax();
        } catch (Exception e) {
            log.warn("카드 버전 스냅샷 한도 파싱 실패 versionId={}", versionId, e);
            return null;
        }
    }
}
