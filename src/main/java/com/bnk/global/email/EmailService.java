package com.bnk.global.email;

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

    // "BNK카드" UTF-8 Base64 (RFC 2047) — 파일 인코딩 무관, 한글 깨짐 방지
    private static final String FROM_RFC2047 = "=?UTF-8?B?Qk5L7Lm065Oc?=";

    // ── 브랜드 컬러 ────────────────────────────────────────────────────
    private static final String RED   = "#DC2626";
    private static final String RED2  = "#b91c1c";   // 진한 빨강 (버튼 테두리)
    private static final String BG    = "#f4f4f4";   // 외부 배경
    private static final String WHITE = "#ffffff";
    private static final String TEXT  = "#111111";
    private static final String MUTED = "#888888";
    private static final String FOOT  = "#f9f9f9";   // 푸터 배경

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
        String resetUrl = "http://localhost:8080/reset-password?token=" + resetToken;
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

            // helper.setFrom() 사용 시 재인코딩으로 한글 깨짐 → 헤더 직접 세팅
            message.setHeader("From", FROM_RFC2047 + " <" + fromAddress + ">");
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
    // 이메일 인증코드 HTML  [빨강/하양 테마]
    // ──────────────────────────────────────────────────────────────────
    private String buildVerificationHtml(String code) {
        return
            "<!DOCTYPE html>" +
            "<html lang='ko'>" +
            "<head><meta charset='UTF-8'>" +
            "<meta name='viewport' content='width=device-width,initial-scale=1'>" +
            "</head>" +
            "<body style='margin:0;padding:0;background:" + BG + ";" +
            "font-family:\"Apple SD Gothic Neo\",\"Malgun Gothic\",Arial,sans-serif;'>" +

            // ── 외부 래퍼 ──
            "<table width='100%' cellpadding='0' cellspacing='0' border='0'>" +
            "<tr><td align='center' style='padding:40px 16px;'>" +

            // ── 카드 ──
            "<table width='480' cellpadding='0' cellspacing='0' border='0'" +
            " style='background:" + WHITE + ";border-radius:8px;overflow:hidden;" +
            "box-shadow:0 2px 16px rgba(0,0,0,0.08);max-width:480px;width:100%;'>" +

            // ── 헤더 (빨강) ──
            "<tr><td style='background:" + RED + ";padding:22px 36px;'>" +
            "<table cellpadding='0' cellspacing='0' border='0' width='100%'><tr>" +
            "<td><span style='color:" + WHITE + ";font-size:18px;font-weight:700;" +
            "letter-spacing:-0.3px;'>BNK\uce74\ub4dc</span></td>" +
            "<td align='right'><span style='color:rgba(255,255,255,0.7);" +
            "font-size:11px;'>BNK\ubd80\uc0b0\uc740\ud589</span></td>" +
            "</tr></table>" +
            "</td></tr>" +

            // ── 빨간 구분선 ──
            "<tr><td style='height:3px;background:" + RED + ";'></td></tr>" +

            // ── 본문 ──
            "<tr><td style='padding:36px 36px 28px;'>" +

            // 제목
            "<p style='margin:0 0 6px;color:" + TEXT + ";font-size:20px;font-weight:700;'>" +
            "\uc774\uba54\uc77c \uc778\uc99d</p>" +

            // 부제목
            "<p style='margin:0 0 28px;color:" + MUTED + ";font-size:14px;line-height:1.7;'>" +
            "\uc544\ub798 \uc778\uc99d\ucf54\ub4dc\ub97c \uc785\ub825\ud574 \uc8fc\uc138\uc694.<br>" +
            "\ucf54\ub4dc\ub294 <strong style='color:" + TEXT + ";'>10\ubd84</strong> " +
            "\ub3d9\uc548 \uc720\ud6a8\ud569\ub2c8\ub2e4.</p>" +

            // 인증코드 박스
            "<div style='border:2px solid " + RED + ";border-radius:6px;" +
            "padding:20px;text-align:center;margin-bottom:28px;" +
            "background:#fef2f2;'>" +
            "<p style='margin:0 0 6px;color:" + MUTED + ";font-size:11px;" +
            "letter-spacing:1px;text-transform:uppercase;'>인증코드</p>" +
            "<p style='margin:0;color:" + RED + ";font-size:36px;font-weight:700;" +
            "letter-spacing:10px;font-family:\"Courier New\",Courier,monospace;'>" +
            code + "</p>" +
            "</div>" +

            // 안내문
            "<p style='margin:0;color:" + MUTED + ";font-size:12px;line-height:1.7;" +
            "border-left:3px solid " + RED + ";padding-left:12px;'>" +
            "\ubcf8\uc778\uc774 \uc694\uccad\ud558\uc9c0 \uc54a\uc740 \uacbd\uc6b0 \uc774 \uba54\uc77c\uc744 " +
            "\ubb34\uc2dc\ud558\uc2dc\uace0, \uc73c\uc2e0 \uc8fc\uc18c\ub97c " +
            "\ud655\uc778\ud574 \uc8fc\uc138\uc694.</p>" +

            "</td></tr>" +

            // ── 푸터 ──
            "<tr><td style='background:" + FOOT + ";padding:18px 36px;" +
            "border-top:1px solid #eeeeee;'>" +
            "<table cellpadding='0' cellspacing='0' border='0' width='100%'><tr>" +
            "<td><p style='margin:0;color:#bbbbbb;font-size:11px;'>" +
            "&copy; BNK\uce74\ub4dc. All rights reserved.</p></td>" +
            "<td align='right'><p style='margin:0;color:#bbbbbb;font-size:11px;'>" +
            "\ubd80\uc0b0\uad11\uc5ed\uc2dc \ub3d9\ub300\uc2e0\uad6c \uc218\uc815\ub85c 60</p></td>" +
            "</tr></table>" +
            "</td></tr>" +

            "</table>" +   // 카드 닫기
            "</td></tr></table>" +  // 외부 래퍼 닫기
            "</body></html>";
    }

    // ──────────────────────────────────────────────────────────────────
    // 비밀번호 재설정 HTML  [빨강/하양 테마]
    // ──────────────────────────────────────────────────────────────────
    private String buildPasswordResetHtml(String resetUrl) {
        return
            "<!DOCTYPE html>" +
            "<html lang='ko'>" +
            "<head><meta charset='UTF-8'>" +
            "<meta name='viewport' content='width=device-width,initial-scale=1'>" +
            "</head>" +
            "<body style='margin:0;padding:0;background:" + BG + ";" +
            "font-family:\"Apple SD Gothic Neo\",\"Malgun Gothic\",Arial,sans-serif;'>" +

            // ── 외부 래퍼 ──
            "<table width='100%' cellpadding='0' cellspacing='0' border='0'>" +
            "<tr><td align='center' style='padding:40px 16px;'>" +

            // ── 카드 ──
            "<table width='480' cellpadding='0' cellspacing='0' border='0'" +
            " style='background:" + WHITE + ";border-radius:8px;overflow:hidden;" +
            "box-shadow:0 2px 16px rgba(0,0,0,0.08);max-width:480px;width:100%;'>" +

            // ── 헤더 (빨강) ──
            "<tr><td style='background:" + RED + ";padding:22px 36px;'>" +
            "<table cellpadding='0' cellspacing='0' border='0' width='100%'><tr>" +
            "<td><span style='color:" + WHITE + ";font-size:18px;font-weight:700;" +
            "letter-spacing:-0.3px;'>BNK\uce74\ub4dc</span></td>" +
            "<td align='right'><span style='color:rgba(255,255,255,0.7);" +
            "font-size:11px;'>BNK\ubd80\uc0b0\uc740\ud589</span></td>" +
            "</tr></table>" +
            "</td></tr>" +

            // ── 빨간 구분선 ──
            "<tr><td style='height:3px;background:" + RED + ";'></td></tr>" +

            // ── 본문 ──
            "<tr><td style='padding:36px 36px 28px;'>" +

            // 제목
            "<p style='margin:0 0 6px;color:" + TEXT + ";font-size:20px;font-weight:700;'>" +
            "\ube44\ubc00\ubc88\ud638 \uc7ac\uc124\uc815</p>" +

            // 부제목
            "<p style='margin:0 0 28px;color:" + MUTED + ";font-size:14px;line-height:1.7;'>" +
            "\uc544\ub798 \ubc84\ud2bc\uc744 \ud074\ub9ad\ud558\uc5ec \ube44\ubc00\ubc88\ud638\ub97c " +
            "\uc7ac\uc124\uc815\ud558\uc138\uc694.<br>" +
            "\ub9c1\ud06c\ub294 \uc694\uccad \ud6c4 <strong style='color:" + TEXT + ";'>30\ubd84</strong> " +
            "\ub3d9\uc548 \uc720\ud6a8\ud569\ub2c8\ub2e4.</p>" +

            // 버튼
            "<div style='text-align:center;margin-bottom:28px;'>" +
            "<a href='" + resetUrl + "'" +
            " style='display:inline-block;background:" + RED + ";" +
            "color:" + WHITE + ";font-size:15px;font-weight:700;" +
            "padding:14px 40px;border-radius:24px;text-decoration:none;" +
            "border:2px solid " + RED2 + ";letter-spacing:0.3px;'>" +
            "\ube44\ubc00\ubc88\ud638 \uc7ac\uc124\uc815\ud558\uae30" +
            "</a>" +
            "</div>" +

            // 안내문
            "<p style='margin:0;color:" + MUTED + ";font-size:12px;line-height:1.7;" +
            "border-left:3px solid " + RED + ";padding-left:12px;'>" +
            "\ubcf8\uc778\uc774 \uc694\uccad\ud558\uc9c0 \uc54a\uc740 \uacbd\uc6b0 \uc774 \uba54\uc77c\uc744 " +
            "\ubb34\uc2dc\ud558\uc138\uc694.<br>" +
            "\ubcf4\uc548\uc744 \uc704\ud574 \uc774 \ub9c1\ud06c\ub294 <strong style='color:" + TEXT + ";'>1\ud68c" +
            "</strong>\ub9cc \uc0ac\uc6a9 \uac00\ub2a5\ud569\ub2c8\ub2e4.</p>" +

            "</td></tr>" +

            // ── 푸터 ──
            "<tr><td style='background:" + FOOT + ";padding:18px 36px;" +
            "border-top:1px solid #eeeeee;'>" +
            "<table cellpadding='0' cellspacing='0' border='0' width='100%'><tr>" +
            "<td><p style='margin:0;color:#bbbbbb;font-size:11px;'>" +
            "&copy; BNK\uce74\ub4dc. All rights reserved.</p></td>" +
            "<td align='right'><p style='margin:0;color:#bbbbbb;font-size:11px;'>" +
            "\ubd80\uc0b0\uad11\uc5ed\uc2dc \ub3d9\ub300\uc2e0\uad6c \uc218\uc815\ub85c 60</p></td>" +
            "</tr></table>" +
            "</td></tr>" +

            "</table>" +   // 카드 닫기
            "</td></tr></table>" +  // 외부 래퍼 닫기
            "</body></html>";
    }
}