package com.bnk.global.auth;

import com.bnk.domain.admin.mapper.AdminUserMapper;
import com.bnk.domain.admin.model.AdminUser;
import com.bnk.global.exception.BusinessException;
import com.bnk.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminDetailsServiceImpl implements UserDetailsService {

    private final AdminUserMapper adminUserMapper;

    /** username(아이디) 기준 로드 */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AdminUser adminUser = adminUserMapper.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("관리자를 찾을 수 없습니다: " + username));
        return new CustomAdminDetails(adminUser);
    }

    /** JWT 필터에서 adminId 기준 로드 */
    public CustomAdminDetails loadUserById(Long adminId) {
        AdminUser adminUser = adminUserMapper.findById(adminId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_NOT_FOUND));
        return new CustomAdminDetails(adminUser);
    }
}
