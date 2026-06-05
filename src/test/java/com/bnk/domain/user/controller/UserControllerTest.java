package com.bnk.domain.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.bnk.domain.user.dto.response.CardStatusResponse;
import com.bnk.domain.user.dto.response.UserResponse;
import com.bnk.domain.user.service.UserService;
import com.bnk.global.auth.CustomUserDetails;
import com.bnk.global.exception.BusinessException;
import com.bnk.global.exception.ErrorCode;
import com.bnk.global.exception.GlobalExceptionHandler;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserController 단위 테스트")
class UserControllerTest {

    @Mock private UserService     userService;
    @InjectMocks private UserController userController;

    private MockMvc mvc;
    private CustomUserDetails mockUserDetails;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        // stub을 @BeforeEach에 두지 않음 → UnnecessaryStubbingException 방지
        //    (Bean Validation이 컨트롤러 진입 전에 막는 테스트에서 getUserId() 미호출)
        mockUserDetails = Mockito.mock(CustomUserDetails.class);

        HandlerMethodArgumentResolver resolver = new HandlerMethodArgumentResolver() {
            @Override public boolean supportsParameter(MethodParameter p) {
                return p.hasParameterAnnotation(AuthenticationPrincipal.class);
            }
            @Override public Object resolveArgument(MethodParameter p, ModelAndViewContainer mvc,
                                                    NativeWebRequest req, WebDataBinderFactory binder) {
                return mockUserDetails;
            }
        };

        mvc = MockMvcBuilders.standaloneSetup(userController)
                .setValidator(validator)
                .setCustomArgumentResolvers(resolver)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ════════════════════════════════════════════════════════════════
    // F-24 | 내 정보 조회
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("내 정보 조회 API [GET /api/users/me]")
    class GetMyInfo {

        @Test
        @DisplayName("[정상] 200 OK → userId·maskedEmail 포함 응답")
        void 정상_200() throws Exception {
            given(mockUserDetails.getUserId()).willReturn(1L);

            UserResponse response = UserResponse.builder()
                    .userId(1L)
                    .name("홍길동")
                    .maskedEmail("te**@test.com")
                    .maskedPhone("010-****-5678")
                    .build();
            given(userService.getMyInfo(1L)).willReturn(response);

            mvc.perform(get("/api/users/me"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.userId").value(1))
                    .andExpect(jsonPath("$.data.maskedEmail").value("te**@test.com"));
        }

        // ── [신규] 실패 케이스 ────────────────────────────────────

        @Test
        @DisplayName("[실패] 존재하지 않는 유저 → 404 + code=U001")
        void 실패_사용자없음_404() throws Exception {
            given(mockUserDetails.getUserId()).willReturn(1L);
            willThrow(new BusinessException(ErrorCode.USER_NOT_FOUND))
                    .given(userService).getMyInfo(1L);

            mvc.perform(get("/api/users/me"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("U001"));
        }
    }

 // ════════════════════════════════════════════════════════════════
    // F-25 | 내 정보 수정
    // 정책: 어떤 필드든 변경 시 currentPassword 검증 필수
    // ════════════════════════════════════════════════════════════════
 
    @Nested
    @DisplayName("내 정보 수정 API [PUT /api/users/me]")
    class UpdateMyInfo {
 
        @Test
        @DisplayName("[정상] 이름 + 비밀번호 재확인 → 200 OK")
        void 정상_200() throws Exception {
            given(mockUserDetails.getUserId()).willReturn(1L);
            willDoNothing().given(userService).updateMyInfo(anyLong(), any());
 
            mvc.perform(put("/api/users/me")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"이순신\",\"currentPassword\":\"Current123!\"}"))
                    .andExpect(status().isOk());
        }
 
        @Test
        @DisplayName("[실패] 존재하지 않는 유저 → 404 + code=U001")
        void 실패_사용자없음_404() throws Exception {
            given(mockUserDetails.getUserId()).willReturn(1L);
            willThrow(new BusinessException(ErrorCode.USER_NOT_FOUND))
                    .given(userService).updateMyInfo(anyLong(), any());
 
            mvc.perform(put("/api/users/me")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"이순신\",\"currentPassword\":\"Current123!\"}"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("U001"));
        }
 
        @Test
        @DisplayName("[실패] currentPassword 누락 → 400 + code=C001")
        void 실패_비밀번호누락_400() throws Exception {
            given(mockUserDetails.getUserId()).willReturn(1L);
            willThrow(new BusinessException(ErrorCode.INVALID_INPUT))
                    .given(userService).updateMyInfo(anyLong(), any());
 
            mvc.perform(put("/api/users/me")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"이순신\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("C001"));
        }
 
        @Test
        @DisplayName("[실패] 비밀번호 불일치 → 400 + code=U003")
        void 실패_비밀번호불일치_400() throws Exception {
            given(mockUserDetails.getUserId()).willReturn(1L);
            willThrow(new BusinessException(ErrorCode.INVALID_PASSWORD))
                    .given(userService).updateMyInfo(anyLong(), any());
 
            mvc.perform(put("/api/users/me")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"이순신\",\"currentPassword\":\"WrongPw!\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("U003"));
        }
    }

    // ════════════════════════════════════════════════════════════════
    // F-26 | 비밀번호 변경
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("비밀번호 변경 API [PATCH /api/users/me/password]")
    class ChangePassword {

        private static final String NEW_PW = "NewPassword123!";

        private String body(String current, String newPw, String confirm) {
            return "{\"currentPassword\":\"" + current +
                   "\",\"newPassword\":\"" + newPw +
                   "\",\"newPasswordConfirm\":\"" + confirm + "\"}";
        }

        @Test
        @DisplayName("[정상] 200 OK")
        void 정상_200() throws Exception {
            given(mockUserDetails.getUserId()).willReturn(1L);
            willDoNothing().given(userService).changePassword(anyLong(), any());

            mvc.perform(patch("/api/users/me/password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body("Current123!", NEW_PW, NEW_PW)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("[실패] 새 비밀번호 확인 불일치 → 400 + code=U009")
        void 실패_비밀번호확인불일치_400() throws Exception {
            given(mockUserDetails.getUserId()).willReturn(1L);
            willThrow(new BusinessException(ErrorCode.PASSWORD_CONFIRM_MISMATCH))
                    .given(userService).changePassword(anyLong(), any());

            mvc.perform(patch("/api/users/me/password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body("Current123!", NEW_PW, "WrongConfirm!")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("U009"));
        }

        @Test
        @DisplayName("[실패] 현재 비밀번호 불일치 → 400 + code=U003")
        void 실패_현재비밀번호불일치_400() throws Exception {
            given(mockUserDetails.getUserId()).willReturn(1L);
            willThrow(new BusinessException(ErrorCode.INVALID_PASSWORD))
                    .given(userService).changePassword(anyLong(), any());

            mvc.perform(patch("/api/users/me/password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body("WrongCurrent!", NEW_PW, NEW_PW)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("U003"));
        }

        @Test
        @DisplayName("[실패] currentPassword 누락(@NotBlank) → 400")
        void 실패_현재비밀번호누락_400() throws Exception {
            // ✅ @NotBlank 검증이 컨트롤러 진입 전에 막음 → getUserId() 미호출 → stub 불필요
            mvc.perform(patch("/api/users/me/password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"newPassword\":\"" + NEW_PW + "\",\"newPasswordConfirm\":\"" + NEW_PW + "\"}"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ════════════════════════════════════════════════════════════════
    // [신규] RQ-F17 | 보유 카드 및 신청 현황
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("보유 카드 및 신청 현황 API [GET /api/users/me/cards]")
    class GetMyCards {

        @Test
        @DisplayName("[정상] 200 OK → ownedCards·applications 포함 응답")
        void 정상_200() throws Exception {
            given(mockUserDetails.getUserId()).willReturn(1L);

            CardStatusResponse response = CardStatusResponse.builder()
                    .ownedCards(Collections.emptyList())
                    .applications(Collections.emptyList())
                    .build();
            given(userService.getMyCards(1L)).willReturn(response);

            mvc.perform(get("/api/users/me/cards"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.ownedCards").isArray())
                    .andExpect(jsonPath("$.data.applications").isArray());
        }

        @Test
        @DisplayName("[실패] 존재하지 않는 유저 → 404 + code=U001")
        void 실패_사용자없음_404() throws Exception {
            given(mockUserDetails.getUserId()).willReturn(1L);
            willThrow(new BusinessException(ErrorCode.USER_NOT_FOUND))
                    .given(userService).getMyCards(1L);

            mvc.perform(get("/api/users/me/cards"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("U001"));
        }
    }
}
