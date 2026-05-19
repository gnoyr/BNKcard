package com.bnk.global.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromAddress;

    @Value("${mail.from.name:BNK카드}")
    private String fromName;

    // ──────────────────────────────────────────────────────
    // 회원가입 이메일 인증코드 발송
    // ──────────────────────────────────────────────────────
    @Async
    public void sendVerificationEmail(String to, String code) {
        send(to, "[BNK카드] 이메일 인증 코드", buildVerificationHtml(code));
    }

    // ──────────────────────────────────────────────────────
    // 비밀번호 재설정 링크 발송
    // ──────────────────────────────────────────────────────
    @Async
    public void sendPasswordResetEmail(String to, String resetToken) {
        String resetUrl = "http://localhost:8080/reset-password?token=" + resetToken;
        send(to, "[BNK카드] 비밀번호 재설정 안내", buildPasswordResetHtml(resetUrl));
    }

    // ──────────────────────────────────────────────────────
    // 공통 발송 메서드
    // ──────────────────────────────────────────────────────
    private void send(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            
            // InternetAddress 에 UTF-8 직접 지정 (MimeMessageHelper.setFrom 은 이중 인코딩 발생)
            message.setFrom(new InternetAddress(fromAddress, fromName, "UTF-8"));

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("[이메일발송] 성공: to={}, subject={}", to, subject);
        } catch (Exception e) {
            log.error("[이메일발송] 실패: to={}, error={}", to, e.getMessage());
        }
    }

    // ──────────────────────────────────────────────────────
    // HTML 템플릿
    // ──────────────────────────────────────────────────────
    private String buildVerificationHtml(String code) {
        return """
                <!DOCTYPE html>
                <html lang="ko">
                <body style="margin:0;padding:0;background:#f4f6f9;font-family:'Apple SD Gothic Neo',sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0">
                    <tr><td align="center" style="padding:40px 0;">
                      <table width="480" cellpadding="0" cellspacing="0"
                             style="background:#fff;border-radius:12px;overflow:hidden;
                                    box-shadow:0 2px 12px rgba(0,0,0,.08);">
                        <tr>
                          <td style="background:#1a3a6c;padding:28px 40px;">
                            <span style="color:#fff;font-size:20px;font-weight:700;">BNK카드</span>
                          </td>
                        </tr>
                        <tr>
                          <td style="padding:40px;">
                            <p style="margin:0 0 8px;color:#222;font-size:18px;font-weight:700;">
                              이메일 인증 코드
                            </p>
                            <p style="margin:0 0 28px;color:#666;font-size:14px;line-height:1.6;">
                              아래 인증 코드를 입력창에 입력해 주세요.<br>
                              코드는 <strong>10분</strong> 동안 유효합니다.
                            </p>
                            <div style="background:#f0f4ff;border:2px dashed #1a3a6c;border-radius:8px;
                                        padding:20px;text-align:center;margin-bottom:28px;">
                              <span style="font-size:36px;font-weight:800;letter-spacing:12px;color:#1a3a6c;">
                                %s
                              </span>
                            </div>
                            <p style="margin:0;color:#999;font-size:12px;line-height:1.6;">
                              본인이 요청하지 않은 경우 이 메일을 무시하세요.<br>
                              인증 코드는 타인에게 공유하지 마세요.
                            </p>
                          </td>
                        </tr>
                        <tr>
                          <td style="background:#f8f9fb;padding:20px 40px;border-top:1px solid #eee;">
                            <p style="margin:0;color:#aaa;font-size:11px;">ⓒ BNK카드. All rights reserved.</p>
                          </td>
                        </tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(code);
    }

    private String buildPasswordResetHtml(String resetUrl) {
        return """
                <!DOCTYPE html>
                <html lang="ko">
                <body style="margin:0;padding:0;background:#f4f6f9;font-family:'Apple SD Gothic Neo',sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0">
                    <tr><td align="center" style="padding:40px 0;">
                      <table width="480" cellpadding="0" cellspacing="0"
                             style="background:#fff;border-radius:12px;overflow:hidden;
                                    box-shadow:0 2px 12px rgba(0,0,0,.08);">
                        <tr>
                          <td style="background:#1a3a6c;padding:28px 40px;">
                            <span style="color:#fff;font-size:20px;font-weight:700;">BNK카드</span>
                          </td>
                        </tr>
                        <tr>
                          <td style="padding:40px;">
                            <p style="margin:0 0 8px;color:#222;font-size:18px;font-weight:700;">
                              비밀번호 재설정
                            </p>
                            <p style="margin:0 0 28px;color:#666;font-size:14px;line-height:1.6;">
                              아래 버튼을 클릭하여 비밀번호를 재설정하세요.<br>
                              링크는 <strong>30분</strong> 동안 유효합니다.
                            </p>
                            <div style="text-align:center;margin-bottom:28px;">
                              <a href="%s"
                                 style="display:inline-block;background:#1a3a6c;color:#fff;
                                        font-size:15px;font-weight:700;padding:14px 36px;
                                        border-radius:8px;text-decoration:none;">
                                비밀번호 재설정하기
                              </a>
                            </div>
                            <p style="margin:0 0 6px;color:#999;font-size:12px;">버튼이 작동하지 않으면 아래 링크를 복사하세요.</p>
                            <p style="margin:0;color:#1a3a6c;font-size:11px;word-break:break-all;">%s</p>
                            <hr style="margin:24px 0;border:none;border-top:1px solid #eee;">
                            <p style="margin:0;color:#999;font-size:12px;line-height:1.6;">
                              본인이 요청하지 않은 경우 이 메일을 무시하세요.
                            </p>
                          </td>
                        </tr>
                        <tr>
                          <td style="background:#f8f9fb;padding:20px 40px;border-top:1px solid #eee;">
                            <p style="margin:0;color:#aaa;font-size:11px;">ⓒ BNK카드. All rights reserved.</p>
                          </td>
                        </tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(resetUrl, resetUrl);
    }
}