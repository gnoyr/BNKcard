package com.bnk.domain.deviceauth.mapper;

import com.bnk.domain.deviceauth.model.UserTrustedDevice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * USER_TRUSTED_DEVICES MyBatis Mapper
 * XML: resources/mappers/deviceauth/DeviceTrustMapper.xml
 *
 * 모든 신뢰 판정은 device_id_hash(SHA-256) 기반.
 */
@Mapper
public interface DeviceTrustMapper {

    /** 활성 신뢰 기기 단건 조회 — device_id_hash 기반 */
    Optional<UserTrustedDevice> findActiveByUserIdAndDeviceHash(
            @Param("userId")       Long   userId,
            @Param("deviceIdHash") String deviceIdHash
    );

    /** 사용자 신뢰 기기 전체 목록 (논리 삭제 제외, is_initial DESC → created_at ASC) */
    List<UserTrustedDevice> findAllByUserId(@Param("userId") Long userId);

    /** device_trust_id + user_id 단건 (소유권 검증) */
    Optional<UserTrustedDevice> findByDeviceTrustIdAndUserId(
            @Param("deviceTrustId") Long deviceTrustId,
            @Param("userId")        Long userId
    );

    /** 활성 신뢰 기기 수 (최대 개수 제한 체크) */
    int countActiveByUserId(@Param("userId") Long userId);

    /** 중복 기기 존재 여부 — device_id_hash 기반 */
    boolean existsByUserIdAndDeviceHash(
            @Param("userId")       Long   userId,
            @Param("deviceIdHash") String deviceIdHash
    );

    /**
     * INSERT
     * last_ip_address → typeHandler=AesTypeHandler (XML 명시) : AES 암호화
     * device_id_hash / last_ip_hash → TypeHandler 없음        : SHA-256 평문 저장
     */
    int insert(UserTrustedDevice model);

    /** last_used_at 갱신 (신뢰 기기 로그인 통과 시) */
    int updateLastUsedAt(@Param("deviceTrustId") Long deviceTrustId);

    /** 마지막 접속 IP 갱신 (새 IP 접속 감지 후) */
    int updateLastIp(
            @Param("deviceTrustId") Long   deviceTrustId,
            @Param("lastIpAddress") String lastIpAddress,
            @Param("lastIpHash")    String lastIpHash
    );

    /** 기기명 수정 */
    int updateDeviceName(
            @Param("deviceTrustId") Long   deviceTrustId,
            @Param("userId")        Long   userId,
            @Param("deviceName")    String deviceName
    );

    /** 논리 삭제 (deleted_yn='Y', status_code='DISABLED') */
    int softDelete(
            @Param("deviceTrustId") Long deviceTrustId,
            @Param("userId")        Long userId
    );
}
