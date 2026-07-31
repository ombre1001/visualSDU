package cn.sduonline.infrastructure.jwt;

import cn.sduonline.infrastructure.jwt.sdupass.SduPassJwtPayload;
import cn.sduonline.infrastructure.jwt.sdupass.SduPassJwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.spec.KeySpec;

@Slf4j
public class JwtTokenUtils {

    private final LocalJwtProperties localJwtProperties;
    private final SecretKey sduPassSecretKey;
//    private final SecretKey localSecretKey;

    private final static String ALGO = "PBKDF2WithHmacSHA256";

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
    }

    public SduPassJwtPayload parseSduPassJwt(String sduPassJwt) {
        Claims claims = Jwts.parser().verifyWith(sduPassSecretKey)
                .build()
                .parseSignedClaims(sduPassJwt)
                .getPayload();

        return SduPassJwtPayload.fromClaims(claims);
    }


}
