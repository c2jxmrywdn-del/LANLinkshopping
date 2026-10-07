package com.lanlink.shopping.common;

import com.lanlink.shopping.config.AppCryptoProperties;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 敏感字段加密工具单元测试：AES-256-GCM 往返、存量明文兼容、随机 IV、篡改检测、密钥隔离与脱敏。
 */
class CryptoUtilTest {

    private static CryptoUtil util(String key) {
        AppCryptoProperties props = new AppCryptoProperties();
        props.setKey(key);
        return new CryptoUtil(props);
    }

    @Test
    void encryptDecryptRoundTripForPassphraseKey() {
        CryptoUtil c = util("unit-test-passphrase");
        String plain = "91500000MA5U0000XA";
        String cipher = c.encrypt(plain);

        assertNotEquals(plain, cipher, "密文不应等于明文");
        assertTrue(cipher.startsWith("enc:"), "密文必须带 enc: 前缀，供读侧识别");
        assertEquals(plain, c.decrypt(cipher), "解密应还原明文");
    }

    @Test
    void encryptDecryptRoundTripForBase64Key() {
        // Base64(32 字节) 密钥：直接作为 AES-256 密钥使用
        byte[] raw = new byte[32];
        for (int i = 0; i < raw.length; i++) raw[i] = (byte) (i + 1);
        CryptoUtil c = util(Base64.getEncoder().encodeToString(raw));

        String plain = "重庆华晨建材集团有限公司";
        assertEquals(plain, c.decrypt(c.encrypt(plain)));
    }

    @Test
    void legacyPlaintextReturnedAsIs() {
        CryptoUtil c = util("unit-test-passphrase");
        // 无 enc: 前缀的历史明文：原样返回，保证存量数据可读
        assertEquals("91500000MA5U0000XA", c.decrypt("91500000MA5U0000XA"));
        assertNull(c.decrypt(null));
        assertFalse(c.isEncrypted("91500000MA5U0000XA"));
        assertTrue(c.isEncrypted("enc:abc"));
    }

    @Test
    void encryptionIsIdempotentForAlreadyEncryptedValue() {
        CryptoUtil c = util("unit-test-passphrase");
        String once = c.encrypt("91500000MA5U0000XA");
        assertEquals(once, c.encrypt(once), "对密文再次加密应幂等，避免双重加密导致无法解密");
    }

    @Test
    void nullAndEmptyValuesStayUntouched() {
        CryptoUtil c = util("unit-test-passphrase");
        assertNull(c.encrypt(null));
        assertEquals("", c.encrypt(""));
    }

    @Test
    void randomIvMakesCipherTextUnique() {
        CryptoUtil c = util("unit-test-passphrase");
        String a = c.encrypt("91500000MA5U0000XA");
        String b = c.encrypt("91500000MA5U0000XA");

        assertNotEquals(a, b, "每次加密应使用随机 IV，相同明文得到不同密文");
        assertEquals(c.decrypt(a), c.decrypt(b));
    }

    @Test
    void tamperedCipherTextIsRejected() {
        CryptoUtil c = util("unit-test-passphrase");
        String cipher = c.encrypt("91500000MA5U0000XA");

        byte[] all = Base64.getDecoder().decode(cipher.substring(4));
        all[all.length - 1] ^= 0x01; // 翻转密文末字节，破坏 GCM 认证标签
        String tampered = "enc:" + Base64.getEncoder().encodeToString(all);

        assertThrows(IllegalStateException.class, () -> c.decrypt(tampered),
                "GCM 应校验完整性，篡改密文必须解密失败");
    }

    @Test
    void differentKeysCannotMutuallyDecrypt() {
        CryptoUtil a = util("key-a");
        CryptoUtil b = util("key-b");
        String cipher = a.encrypt("91500000MA5U0000XA");

        assertThrows(IllegalStateException.class, () -> b.decrypt(cipher),
                "密钥不匹配时解密必须失败，不得返回错误明文");
    }

    @Test
    void blankKeyFallsBackToDevKeyAndStaysInteroperable() {
        // 未配置密钥：回退内置演示密钥，保证本地开箱可运行（生产由环境变量 MERCHANT_CRYPTO_KEY 覆盖）
        CryptoUtil c = util("");
        assertEquals("91500000MA5U0000XA", c.decrypt(c.encrypt("91500000MA5U0000XA")));
        // 两个独立实例（同一回退密钥）应可互相解密：保证重启后历史密文仍可读
        assertEquals("X", util("").decrypt(util("").encrypt("X")));
    }

    @Test
    void maskKeepsFirstThreeAndLastFour() {
        assertEquals("915****00XA", CryptoUtil.mask("91500000MA5U0000XA"));
        assertEquals("137****0002", CryptoUtil.mask("13700000002"));
    }

    @Test
    void maskHandlesShortAndEmptyValues() {
        assertEquals("****", CryptoUtil.mask("12345678"), "长度 ≤8 应整体掩码，不泄露任何字符");
        assertEquals("****", CryptoUtil.mask("abc"));
        assertNull(CryptoUtil.mask(null));
        assertEquals("", CryptoUtil.mask(""));
    }

    @Test
    void isMaskedDetectsPlaceholderValues() {
        // 用于拦截「脱敏值被回填提交」：掩码结果必须被识别为占位值
        assertTrue(CryptoUtil.isMasked(CryptoUtil.mask("91500000MA5U0000XA")));
        assertTrue(CryptoUtil.isMasked("915****00XA"));
        assertFalse(CryptoUtil.isMasked("91500000MA5U0000XA"), "真实值不应被误判");
        assertFalse(CryptoUtil.isMasked("enc:AAAABBBBCCCC"));
        assertFalse(CryptoUtil.isMasked(null));
        assertFalse(CryptoUtil.isMasked(""));
    }
}
