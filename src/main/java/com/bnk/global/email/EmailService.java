package com.bnk.global.email;

import java.nio.charset.StandardCharsets;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromAddress;

    @Value("${app.base-url}")
    private String baseUrl;

    // 발신자 표시명 — InternetAddress 로 인코딩하므로 한글 리터럴 직접 사용
    private static final String FROM_NAME = "BNK카드";

    // ── Teal 팔레트 (teal-theme.css 기준) ──────────────────────────────
    private static final String TEAL_900 = "#003040";   // 헤더 배경
    private static final String TEAL_800 = "#004D61";   // 강조 텍스트
    private static final String TEAL_600 = "#00677F";   // primary / 버튼
    private static final String TEAL_400 = "#3AAFC4";   // 코드박스 border / 버튼 hover
    private static final String TEAL_100 = "#B3E3EC";   // 연한 구분선
    private static final String BG       = "#f0f4f5";   // 외부 배경 (gray-100 계열)
    private static final String WHITE    = "#ffffff";
    private static final String MUTED    = "#5C7A83";   // gray-600 계열 (본문 보조 텍스트)
    private static final String FOOT_BG  = "#f8fafb";   // gray-50 계열

    // ──────────────────────────────────────────────────────────────────
    // 이메일 인증코드 발송  [F-00 / send-verify-code]
    // ──────────────────────────────────────────────────────────────────
    @Async
    public void sendVerificationEmail(String to, String code) {
        send(to,
             "[BNK카드] 이메일 인증 코드",
             buildVerificationHtml(code));
    }

    // ──────────────────────────────────────────────────────────────────
    // 비밀번호 재설정 이메일 발송
    // ──────────────────────────────────────────────────────────────────
    @Async
    public void sendPasswordResetEmail(String to, String token) {
        String resetUrl = baseUrl + "/reset-password?token=" + token;
        send(to,
             "[BNK카드] 비밀번호 재설정",
             buildPasswordResetHtml(resetUrl));
    }

    // ──────────────────────────────────────────────────────────────────
    // 공통 발송
    // ──────────────────────────────────────────────────────────────────
    private void send(String to, String subject, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(new InternetAddress(fromAddress, FROM_NAME, "UTF-8"));
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            log.info("[이메일발송] 성공: to={}, subject={}", to, subject);
        } catch (Exception e) {
            log.error("[이메일발송] 실패: to={}, error={}", to, e.getMessage());
        }
    }

    // ──────────────────────────────────────────────────────────────────
    // 이메일 인증코드 HTML  [Teal 테마]
    // ──────────────────────────────────────────────────────────────────
    private String buildVerificationHtml(String code) {
        return
            "<!DOCTYPE html>" +
            "<html lang='ko'>" +
            "<head>" +
            "<meta charset='UTF-8'>" +
            "<meta name='viewport' content='width=device-width,initial-scale=1'>" +
            "</head>" +
            "<body style='margin:0;padding:0;background:" + BG + ";" +
            "font-family:\"Apple SD Gothic Neo\",\"Malgun Gothic\",Arial,sans-serif;'>" +

            // ── 외부 래퍼 ──
            "<table width='100%' cellpadding='0' cellspacing='0' border='0'>" +
            "<tr><td align='center' style='padding:40px 16px;'>" +

            // ── 카드 ──
            "<table width='480' cellpadding='0' cellspacing='0' border='0'" +
            " style='background:" + WHITE + ";border-radius:10px;overflow:hidden;" +
            "box-shadow:0 4px 20px rgba(0,103,127,0.13);max-width:480px;width:100%;'>" +

            // ── 헤더 (Teal-900) ──
            "<tr><td style='background:" + TEAL_900 + ";padding:22px 36px;'>" +
            "<table cellpadding='0' cellspacing='0' border='0' width='100%'><tr>" +
            "<td><span style='color:" + WHITE + ";font-size:18px;font-weight:700;" +
            "letter-spacing:-0.3px;'>BNK카드</span></td>" +
            "<td align='right'><span style='color:rgba(255,255,255,0.6);" +
            "font-size:11px;'>BNK부산은행</span></td>" +
            "</tr></table>" +
            "</td></tr>" +

            // ── Teal-600 구분선 ──
            "<tr><td style='height:3px;background:" + TEAL_600 + ";'></td></tr>" +

            // ── 본문 ──
            "<tr><td style='padding:36px 36px 28px;'>" +

            // 제목
            "<p style='margin:0 0 6px;color:" + TEAL_800 + ";font-size:20px;font-weight:700;" +
            "letter-spacing:-0.3px;'>이메일 인증</p>" +

            // 부제목
            "<p style='margin:0 0 28px;color:" + MUTED + ";font-size:14px;line-height:1.7;'>" +
            "아래 버튼을 클릭하여 인증번호를 확인하세요.<br>" +
            "인증번호는 <strong style='color:" + TEAL_800 + ";'>10분</strong> 동안 유효합니다.</p>" +

            // ── 인증번호 확인 버튼 (클릭 → /copy-code?code=XXX → 코드 표시 + 복사) ──
            "<table cellpadding='0' cellspacing='0' border='0' align='center' style='margin:0 auto 16px;'><tr><td align='center'>" +
            "<a href='" + baseUrl + "/copy-code?code=" + code + "'" +
            " style='display:inline-block;background:" + TEAL_600 + ";color:" + WHITE + ";" +
            "font-size:15px;font-weight:700;padding:14px 32px;border-radius:8px;" +
            "text-decoration:none;letter-spacing:0.3px;'>인증번호 확인하기</a>" +
            "</td></tr></table>" +

            // 안내문
            "<p style='margin:0 0 28px;color:" + MUTED + ";font-size:12px;line-height:1.8;" +
            "border-left:3px solid " + TEAL_400 + ";padding-left:12px;'>" +
            "본인이 요청하지 않은 경우 이 메일을 " +
            "무시하시고, 이메일 주소를 " +
            "확인해 주세요.</p>" +

            "</td></tr>" +

            // ── 푸터 ──
            "<tr><td style='background:" + FOOT_BG + ";padding:18px 36px;" +
            "border-top:1px solid " + TEAL_100 + ";'>" +
            "<table cellpadding='0' cellspacing='0' border='0' width='100%'><tr>" +
            "<td><p style='margin:0;color:#9BB4BB;font-size:11px;'>" +
            "&copy; BNK카드. All rights reserved.</p></td>" +
            "<td align='right'><p style='margin:0;color:#9BB4BB;font-size:11px;'>" +
            "부산광역시 동대신구 수정로 60</p></td>" +
            "</tr></table>" +
            "</td></tr>" +

            "</table>" +
            "</td></tr></table>" +
            "</body></html>";
    }

    // ──────────────────────────────────────────────────────────────────
    // 비밀번호 재설정 HTML  [Teal 테마]
    // ──────────────────────────────────────────────────────────────────
    private String buildPasswordResetHtml(String resetUrl) {
        return
            "<!DOCTYPE html>" +
            "<html lang='ko'>" +
            "<head>" +
            "<meta charset='UTF-8'>" +
            "<meta name='viewport' content='width=device-width,initial-scale=1'>" +
            "</head>" +
            "<body style='margin:0;padding:0;background:" + BG + ";" +
            "font-family:\"Apple SD Gothic Neo\",\"Malgun Gothic\",Arial,sans-serif;'>" +

            // ── 외부 래퍼 ──
            "<table width='100%' cellpadding='0' cellspacing='0' border='0'>" +
            "<tr><td align='center' style='padding:40px 16px;'>" +

            // ── 카드 ──
            "<table width='480' cellpadding='0' cellspacing='0' border='0'" +
            " style='background:" + WHITE + ";border-radius:10px;overflow:hidden;" +
            "box-shadow:0 4px 20px rgba(0,103,127,0.13);max-width:480px;width:100%;'>" +

            // ── 헤더 ──
            "<tr><td style='background:" + TEAL_900 + ";padding:22px 36px;'>" +
            "<table cellpadding='0' cellspacing='0' border='0' width='100%'><tr>" +
            "<td><span style='color:" + WHITE + ";font-size:18px;font-weight:700;" +
            "letter-spacing:-0.3px;'>BNK카드</span></td>" +
            "<td align='right'><span style='color:rgba(255,255,255,0.6);" +
            "font-size:11px;'>BNK부산은행</span></td>" +
            "</tr></table>" +
            "</td></tr>" +

            // ── 구분선 ──
            "<tr><td style='height:3px;background:" + TEAL_600 + ";'></td></tr>" +

            // ── 본문 ──
            "<tr><td style='padding:36px 36px 28px;'>" +

            "<p style='margin:0 0 6px;color:" + TEAL_800 + ";font-size:20px;font-weight:700;" +
            "letter-spacing:-0.3px;'>비밀번호 재설정</p>" +

            "<p style='margin:0 0 28px;color:" + MUTED + ";font-size:14px;line-height:1.7;'>" +
            "아래 버튼을 클릭하여 비밀번호를 " +
            "재설정하세요.<br>" +
            "링크는 요청 후 <strong style='color:" + TEAL_800 + ";'>30분</strong>" +
            " 동안 유효합니다.</p>" +

            // 버튼
            "<table cellpadding='0' cellspacing='0' border='0'><tr><td>" +
            "<a href='" + resetUrl + "'" +
            " style='display:inline-block;background:" + TEAL_600 + ";color:" + WHITE + ";" +
            "font-size:15px;font-weight:700;padding:14px 32px;border-radius:8px;" +
            "text-decoration:none;letter-spacing:0.3px;'>비밀번호 재설정하기</a>" +
            "</td></tr></table>" +

            "<p style='margin:20px 0 0;color:" + MUTED + ";font-size:12px;line-height:1.8;" +
            "border-left:3px solid " + TEAL_400 + ";padding-left:12px;'>" +
            "본인이 요청하지 않은 경우 이 메일을 무시하세요.</p>" +

            "</td></tr>" +

            // ── 푸터 ──
            "<tr><td style='background:" + FOOT_BG + ";padding:18px 36px;" +
            "border-top:1px solid " + TEAL_100 + ";'>" +
            "<table cellpadding='0' cellspacing='0' border='0' width='100%'><tr>" +
            "<td><p style='margin:0;color:#9BB4BB;font-size:11px;'>" +
            "&copy; BNK카드. All rights reserved.</p></td>" +
            "<td align='right'><p style='margin:0;color:#9BB4BB;font-size:11px;'>" +
            "부산광역시 동대신구 수정로 60</p></td>" +
            "</tr></table>" +
            "</td></tr>" +

            "</table>" +
            "</td></tr></table>" +
            "</body></html>";
    }
    
    @Async
    public void sendIpVerifyCode(String to, String code) {
        send(to,
             "[BNK카드] IP 기기 인증 코드",
             buildIpVerifyHtml(code));
    }

    private String buildIpVerifyHtml(String code) {
        return buildVerificationHtml(code);
    }
}