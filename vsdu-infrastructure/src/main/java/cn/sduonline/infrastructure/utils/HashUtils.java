package cn.sduonline.infrastructure.utils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

public final class HashUtils {

    private static final String SHA_256 = "SHA-256";
    private static final String HMAC_SHA256 = "HmacSHA256";

    private static final HexFormat HEX = HexFormat.of();

    private HashUtils() {}

    public static String sha256Hex(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA_256);
            byte[] bytes = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HEX.formatHex(bytes);
        } catch (Exception e) {
            throw new IllegalArgumentException("HSA-256 hash failed", e);
        }
    }

    public static String hmacSha256Hex(String raw, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec keySpec = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8),
                    HMAC_SHA256
            );
            mac.init(keySpec);

            byte[] bytes = mac.doFinal(raw.getBytes(StandardCharsets.UTF_8));
            return HEX.formatHex(bytes);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC-SHA256 hash failed", e);
        }
    }
}
