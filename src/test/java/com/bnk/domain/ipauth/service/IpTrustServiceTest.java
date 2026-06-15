package com.bnk.domain.ipauth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bnk.domain.ipauth.mapper.IpTrustMapper;
import com.bnk.domain.ipauth.model.UserTrustedIp;
import com.bnk.global.exception.BusinessException;
import com.bnk.global.util.IpHashUtil;
import com.bnk.global.util.TokenStore;
import com.bnk.global.util.audit.AuditLogger;

@ExtendWith(MockitoExtension.class)
@DisplayName("IpTrustService 단위 테스트")
class IpTrustServiceTest {

    @Mock IpTrustMapper ipTrustMapper;
    @Mock TokenStore    tokenStore;
    @Mock AuditLogger   auditLogger;
    @Mock IpHashUtil    ipHashUtil;

    @InjectMocks IpTrustService ipTrustService;

    private static final Long   USER_ID        = 42L;
    private static final String TRUSTED_IP     = "192.168.1.100";
    private static final String UNTRUSTED_IP   = "10.0.0.99";
    private static final String TRUSTED_HASH   = "aaa111hash";
    private static final String UNTRUSTED_HASH = "bbb222hash";

    @BeforeEach
    void setupHash() {
        lenient().when(ipHashUtil.hash(TRUSTED_IP)).thenReturn(TRUSTED_HASH);
        lenient().when(ipHashUtil.hash(UNTRUSTED_IP)).thenReturn(UNTRUSTED_HASH);
    }
    // ─── checkIp() ────────────────────────────────────────────────

    @Nested
    @DisplayName("checkIp()")
    class CheckIpTests {

        @Test
        @DisplayName("신뢰 IP → hash 기반 DB 조회 → empty + last_used_at 갱신")
        void 신뢰IP_정상로그인() {
            UserTrustedIp trusted = build(TRUSTED_IP, TRUSTED_HASH, "N");
            given(ipTrustMapper.findActiveByUserIdAndIpHash(USER_ID, TRUSTED_HASH))
                    .willReturn(Optional.of(trusted));

            var result = ipTrustService.checkIp(USER_ID, TRUSTED_IP);

            assertThat(result).isEmpty();
            then(ipTrustMapper).should().findActiveByUserIdAndIpHash(USER_ID, TRUSTED_HASH);
            then(ipTrustMapper).should().updateLastUsedAt(trusted.getTrustId());
            then(tokenStore).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("미등록 IP → challengeToken(hash) + Redis key=hash, value=평문IP, ttl=15분")
        void 미등록IP_챌린지발급() {
            given(ipTrustMapper.findActiveByUserIdAndIpHash(USER_ID, UNTRUSTED_HASH))
                    .willReturn(Optional.empty());

            var result = ipTrustService.checkIp(USER_ID, UNTRUSTED_IP);

            String expectedToken = "ip:challenge:" + USER_ID + ":" + UNTRUSTED_HASH;
            assertThat(result).hasValue(expectedToken);
            then(tokenStore).should().set(expectedToken, UNTRUSTED_IP, 15L);
        }
    }

    // ─── registerInitialIp() ──────────────────────────────────────

    @Nested
    @DisplayName("registerInitialIp()")
    class RegisterInitialIpTests {

        @Test
        @DisplayName("INSERT 시 ipAddress(평문) + ipAddressHash(SHA-256) 모두 세팅")
        void 최초IP등록() {
            ipTrustService.registerInitialIp(USER_ID, TRUSTED_IP);

            then(ipTrustMapper).should().insert(argThat(m ->
                    TRUSTED_IP.equals(m.getIpAddress()) &&
                    TRUSTED_HASH.equals(m.getIpAddressHash()) &&
                    "Y".equals(m.getIsInitial()) &&
                    "SIGNUP".equals(m.getRegisteredVia())
            ));
        }
    }

    // ─── approvePendingIp() ───────────────────────────────────────

    @Nested
    @DisplayName("approvePendingIp()")
    class ApprovePendingIpTests {

        @Test
        @DisplayName("hash 기반 중복 체크 + approved key=hash + challenge key 삭제")
        void 이메일인증_신뢰IP등록() {
            given(ipTrustMapper.countActiveByUserId(USER_ID)).willReturn(2);
            given(ipTrustMapper.existsByUserIdAndIpHash(USER_ID, UNTRUSTED_HASH)).willReturn(false);

            ipTrustService.approvePendingIp(USER_ID, UNTRUSTED_IP, "EMAIL_VERIFY", "집");

            then(ipTrustMapper).should().existsByUserIdAndIpHash(USER_ID, UNTRUSTED_HASH);
            then(ipTrustMapper).should().insert(argThat(m ->
                    UNTRUSTED_IP.equals(m.getIpAddress()) &&
                    UNTRUSTED_HASH.equals(m.getIpAddressHash()) &&
                    "EMAIL_VERIFY".equals(m.getRegisteredVia()) &&
                    "집".equals(m.getNickname())
            ));
            then(tokenStore).should().set(
                    "ip:approved:" + USER_ID + ":" + UNTRUSTED_HASH, "Y", 30L);
            then(tokenStore).should().delete(
                    "ip:challenge:" + USER_ID + ":" + UNTRUSTED_HASH);
        }

        @Test
        @DisplayName("최대 10개 초과 → IP006")
        void 최대개수초과() {
            given(ipTrustMapper.countActiveByUserId(USER_ID)).willReturn(10);
            assertThatThrownBy(() ->
                    ipTrustService.approvePendingIp(USER_ID, UNTRUSTED_IP, "CI_VERIFY", null))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo("IP006");
        }

        @Test
        @DisplayName("중복 IP → IP005")
        void 중복IP() {
            given(ipTrustMapper.countActiveByUserId(USER_ID)).willReturn(2);
            given(ipTrustMapper.existsByUserIdAndIpHash(USER_ID, UNTRUSTED_HASH)).willReturn(true);
            assertThatThrownBy(() ->
                    ipTrustService.approvePendingIp(USER_ID, UNTRUSTED_IP, "EMAIL_VERIFY", null))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo("IP005");
        }

        @Test
        @DisplayName("nickname null → '내 기기' 기본값")
        void 닉네임기본값() {
            given(ipTrustMapper.countActiveByUserId(USER_ID)).willReturn(1);
            given(ipTrustMapper.existsByUserIdAndIpHash(USER_ID, UNTRUSTED_HASH)).willReturn(false);
            ipTrustService.approvePendingIp(USER_ID, UNTRUSTED_IP, "CI_VERIFY", null);
            then(ipTrustMapper).should().insert(argThat(m -> "내 기기".equals(m.getNickname())));
        }
    }

    // ─── deleteTrustedIp() ────────────────────────────────────────

    @Nested
    @DisplayName("deleteTrustedIp()")
    class DeleteTests {

        @Test
        @DisplayName("최초 기기 삭제 → IP004")
        void 최초기기_삭제금지() {
            given(ipTrustMapper.findByTrustIdAndUserId(1L, USER_ID))
                    .willReturn(Optional.of(build(TRUSTED_IP, TRUSTED_HASH, "Y")));
            assertThatThrownBy(() -> ipTrustService.deleteTrustedIp(USER_ID, 1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo("IP004");
        }

        @Test
        @DisplayName("일반 기기 논리 삭제 성공")
        void 일반기기_삭제성공() {
            given(ipTrustMapper.findByTrustIdAndUserId(2L, USER_ID))
                    .willReturn(Optional.of(build(UNTRUSTED_IP, UNTRUSTED_HASH, "N")));
            ipTrustService.deleteTrustedIp(USER_ID, 2L);
            then(ipTrustMapper).should().softDelete(2L, USER_ID);
        }
    }

    // ─── validateChallengeToken() ─────────────────────────────────

    @Test
    @DisplayName("챌린지 토큰 만료(null) → IP001")
    void 챌린지토큰_만료() {
        String token = "ip:challenge:" + USER_ID + ":" + UNTRUSTED_HASH;
        given(tokenStore.get(token)).willReturn(null);
        assertThatThrownBy(() -> ipTrustService.validateChallengeToken(USER_ID, token))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                .isEqualTo("IP001");
    }

    @Test
    @DisplayName("챌린지 토큰 유효 → 평문 IP 반환")
    void 챌린지토큰_유효() {
        String token = "ip:challenge:" + USER_ID + ":" + UNTRUSTED_HASH;
        given(tokenStore.get(token)).willReturn(UNTRUSTED_IP);
        assertThat(ipTrustService.validateChallengeToken(USER_ID, token)).isEqualTo(UNTRUSTED_IP);
    }

    // ─── 픽스처 ──────────────────────────────────────────────────

    private UserTrustedIp build(String ip, String hash, String isInitial) {
        return UserTrustedIp.builder()
                .trustId(1L).userId(USER_ID)
                .ipAddress(ip).ipAddressHash(hash)
                .nickname("테스트 기기").isInitial(isInitial)
                .statusCode("ACTIVE").registeredVia("SIGNUP").deletedYn("N")
                .build();
    }
}