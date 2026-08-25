package cn.sduonline.infrastructure.jwt;

import cn.sduonline.infrastructure.jwt.local.LocalJwtPayload;
import cn.sduonline.infrastructure.jwt.local.LocalJwtProperties;
import cn.sduonline.infrastructure.jwt.sdupass.SduPassJwtPayload;
import cn.sduonline.infrastructure.jwt.sdupass.SduPassJwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import static cn.sduonline.infrastructure.jwt.local.LocalJwtPayload.ROLE_CLAIM_KEY;
import static cn.sduonline.infrastructure.jwt.local.LocalJwtPayload.TOKEN_VERSION_CLAIM_KEY;

@Slf4j
public class JwtTokenUtils {

    private final LocalJwtProperties localJwtProperties;
    private final SecretKey sduPassSecretKey;
    private final SecretKey localSecretKey;

    private final static String ALGO = "PBKDF2WithHmacSHA256";

    private final SecureRandom secureRandom = new SecureRandom();

    private static final int DEFAULT_REFRESH_TOKEN_BYTES = 32;
    private static final int DEFAULT_LOGIN_TICKET_BYTES = 24;

    public JwtTokenUtils(
            SduPassJwtProperties properties,
            LocalJwtProperties localJwtProperties
    ) throws Exception {

        this.localJwtProperties = localJwtProperties;

        KeySpec spec = new PBEKeySpec(
                properties.sduPassSecret().toCharArray(),
                properties.salt().getBytes(StandardCharsets.UTF_8),
                properties.iterationCound(),
                properties.keyLength()
        );
        SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGO);

        this.sduPassSecretKey = Keys.hmacShaKeyFor(
                factory.generateSecret(spec).getEncoded()
        );

        this.localSecretKey = Keys.hmacShaKeyFor(
                Decoders.BASE64.decode(localJwtProperties.localSecret())
        );
    }

    public String generateAccessToken(LocalJwtPayload payload) {
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(payload.userId().toString())
                .claim(ROLE_CLAIM_KEY, payload.role())
                .claim(TOKEN_VERSION_CLAIM_KEY, payload.tokenVersion())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(localJwtProperties.accessTokenExpireSeconds())))
                .signWith(localSecretKey, Jwts.SIG.HS256)
                .compact();
    }

    public LocalJwtPayload parseAccessToken(String jwsToken) {
        Claims claims = Jwts.parser().verifyWith(localSecretKey)
                .build()
                .parseSignedClaims(jwsToken)
                .getPayload();

        return LocalJwtPayload.fromClaims(claims);
    }

    public SduPassJwtPayload parseSduPassJwt(String sduPassJwt) {
        Claims claims = Jwts.parser().verifyWith(sduPassSecretKey)
                .build()
                .parseSignedClaims(sduPassJwt)
                .getPayload();

        return SduPassJwtPayload.fromClaims(claims);
    }

    public String generateRefreshToken() {
        return generateRandomToken(DEFAULT_REFRESH_TOKEN_BYTES);
    }

    public String generateLoginTicket() {
        return generateRandomToken(DEFAULT_LOGIN_TICKET_BYTES);
    }

    public String generateRandomToken(int byteLength) {
        byte[] bytes = new byte[byteLength];
        secureRandom.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }


}
