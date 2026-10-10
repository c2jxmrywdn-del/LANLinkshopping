package com.lanlink.shopping.service;

import com.lanlink.shopping.common.BusinessException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VerifyCodeServiceTest {

    private VerifyCodeService service;

    @BeforeEach
    void setUp() {
        // phone 演示分支，不需要邮件通道
        service = new VerifyCodeService(null, "", "", "", true);
    }

    @Test
    void sendReturnsSixDigitCode() {
        String code = service.send(1L, "phone", "13900000001");
        assertNotNull(code);
        assertTrue(code.matches("\\d{6}"), "验证码应为 6 位数字");
    }

    @Test
    void verifySuccess() {
        String code = service.send(1L, "phone", "13900000001");
        assertDoesNotThrow(() -> service.verify(1L, "phone", "13900000001", code));
    }

    @Test
    void wrongCodeRejected() {
        service.send(1L, "phone", "13900000001");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.verify(1L, "phone", "13900000001", "000000"));
        assertTrue(ex.getMessage().contains("验证码错误"));
    }

    @Test
    void codeSingleUse() {
        String code = service.send(1L, "phone", "13900000001");
        service.verify(1L, "phone", "13900000001", code);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.verify(1L, "phone", "13900000001", code));
        assertTrue(ex.getMessage().contains("过期") || ex.getMessage().contains("错误"));
    }

    @Test
    void cooldownBlocksResend() {
        service.send(1L, "phone", "13900000001");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.send(1L, "phone", "13900000001"));
        assertTrue(ex.getMessage().contains("频繁"));
    }

    @Test
    void productionDoesNotEchoPhoneCodeWhenSmsIsNotConfigured() {
        VerifyCodeService production = new VerifyCodeService(null, "", "", "", false);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> production.send(7L, "phone", "13900000007"));
        assertTrue(ex.getMessage().contains("短信验证码服务暂未配置"));
    }

    @Test
    void productionDoesNotEchoEmailCodeWhenSmtpIsNotConfigured() {
        VerifyCodeService production = new VerifyCodeService(null, "", "", "", false);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> production.send(8L, "email", "buyer@example.com"));
        assertTrue(ex.getMessage().contains("邮箱验证码服务暂未配置"));
    }

    @Test
    void productionDoesNotAttemptSmtpWhenPasswordIsMissing() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        VerifyCodeService production = new VerifyCodeService(
                mailSender, "smtp.qq.com", "sender@qq.com", "", false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> production.send(9L, "email", "buyer@example.com"));

        assertTrue(ex.getMessage().contains("邮箱验证码服务暂未配置"));
        verifyNoInteractions(mailSender);
    }

    @Test
    void developmentFallsBackToDemoCodeWhenSmtpCredentialsAreIncomplete() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        VerifyCodeService development = new VerifyCodeService(
                mailSender, "smtp.qq.com", "sender@qq.com", "", true);

        String code = development.send(10L, "email", "buyer@example.com");

        assertNotNull(code);
        assertTrue(code.matches("\\d{6}"));
        verifyNoInteractions(mailSender);
    }

    @Test
    void configuredSmtpSendsEmailWithoutEchoingCode() throws Exception {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        VerifyCodeService configured = new VerifyCodeService(
                mailSender, "smtp.qq.com", "sender@qq.com", "smtp-app-password", false);
        String responseCode = configured.send(11L, "email", "buyer@example.com");

        assertNull(responseCode, "通过 SMTP 发送后不应向 API 回显验证码");
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        assertEquals("【LANLink 商城】邮箱验证码", captor.getValue().getSubject());
        assertEquals("buyer@example.com", captor.getValue().getAllRecipients()[0].toString());
    }

    @Test
    void smtpFailureDoesNotFallbackToDemoCode() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        when(mailSender.createMimeMessage()).thenReturn(
                new MimeMessage(Session.getInstance(new Properties())));
        doThrow(new MailSendException("SMTP authentication failed"))
                .when(mailSender).send(any(MimeMessage.class));

        VerifyCodeService configured = new VerifyCodeService(
                mailSender, "smtp.qq.com", "sender@qq.com", "smtp-app-password", true);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> configured.send(12L, "email", "buyer@example.com"));

        assertTrue(ex.getMessage().contains("邮件发送失败"));
    }

    @Test
    void differentTargetIndependent() {
        String c1 = service.send(1L, "phone", "13900000001");
        String c2 = service.send(1L, "phone", "13900000002");
        assertDoesNotThrow(() -> service.verify(1L, "phone", "13900000002", c2));
        assertDoesNotThrow(() -> service.verify(1L, "phone", "13900000001", c1));
    }
}
