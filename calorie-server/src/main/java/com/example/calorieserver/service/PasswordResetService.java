package com.example.calorieserver.service;

import com.example.calorieserver.entity.PasswordResetToken;
import com.example.calorieserver.entity.User;
import com.example.calorieserver.exception.BusinessException;
import com.example.calorieserver.repository.PasswordResetTokenRepository;
import com.example.calorieserver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * 密码重置：申请（防枚举 + 一次性 token + 邮件）+ 重置（校验 token 后改密）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final long EXPIRE_MINUTES = 30;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final JavaMailSender mailSender;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${spring.mail.username:}")
    private String mailUsername;

    /**
     * 申请重置：生成 30 分钟有效的一次性 token 并发邮件。
     * 防枚举：邮箱不存在时静默成功，不泄露注册状态；发信失败只记日志，前端同样返回成功。
     */
    @Transactional
    public void requestReset(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return;
        }
        User user = userOpt.get();

        // 同一时刻只有一个有效链接
        tokenRepository.deleteByUserId(user.getId());

        String token = UUID.randomUUID().toString().replace("-", "");
        tokenRepository.save(PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiresAt(LocalDateTime.now().plusMinutes(EXPIRE_MINUTES))
                .used(false)
                .build());

        sendResetEmail(user.getEmail(), token);
    }

    /**
     * 重置密码：校验 token 存在 / 未使用 / 未过期，成功后作废 token。
     */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException("重置链接无效或已过期"));
        if (Boolean.TRUE.equals(resetToken.getUsed())) {
            throw new BusinessException("重置链接已使用，请重新申请");
        }
        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("重置链接已过期，请重新申请");
        }

        User user = resetToken.getUser();
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new BusinessException("新密码不能与原密码相同");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);
    }

    private void sendResetEmail(String toEmail, String token) {
        String link = "http://localhost:5173/reset-password?token=" + token;
        // 本地调试：后端日志打印重置链接，方便无真实邮箱时联调
        log.info("[本地调试] 密码重置链接: {}", link);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(mailUsername);
            helper.setTo(toEmail);
            helper.setSubject("重置你的「卡路里追踪」密码");
            helper.setText(buildHtml(link), true);
            mailSender.send(message);
        } catch (Exception e) {
            log.warn("密码重置邮件发送失败: {}", e.getMessage());
        }
    }

    private String buildHtml(String link) {
        return "<div style=\"font-family:system-ui,sans-serif;max-width:480px;margin:0 auto;padding:24px\">"
                + "<h2 style=\"color:#059669\">重置你的密码</h2>"
                + "<p>你好，</p>"
                + "<p>我们收到了你的密码重置请求。请点击下方按钮设置新密码（<b>30 分钟内有效</b>）：</p>"
                + "<p style=\"text-align:center\"><a href=\"" + link + "\" "
                + "style=\"display:inline-block;background:#059669;color:#fff;padding:10px 28px;"
                + "border-radius:8px;text-decoration:none\">重置密码</a></p>"
                + "<p style=\"color:#888;font-size:12px\">如果不是你本人操作，请忽略此邮件，你的密码不会改变。</p>"
                + "</div>";
    }
}
