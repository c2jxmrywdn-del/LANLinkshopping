package com.lanlink.shopping.service;

import com.lanlink.shopping.common.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证码服务：发送 / 校验 / 单次使用 / 冷却
 */
class VerifyCodeServiceTest {

    private VerifyCodeService service;

    @BeforeEach
    void setUp() {
        // 测试场景走 phone 演示分支，mailSender 传 null 即可
        service = new VerifyCodeService(null, "", "", true);
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
        // 二次使用应失败
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
        VerifyCodeService production = new VerifyCodeService(null, "", "", false);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> production.send(7L, "phone", "13900000007"));
        assertTrue(ex.getMessage().contains("短信验证码服务暂未配置"));
    }

    @Test
    void productionDoesNotEchoEmailCodeWhenSmtpIsNotConfigured() {
        VerifyCodeService production = new VerifyCodeService(null, "", "", false);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> production.send(8L, "email", "buyer@example.com"));
        assertTrue(ex.getMessage().contains("邮箱验证码服务暂未配置"));
    }

    @Test
    void differentTargetIndependent() {
        String c1 = service.send(1L, "phone", "13900000001");
        String c2 = service.send(1L, "phone", "13900000002");
        assertDoesNotThrow(() -> service.verify(1L, "phone", "13900000002", c2));
        assertDoesNotThrow(() -> service.verify(1L, "phone", "13900000001", c1));
    }
}
