package com.bnk.global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Firebase Admin SDK 초기화.
 *
 * <p>application.properties 에서 {@code fcm.enabled=true} 일 때만 빈이 등록된다.
 * 기본값(false)이면 FCM 관련 빈이 모두 비활성화되어 기존 stub 동작이 유지된다.</p>
 *
 * <p>서비스 계정 키 경로:
 * <ul>
 *   <li>{@code fcm.service-account-path} 프로퍼티에 절대경로 지정 시 해당 파일 사용</li>
 *   <li>미지정 시 classpath 의 {@code firebase/service-account.json} 사용</li>
 * </ul>
 * 키 파일은 절대 VCS에 커밋하지 말 것(.gitignore 등록 필수).</p>
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "fcm.enabled", havingValue = "true")
public class FcmConfig {

    @Value("${fcm.service-account-path:}")
    private String serviceAccountPath;

    @Bean
    FirebaseMessaging firebaseMessaging() throws IOException {
        try (InputStream serviceAccount = openServiceAccount()) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            FirebaseApp app = FirebaseApp.getApps().isEmpty()
                    ? FirebaseApp.initializeApp(options)
                    : FirebaseApp.getInstance();

            log.info("[FCM] FirebaseApp 초기화 완료: name={}", app.getName());
            return FirebaseMessaging.getInstance(app);
        }
    }

    private InputStream openServiceAccount() throws IOException {
        if (serviceAccountPath != null && !serviceAccountPath.isBlank()) {
            log.info("[FCM] 서비스 계정 키 로드(파일): {}", serviceAccountPath);
            return new FileInputStream(serviceAccountPath);
        }
        log.info("[FCM] 서비스 계정 키 로드(classpath): firebase/service-account.json");
        return new ClassPathResource("firebase/service-account.json").getInputStream();
    }
}
