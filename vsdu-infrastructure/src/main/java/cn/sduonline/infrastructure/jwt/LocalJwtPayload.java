package cn.sduonline.infrastructure.jwt;

import io.jsonwebtoken.Claims;
import lombok.Builder;

@Builder
public record LocalJwtPayload(
        Long userId,
        Integer role,
        Integer tokenVersion
) {

    public static final String ROLE_CLAIM_KEY = "role";
    public static final String TOKEN_VERSION_CLAIM_KEY = "tv";

    public static LocalJwtPayload fromClaims(Claims claims) {
        return new LocalJwtPayload(
                Long.valueOf(claims.getSubject()),
                claims.get(ROLE_CLAIM_KEY, Integer.class),
                claims.get(TOKEN_VERSION_CLAIM_KEY, Integer.class)
        );
    }
}
