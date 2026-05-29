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
    private static final String TEAL_50  = "#E0F4F7";   // 코드박스 배경
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
             "[BNK\uce74\ub4dc] \uc774\uba54\uc77c \uc778\uc99d \ucf54\ub4dc",
             buildVerificationHtml(code));
    }

    // ──────────────────────────────────────────────────────────────────
    // 비밀번호 재설정 링크 발송  [F-22 / find-password]
    // ──────────────────────────────────────────────────────────────────
    @Async
    public void sendPasswordResetEmail(String to, String resetToken) {
        String resetUrl = baseUrl + "/auth/reset-password.html?token=" + resetToken;
        send(to,
             "[BNK\uce74\ub4dc] \ube44\ubc00\ubc88\ud638 \uc7ac\uc124\uc815 \uc548\ub0b4",
             buildPasswordResetHtml(resetUrl));
    }

    // ──────────────────────────────────────────────────────────────────
    // 공통 발송 로직
    // ──────────────────────────────────────────────────────────────────
    private void send(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // ── 발신자 설정 ──────────────────────────────────────────────
            // 기존: message.setHeader("From", RFC2047_수동인코딩)
            //   → Gmail SMTP로 Gmail 수신자에게 발송 시 SPF/DKIM 불일치로 차단됨
            //   → MimeMessageHelper 가 setFrom 이후 내부에서 재인코딩하여 깨짐 발생
            //
            // 수정: InternetAddress(address, personal, charset) 사용
            //   → JavaMail 이 RFC 2047 인코딩을 직접 처리 → 한글 깨짐 없음
            //   → helper.setFrom() 경유 → SMTP envelope-From 과 헤더 From 일치
            //   → SPF/DKIM 검증 통과 → Gmail 수신자에게 정상 전달
            helper.setFrom(new InternetAddress(fromAddress, FROM_NAME, StandardCharsets.UTF_8.name()));

            // ── Reply-To 추가 ─────────────────────────────────────────────
            // Gmail은 발신/수신 주소가 같으면 "루프 메일"로 의심해 드롭할 수 있음
            // Reply-To 를 발신 주소로 명시하면 정상 메일로 인식됨
            message.setReplyTo(new InternetAddress[]{ new InternetAddress(fromAddress, FROM_NAME, StandardCharsets.UTF_8.name()) });

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

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
            "letter-spacing:-0.3px;'>BNK\uce74\ub4dc</span></td>" +
            "<td align='right'><span style='color:rgba(255,255,255,0.6);" +
            "font-size:11px;'>BNK\ubd80\uc0b0\uc740\ud589</span></td>" +
            "</tr></table>" +
            "</td></tr>" +

            // ── Teal-600 구분선 ──
            "<tr><td style='height:3px;background:" + TEAL_600 + ";'></td></tr>" +

            // ── 본문 ──
            "<tr><td style='padding:36px 36px 28px;'>" +

            // 제목
            "<p style='margin:0 0 6px;color:" + TEAL_800 + ";font-size:20px;font-weight:700;" +
            "letter-spacing:-0.3px;'>" +
            "\uc774\uba54\uc77c \uc778\uc99d</p>" +

            // 부제목
            "<p style='margin:0 0 28px;color:" + MUTED + ";font-size:14px;line-height:1.7;'>" +
            "\uc544\ub798 \uc778\uc99d\ucf54\ub4dc\ub97c \uc785\ub825\ud574 \uc8fc\uc138\uc694.<br>" +
            "\ucf54\ub4dc\ub294 <strong style='color:" + TEAL_800 + ";'>10\ubd84</strong>" +
            " \ub3d9\uc548 \uc720\ud6a8\ud569\ub2c8\ub2e4.</p>" +

            // 인증코드 박스
            "<div style='border:2px solid " + TEAL_400 + ";border-radius:8px;" +
            "padding:24px 20px;text-align:center;margin-bottom:28px;" +
            "background:" + TEAL_50 + ";'>" +
            "<p style='margin:0 0 8px;color:" + MUTED + ";font-size:11px;" +
            "letter-spacing:2px;text-transform:uppercase;'>\uc778\uc99d\ucf54\ub4dc</p>" +
            "<p style='margin:0;color:" + TEAL_600 + ";font-size:38px;font-weight:700;" +
            "letter-spacing:12px;font-family:\"Courier New\",Courier,monospace;'>" +
            code + "</p>" +
            "</div>" +

            // 안내문
            "<p style='margin:0;color:" + MUTED + ";font-size:12px;line-height:1.8;" +
            "border-left:3px solid " + TEAL_400 + ";padding-left:12px;'>" +
            "\ubcf8\uc778\uc774 \uc694\uccad\ud558\uc9c0 \uc54a\uc740 \uacbd\uc6b0 \uc774 \uba54\uc77c\uc744 " +
            "\ubb34\uc2dc\ud558\uc2dc\uace0, \uc774\uba54\uc77c \uc8fc\uc18c\ub97c " +
            "\ud655\uc778\ud574 \uc8fc\uc138\uc694.</p>" +

            "</td></tr>" +

            // ── 푸터 ──
            "<tr><td style='background:" + FOOT_BG + ";padding:18px 36px;" +
            "border-top:1px solid " + TEAL_100 + ";'>" +
            "<table cellpadding='0' cellspacing='0' border='0' width='100%'><tr>" +
            "<td><p style='margin:0;color:#9BB4BB;font-size:11px;'>" +
            "&copy; BNK\uce74\ub4dc. All rights reserved.</p></td>" +
            "<td align='right'><p style='margin:0;color:#9BB4BB;font-size:11px;'>" +
            "\ubd80\uc0b0\uad11\uc5ed\uc2dc \ub3d9\ub300\uc2e0\uad6c \uc218\uc815\ub85c 60</p></td>" +
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

            // ── 헤더 (Teal-900) ──
            "<tr><td style='background:" + TEAL_900 + ";padding:22px 36px;'>" +
            "<table cellpadding='0' cellspacing='0' border='0' width='100%'><tr>" +
            "<td><span style='color:" + WHITE + ";font-size:18px;font-weight:700;" +
            "letter-spacing:-0.3px;'>BNK\uce74\ub4dc</span></td>" +
            "<td align='right'><span style='color:rgba(255,255,255,0.6);" +
            "font-size:11px;'>BNK\ubd80\uc0b0\uc740\ud589</span></td>" +
            "</tr></table>" +
            "</td></tr>" +

            // ── Teal-600 구분선 ──
            "<tr><td style='height:3px;background:" + TEAL_600 + ";'></td></tr>" +

            // ── 본문 ──
            "<tr><td style='padding:36px 36px 28px;'>" +

            // 제목
            "<p style='margin:0 0 6px;color:" + TEAL_800 + ";font-size:20px;font-weight:700;" +
            "letter-spacing:-0.3px;'>" +
            "\ube44\ubc00\ubc88\ud638 \uc7ac\uc124\uc815</p>" +

            // 부제목
            "<p style='margin:0 0 28px;color:" + MUTED + ";font-size:14px;line-height:1.7;'>" +
            "\uc544\ub798 \ubc84\ud2bc\uc744 \ud074\ub9ad\ud558\uc5ec \ube44\ubc00\ubc88\ud638\ub97c " +
            "\uc7ac\uc124\uc815\ud558\uc138\uc694.<br>" +
            "\ub9c1\ud06c\ub294 \uc694\uccad \ud6c4 <strong style='color:" + TEAL_800 + ";'>30\ubd84</strong>" +
            " \ub3d9\uc548 \uc720\ud6a8\ud569\ub2c8\ub2e4.</p>" +

            // 버튼
            "<table cellpadding='0' cellspacing='0' border='0' style='margin-bottom:28px;'>" +
            "<tr><td style='background:" + TEAL_600 + ";border-radius:6px;" +
            "box-shadow:0 2px 8px rgba(0,103,127,0.25);'>" +
            "<a href='" + resetUrl + "'" +
            " style='display:inline-block;padding:14px 32px;" +
            "color:" + WHITE + ";font-size:15px;font-weight:700;" +
            "text-decoration:none;letter-spacing:-0.2px;'>" +
            "\ube44\ubc00\ubc88\ud638 \uc7ac\uc124\uc815\ud558\uae30" +
            "</a>" +
            "</td></tr></table>" +

            // URL 직접 입력 안내
            "<p style='margin:0 0 20px;color:" + MUTED + ";font-size:12px;line-height:1.7;'>" +
            "\ubc84\ud2bc\uc774 \uc791\ub3d9\ud558\uc9c0 \uc54a\uc73c\uba74 \uc544\ub798 \uc8fc\uc18c\ub97c " +
            "\ube0c\ub77c\uc6b0\uc800\uc5d0 \uc9c1\uc811 \uc785\ub825\ud574 \uc8fc\uc138\uc694.<br>" +
            "<span style='color:" + TEAL_600 + ";word-break:break-all;'>" + resetUrl + "</span></p>" +

            // 경고 안내문
            "<p style='margin:0;color:" + MUTED + ";font-size:12px;line-height:1.8;" +
            "border-left:3px solid " + TEAL_400 + ";padding-left:12px;'>" +
            "\ubcf8\uc778\uc774 \uc694\uccad\ud558\uc9c0 \uc54a\uc740 \uacbd\uc6b0 \uc774 \uba54\uc77c\uc744 " +
            "\ubb34\uc2dc\ud558\uc2dc\uace0, \ube44\ubc00\ubc88\ud638 \ubcc0\uacbd\uc774 " +
            "\uc758\uc2ec\ub418\uba74 \uc989\uc2dc \ub85c\uadf8\uc778 \ud6c4 " +
            "\ube44\ubc00\ubc88\ud638\ub97c \ubcc0\uacbd\ud574 \uc8fc\uc138\uc694.</p>" +

            "</td></tr>" +

            // ── 푸터 ──
            "<tr><td style='background:" + FOOT_BG + ";padding:18px 36px;" +
            "border-top:1px solid " + TEAL_100 + ";'>" +
            "<table cellpadding='0' cellspacing='0' border='0' width='100%'><tr>" +
            "<td><p style='margin:0;color:#9BB4BB;font-size:11px;'>" +
            "&copy; BNK\uce74\ub4dc. All rights reserved.</p></td>" +
            "<td align='right'><p style='margin:0;color:#9BB4BB;font-size:11px;'>" +
            "\ubd80\uc0b0\uad11\uc5ed\uc2dc \ub3d9\ub300\uc2e0\uad6c \uc218\uc815\ub85c 60</p></td>" +
            "</tr></table>" +
            "</td></tr>" +

            "</table>" +
            "</td></tr></table>" +
            "</body></html>";
    }
}