package com.ljl.agent.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 密码哈希工具。
 *
 * <p>使用 PBKDF2WithHmacSHA256。
 * 数据库存储格式：
 * pbkdf2_sha256$迭代次数$salt$hash</p>
 */
public final class PasswordUtils {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String PREFIX = "pbkdf2_sha256";

    private static final int ITERATIONS = 120_000;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_LENGTH = 16;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private PasswordUtils() {
    }

    public static String hash(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("密码不能为空");
        }

        byte[] salt = new byte[SALT_LENGTH];
        SECURE_RANDOM.nextBytes(salt);

        byte[] hash = derive(
                rawPassword.toCharArray(),
                salt,
                ITERATIONS,
                KEY_LENGTH
        );

        Base64.Encoder encoder = Base64.getEncoder().withoutPadding();

        return PREFIX
                + "$" + ITERATIONS
                + "$" + encoder.encodeToString(salt)
                + "$" + encoder.encodeToString(hash);
    }

    /**
     * 为后续阶段 2 登录密码校验预留。
     */
    public static boolean matches(String rawPassword, String storedPasswordHash) {
        if (rawPassword == null
                || rawPassword.isBlank()
                || storedPasswordHash == null
                || storedPasswordHash.isBlank()) {
            return false;
        }

        String[] parts = storedPasswordHash.split("\\$");
        if (parts.length != 4 || !PREFIX.equals(parts[0])) {
            return false;
        }

        try {
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[3]);

            byte[] actualHash = derive(
                    rawPassword.toCharArray(),
                    salt,
                    iterations,
                    expectedHash.length * 8
            );

            return MessageDigest.isEqual(expectedHash, actualHash);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static byte[] derive(
            char[] password,
            byte[] salt,
            int iterations,
            int keyLength) {

        PBEKeySpec keySpec =
                new PBEKeySpec(password, salt, iterations, keyLength);

        try {
            SecretKeyFactory factory =
                    SecretKeyFactory.getInstance(ALGORITHM);

            return factory.generateSecret(keySpec).getEncoded();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("密码哈希处理失败", exception);
        } finally {
            keySpec.clearPassword();
        }
    }
}