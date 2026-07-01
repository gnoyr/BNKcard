package com.bnk.domain.user.mapper;

import com.bnk.domain.user.model.UserAddress;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * USER_ADDRESSES MyBatis Mapper
 * XML: resources/mappers/user/UserAddressMapper.xml
 *
 * address / address_detail 는 AesTypeHandler 로 암복호화(USERS.phone 패턴 동일).
 */
@Mapper
public interface UserAddressMapper {

    /** 사용자 주소 전체 목록 (논리 삭제 제외, 기본배송지 우선 → 최신순) */
    List<UserAddress> findAllByUserId(@Param("userId") Long userId);

    /** address_id + user_id 단건 (소유권 검증) */
    Optional<UserAddress> findByAddressIdAndUserId(
            @Param("addressId") Long addressId,
            @Param("userId")    Long userId
    );

    /** 활성 주소 수 (최대 개수 제한 체크) */
    int countActiveByUserId(@Param("userId") Long userId);

    /** INSERT — address/address_detail 는 XML 에서 AES 암호화 */
    int insert(UserAddress model);

    /** 별칭 수정 */
    int updateAlias(
            @Param("addressId") Long   addressId,
            @Param("userId")    Long   userId,
            @Param("alias")     String alias
    );

    /** 사용자의 모든 기본배송지 해제 (단일 기본배송지 보장용) */
    int clearDefault(@Param("userId") Long userId);

    /** 특정 주소를 기본배송지로 설정 */
    int setDefault(
            @Param("addressId") Long addressId,
            @Param("userId")    Long userId
    );

    /** 논리 삭제 (deleted_yn='Y', status_code='DISABLED', is_default='N') */
    int softDelete(
            @Param("addressId") Long addressId,
            @Param("userId")    Long userId
    );
}
