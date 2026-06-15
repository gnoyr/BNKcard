package com.bnk.domain.ipauth.mapper;

import com.bnk.domain.ipauth.model.UserTrustedIp;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * USER_TRUSTED_IPS MyBatis Mapper
 * XML: resources/mappers/ipauth/IpTrustMapper.xml
 *
 * 모든 IP 조회는 ip_address_hash(SHA-256) 기반.
 * ip_address 직접 비교 불가 — AES-GCM IV가 매번 달라 암호문이 다름.
 */
@Mapper
public interface IpTrustMapper {

    /** 활성 신뢰 IP 단건 조회 — ip_address_hash 기반 */
    Optional<UserTrustedIp> findActiveByUserIdAndIpHash(
            @Param("userId")        Long   userId,
            @Param("ipAddressHash") String ipAddressHash
    );

    /** 사용자 신뢰 IP 전체 목록 (논리 삭제 제외, is_initial DESC → created_at ASC) */
    List<UserTrustedIp> findAllByUserId(@Param("userId") Long userId);

    /** trust_id + user_id 단건 (소유권 검증) */
    Optional<UserTrustedIp> findByTrustIdAndUserId(
            @Param("trustId") Long trustId,
            @Param("userId")  Long userId
    );

    /** 활성 신뢰 IP 수 (최대 10개 제한 체크) */
    int countActiveByUserId(@Param("userId") Long userId);

    /**
     * INSERT
     * ip_address      → typeHandler=AesTypeHandler (XML 명시) : AES 암호화
     * ip_address_hash → TypeHandler 없음                      : SHA-256 평문 저장
     */
    int insert(UserTrustedIp model);

    /** last_used_at 갱신 (신뢰 IP 로그인 통과 시) */
    int updateLastUsedAt(@Param("trustId") Long trustId);

    /** 별명 수정 */
    int updateNickname(
            @Param("trustId")  Long   trustId,
            @Param("userId")   Long   userId,
            @Param("nickname") String nickname
    );

    /** 논리 삭제 (deleted_yn='Y', status_code='DISABLED') */
    int softDelete(
            @Param("trustId") Long trustId,
            @Param("userId")  Long userId
    );

    /** 중복 IP 존재 여부 — ip_address_hash 기반 */
    boolean existsByUserIdAndIpHash(
            @Param("userId")        Long   userId,
            @Param("ipAddressHash") String ipAddressHash
    );
}
