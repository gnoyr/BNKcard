package com.bnk.domain.user.service;

import com.bnk.domain.user.dto.request.AddressCreateRequest;
import com.bnk.domain.user.dto.response.AddressResponse;
import com.bnk.domain.user.mapper.UserAddressMapper;
import com.bnk.domain.user.model.UserAddress;
import com.bnk.global.exception.BusinessException;
import com.bnk.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 사용자 주소록(배송지) 관리 서비스.
 *
 * - 주소(address/address_detail)는 매퍼 XML 의 AesTypeHandler 로 자동 암복호화.
 * - 사용자당 최대 {@link #MAX_ADDRESS_COUNT}개.
 * - 기본 배송지(is_default='Y')는 사용자당 1건만 유지(설정 시 기존 해제 후 지정).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserAddressService {

    private static final int MAX_ADDRESS_COUNT = 10;

    private final UserAddressMapper addressMapper;

    @Transactional(readOnly = true)
    public List<AddressResponse> getAddresses(Long userId) {
        return addressMapper.findAllByUserId(userId).stream()
                .map(AddressResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public Long addAddress(Long userId, AddressCreateRequest req) {
        int count = addressMapper.countActiveByUserId(userId);
        if (count >= MAX_ADDRESS_COUNT) {
            throw new BusinessException(ErrorCode.ADDRESS_MAX_LIMIT_EXCEEDED);
        }

        // 등록된 주소가 없거나 명시적으로 요청하면 기본 배송지로 지정
        boolean makeDefault = Boolean.TRUE.equals(req.getSetDefault()) || count == 0;
        if (makeDefault) {
            addressMapper.clearDefault(userId);
        }

        UserAddress model = UserAddress.builder()
                .userId(userId)
                .alias((req.getAlias() == null || req.getAlias().isBlank()) ? "내 주소" : req.getAlias().trim())
                .zipcode(req.getZipcode())
                .address(req.getAddress().trim())
                .addressDetail(req.getAddressDetail())
                .isDefault(makeDefault ? "Y" : "N")
                .statusCode("ACTIVE")
                .build();

        addressMapper.insert(model);
        log.info("[UserAddress] userId={} 주소 등록 (default={})", userId, makeDefault);
        return model.getAddressId();
    }

    @Transactional
    public void updateAlias(Long userId, Long addressId, String alias) {
        int updated = addressMapper.updateAlias(addressId, userId, alias);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.ADDRESS_NOT_FOUND);
        }
    }

    @Transactional
    public void setDefault(Long userId, Long addressId) {
        addressMapper.findByAddressIdAndUserId(addressId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADDRESS_NOT_FOUND));
        addressMapper.clearDefault(userId);
        addressMapper.setDefault(addressId, userId);
        log.info("[UserAddress] userId={} 기본 배송지 변경 addressId={}", userId, addressId);
    }

    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        UserAddress target = addressMapper.findByAddressIdAndUserId(addressId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADDRESS_NOT_FOUND));

        addressMapper.softDelete(addressId, userId);

        // 기본 배송지를 삭제한 경우, 남은 주소 중 가장 최근 것을 기본배송지로 승격
        if (target.isDefaultAddress()) {
            List<UserAddress> remaining = addressMapper.findAllByUserId(userId);
            if (!remaining.isEmpty()) {
                addressMapper.setDefault(remaining.get(0).getAddressId(), userId);
            }
        }
        log.info("[UserAddress] userId={} 주소 삭제 addressId={}", userId, addressId);
    }
}
