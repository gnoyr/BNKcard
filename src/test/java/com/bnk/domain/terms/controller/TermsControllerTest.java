package com.bnk.domain.terms.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.List;

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
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.bnk.domain.terms.dto.response.TermsFileResponse;
import com.bnk.domain.terms.dto.response.TermsPackageResponse;
import com.bnk.domain.terms.service.TermsService;
import com.bnk.global.auth.CustomUserDetails;
import com.bnk.global.exception.BusinessException;
import com.bnk.global.exception.ErrorCode;
import com.bnk.global.exception.GlobalExceptionHandler;
import com.bnk.global.util.audit.AuditLogger;

/**
 * TermsController 단위 테스트 (SonarQube 커버리지 대상)
 * <p>
 * standaloneSetup 기반 — Spring Context 없이 경량 실행.
 * GlobalExceptionHandler 등록으로 BusinessException 응답코드 검증 가능.
 * </p>
 * 커버 대상:
 * GET /api/terms/packages/{packageType},
 * POST /api/terms/agree,
 * GET /api/terms/{termsId}/files
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TermsController 단위 테스트")
class TermsControllerTest {

    @Mock
    private TermsService termsService;

    @Mock
    private AuditLogger auditLogger;

    @InjectMocks
    private TermsController termsController;

    private MockMvc mockMvc;

    private static final HandlerMethodArgumentResolver USER_DETAILS_RESOLVER =
            new HandlerMethodArgumentResolver() {
                @Override
                public boolean supportsParameter(MethodParameter parameter) {
                    return parameter.hasParameterAnnotation(AuthenticationPrincipal.class)
                            && parameter.getParameterType().isAssignableFrom(CustomUserDetails.class);
                }

                @Override
                public Object resolveArgument(MethodParameter parameter,
                        ModelAndViewContainer mavContainer,
                        NativeWebRequest webRequest,
                        WebDataBinderFactory binderFactory) {
                    CustomUserDetails mock = Mockito.mock(CustomUserDetails.class);
                    Mockito.when(mock.getUserId()).thenReturn(1L);
                    return mock;
                }
            };

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(termsController)
                .setControllerAdvice(new GlobalExceptionHandler(auditLogger))
                .setCustomArgumentResolvers(USER_DETAILS_RESOLVER)
                .build();
    }

    @Nested
    @DisplayName("약관 패키지 조회 [GET /api/terms/packages/{packageType}]")
    class GetTermsPackage {

        @Test
        @DisplayName("[정상] SIGNUP 패키지 → 200 & packageType 반환")
        void 정상_SIGNUP패키지() throws Exception {
            TermsPackageResponse.TermsItem item = TermsPackageResponse.TermsItem.builder()
                    .termsId(1L)
                    .title("개인정보 수집 동의")
                    .requiredYn("Y")
                    .build();

            TermsPackageResponse response = TermsPackageResponse.builder()
                    .packageType("SIGNUP")
                    .terms(List.of(item))
                    .build();

            given(termsService.getTermsPackage("SIGNUP")).willReturn(response);

            mockMvc.perform(get("/api/terms/packages/SIGNUP"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.packageType").value("SIGNUP"))
                    .andExpect(jsonPath("$.data.terms").isArray())
                    .andExpect(jsonPath("$.data.terms[0].termsId").value(1));
        }

        @Test
        @DisplayName("[정상] CARD_APPLY 패키지 → 200 & 빈 terms 배열 허용")
        void 정상_CARD_APPLY패키지_빈목록() throws Exception {
            TermsPackageResponse response = TermsPackageResponse.builder()
                    .packageType("CARD_APPLY")
                    .terms(Collections.emptyList())
                    .build();
            given(termsService.getTermsPackage("CARD_APPLY")).willReturn(response);

            mockMvc.perform(get("/api/terms/packages/CARD_APPLY"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.terms").isEmpty());
        }

        @Test
        @DisplayName("[예외] 존재하지 않는 패키지 → 404 TERMS_NOT_FOUND")
        void 예외_없는패키지_404() throws Exception {
            given(termsService.getTermsPackage("UNKNOWN"))
                    .willThrow(new BusinessException(ErrorCode.TERMS_NOT_FOUND));

            mockMvc.perform(get("/api/terms/packages/UNKNOWN"))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("약관 동의 처리 [POST /api/terms/agree]")
    class AgreeTerms {

        private static final String AGREE_URL = "/api/terms/agree";

        @Test
        @DisplayName("[정상] 전체 동의 → 201 & termsId 목록 반환")
        void 정상_전체동의_201() throws Exception {
            given(termsService.agreeTerms(any(), anyLong()))
                    .willReturn(List.of(1L, 2L));

            String body = """
                    {
                      "agreementSource": "SIGNUP",
                      "agreementChannel": "WEB",
                      "agreedTerms": [
                        { "termsId": 1, "agreedYn": "Y" },
                        { "termsId": 2, "agreedYn": "Y" }
                      ]
                    }
                    """;

            mockMvc.perform(post(AGREE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0]").value(1))
                    .andExpect(jsonPath("$.data[1]").value(2));
        }

        @Test
        @DisplayName("[예외] 필수 약관 미동의 → 400 REQUIRED_TERMS_NOT_AGREED")
        void 예외_필수미동의_400() throws Exception {
            given(termsService.agreeTerms(any(), anyLong()))
                    .willThrow(new BusinessException(ErrorCode.REQUIRED_TERMS_NOT_AGREED));

            String body = """
                    {
                      "agreementSource": "SIGNUP",
                      "agreementChannel": "WEB",
                      "agreedTerms": [
                        { "termsId": 1, "agreedYn": "N" }
                      ]
                    }
                    """;

            mockMvc.perform(post(AGREE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("[예외] agreementSource 누락(Bean Validation) → 400")
        void 예외_BeanValidation_source누락() throws Exception {
            String body = """
                    {
                      "agreementChannel": "WEB",
                      "agreedTerms": [
                        { "termsId": 1, "agreedYn": "Y" }
                      ]
                    }
                    """;

            mockMvc.perform(post(AGREE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("[예외] agreedTerms 빈 배열(Bean Validation) → 400")
        void 예외_BeanValidation_빈배열() throws Exception {
            String body = """
                    {
                      "agreementSource": "SIGNUP",
                      "agreementChannel": "WEB",
                      "agreedTerms": []
                    }
                    """;

            mockMvc.perform(post(AGREE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("[예외] 존재하지 않는 약관 → 404 TERMS_NOT_FOUND")
        void 예외_없는약관_404() throws Exception {
            given(termsService.agreeTerms(any(), anyLong()))
                    .willThrow(new BusinessException(ErrorCode.TERMS_NOT_FOUND));

            String body = """
                    {
                      "agreementSource": "SIGNUP",
                      "agreementChannel": "WEB",
                      "agreedTerms": [
                        { "termsId": 9999, "agreedYn": "Y" }
                      ]
                    }
                    """;

            mockMvc.perform(post(AGREE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("약관 파일 URL 조회 [GET /api/terms/{termsId}/files]")
    class GetTermsFiles {

        @Test
        @DisplayName("[정상] 유효한 termsId → 200 & 파일 목록 반환")
        void 정상_파일목록반환() throws Exception {
            TermsFileResponse fileResponse = TermsFileResponse.builder()
                    .fileId(100L)
                    .termsId(1L)
                    .fileType("PDF")
                    .filePath("https://cdn.example.com/terms/100.pdf")
                    .originalName("terms_service.pdf")
                    .fileExtension("pdf")
                    .fileSize(102400L)
                    .mimeType("application/pdf")
                    .isPrimary("Y")
                    .build();

            given(termsService.getTermsFiles(1L)).willReturn(List.of(fileResponse));

            mockMvc.perform(get("/api/terms/1/files"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].fileId").value(100))
                    .andExpect(jsonPath("$.data[0].mimeType").value("application/pdf"))
                    .andExpect(jsonPath("$.data[0].isPrimary").value("Y"));
        }

        @Test
        @DisplayName("[정상] 파일이 없는 약관 → 200 & 빈 배열")
        void 정상_파일없음_빈배열() throws Exception {
            given(termsService.getTermsFiles(2L)).willReturn(Collections.emptyList());

            mockMvc.perform(get("/api/terms/2/files"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isEmpty());
        }

        @Test
        @DisplayName("[예외] 존재하지 않는 termsId → 404 TERMS_NOT_FOUND")
        void 예외_없는약관_404() throws Exception {
            given(termsService.getTermsFiles(999L))
                    .willThrow(new BusinessException(ErrorCode.TERMS_NOT_FOUND));

            mockMvc.perform(get("/api/terms/999/files"))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("[정상] 다수 파일 → 200 & 모두 반환")
        void 정상_다수파일_반환() throws Exception {
            TermsFileResponse f1 = TermsFileResponse.builder()
                    .fileId(201L).termsId(3L).filePath("https://cdn.example.com/201.pdf")
                    .fileType("PDF").originalName("main.pdf").fileExtension("pdf")
                    .fileSize(50000L).mimeType("application/pdf").isPrimary("Y").build();
            TermsFileResponse f2 = TermsFileResponse.builder()
                    .fileId(202L).termsId(3L).filePath("https://cdn.example.com/202.pdf")
                    .fileType("PDF").originalName("sub.pdf").fileExtension("pdf")
                    .fileSize(30000L).mimeType("application/pdf").isPrimary("N").build();

            given(termsService.getTermsFiles(3L)).willReturn(List.of(f1, f2));

            mockMvc.perform(get("/api/terms/3/files"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(2));
        }
    }
}