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

	// "BNK카드" UTF-8 Base64 인코딩 (RFC 2047) — 파일 인코딩 무관, 깨짐 방지
	private static final String FROM_RFC2047 = "=?UTF-8?B?Qk5L7Lm065Oc?=";

	@Async
	public void sendVerificationEmail(String to, String code) {
		send(to, "[BNK\uce74\ub4dc] \uc774\uba54\uc77c \uc778\uc99d \ucf54\ub4dc", buildVerificationHtml(code));
	}

	@Async
	public void sendPasswordResetEmail(String to, String resetToken) {
		String resetUrl = "http://localhost:8080/reset-password?token=" + resetToken;
		send(to, "[BNK\uce74\ub4dc] \ube44\ubc00\ubc88\ud638 \uc7ac\uc124\uc815 \uc548\ub0b4",
				buildPasswordResetHtml(resetUrl));
	}

	private void send(String to, String subject, String htmlContent) {
		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

			// From 헤더 직접 세팅 — helper.setFrom() 사용 시 재인코딩으로 한글 깨짐
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

	private String buildVerificationHtml(String code) {
		return "<!DOCTYPE html>" + "<html lang='ko'><body style='margin:0;padding:0;background:#f4f6f9;"
				+ "font-family:\"Apple SD Gothic Neo\",sans-serif;'>"
				+ "<table width='100%' cellpadding='0' cellspacing='0'>"
				+ "<tr><td align='center' style='padding:40px 0;'>"
				+ "<table width='480' cellpadding='0' cellspacing='0'"
				+ " style='background:#fff;border-radius:12px;overflow:hidden;"
				+ "box-shadow:0 2px 12px rgba(0,0,0,.08);'>" + "<tr><td style='background:#1a3a6c;padding:28px 40px;'>"
				+ "<span style='color:#fff;font-size:20px;font-weight:700;'>BNK\uce74\ub4dc</span></td></tr>"
				+ "<tr><td style='padding:40px;'>"
				+ "<p style='margin:0 0 8px;color:#222;font-size:18px;font-weight:700;'>"
				+ "\uc774\uba54\uc77c \uc778\uc99d \ucf54\ub4dc</p>"
				+ "<p style='margin:0 0 28px;color:#666;font-size:14px;line-height:1.6;'>"
				+ "\uc544\ub798 \uc778\uc99d \ucf54\ub4dc\ub97c \uc785\ub825\ucc3d\uc5d0 \uc785\ub825\ud574 \uc8fc\uc138\uc694.<br>"
				+ "\ucf54\ub4dc\ub294 <strong>10\ubd84</strong> \ub3d9\uc548 \uc720\ud6a8\ud569\ub2c8\ub2e4.</p>"
				+ "<div style='background:#f0f4ff;border:2px dashed #1a3a6c;border-radius:8px;"
				+ "padding:20px;text-align:center;margin-bottom:28px;'>"
				+ "<span style='font-size:36px;font-weight:800;letter-spacing:12px;color:#1a3a6c;'>" + code
				+ "</span></div>" + "<p style='margin:0;color:#999;font-size:12px;line-height:1.6;'>"
				+ "\ubcf8\uc778\uc774 \uc694\uccad\ud558\uc9c0 \uc54a\uc740 \uacbd\uc6b0 \uc774 \uba54\uc77c\uc744 \ubb34\uc2dc\ud558\uc138\uc694.<br>"
				+ "\uc778\uc99d \ucf54\ub4dc\ub294 \ud0c0\uc778\uc5d0\uac8c \uacf5\uc720\ud558\uc9c0 \ub9c8\uc138\uc694.</p>"
				+ "</td></tr>" + "<tr><td style='background:#f8f9fb;padding:20px 40px;border-top:1px solid #eee;'>"
				+ "<p style='margin:0;color:#aaa;font-size:11px;'>" + "&copy; BNK\uce74\ub4dc. All rights reserved.</p>"
				+ "</td></tr></table></td></tr></table></body></html>";
	}

	private String buildPasswordResetHtml(String resetUrl) {
		return "<!DOCTYPE html>" + "<html lang='ko'><body style='margin:0;padding:0;background:#f4f6f9;"
				+ "font-family:\"Apple SD Gothic Neo\",sans-serif;'>"
				+ "<table width='100%' cellpadding='0' cellspacing='0'>"
				+ "<tr><td align='center' style='padding:40px 0;'>"
				+ "<table width='480' cellpadding='0' cellspacing='0'"
				+ " style='background:#fff;border-radius:12px;overflow:hidden;"
				+ "box-shadow:0 2px 12px rgba(0,0,0,.08);'>" + "<tr><td style='background:#1a3a6c;padding:28px 40px;'>"
				+ "<span style='color:#fff;font-size:20px;font-weight:700;'>BNK\uce74\ub4dc</span></td></tr>"
				+ "<tr><td style='padding:40px;'>"
				+ "<p style='margin:0 0 8px;color:#222;font-size:18px;font-weight:700;'>"
				+ "\ube44\ubc00\ubc88\ud638 \uc7ac\uc124\uc815</p>"
				+ "<p style='margin:0 0 28px;color:#666;font-size:14px;line-height:1.6;'>"
				+ "\uc544\ub798 \ubc84\ud2bc\uc744 \ud074\ub9ad\ud558\uc5ec \ube44\ubc00\ubc88\ud638\ub97c \uc7ac\uc124\uc815\ud558\uc138\uc694.<br>"
				+ "\ub9c1\ud06c\ub294 <strong>30\ubd84</strong> \ub3d9\uc548 \uc720\ud6a8\ud569\ub2c8\ub2e4.</p>"
				+ "<div style='text-align:center;margin-bottom:28px;'>" + "<a href='" + resetUrl + "'"
				+ " style='display:inline-block;background:#1a3a6c;color:#fff;"
				+ "font-size:15px;font-weight:700;padding:14px 36px;" + "border-radius:8px;text-decoration:none;'>"
				+ "\ube44\ubc00\ubc88\ud638 \uc7ac\uc124\uc815\ud558\uae30</a></div>"
				+ "<p style='margin:0 0 6px;color:#999;font-size:12px;'>"
				+ "\ubc84\ud2bc\uc774 \uc791\ub3d9\ud558\uc9c0 \uc54a\uc73c\uba74 \uc544\ub798 \ub9c1\ud06c\ub97c \ubcf5\uc0ac\ud558\uc138\uc694.</p>"
				+ "<p style='margin:0;color:#1a3a6c;font-size:11px;word-break:break-all;'>" + resetUrl + "</p>"
				+ "<hr style='margin:24px 0;border:none;border-top:1px solid #eee;'>"
				+ "<p style='margin:0;color:#999;font-size:12px;line-height:1.6;'>"
				+ "\ubcf8\uc778\uc774 \uc694\uccad\ud558\uc9c0 \uc54a\uc740 \uacbd\uc6b0 \uc774 \uba54\uc77c\uc744 \ubb34\uc2dc\ud558\uc138\uc694.</p>"
				+ "</td></tr>" + "<tr><td style='background:#f8f9fb;padding:20px 40px;border-top:1px solid #eee;'>"
				+ "<p style='margin:0;color:#aaa;font-size:11px;'>" + "&copy; BNK\uce74\ub4dc. All rights reserved.</p>"
				+ "</td></tr></table></td></tr></table></body></html>";
	}
}