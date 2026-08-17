package cn.sduonline.infrastructure.jwt.local;

import cn.sduonline.infrastructure.redis.RedisClient;
import cn.sduonline.infrastructure.utils.HashUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class TokenRedisOperator {

    private final RedisClient redis;
    private final LocalJwtProperties localJwtProperties;

    private static final RedisScript<Boolean> ROTATE_REFRESH_TOKEN_SCRIPT =
            RedisScript.of(
                    new ClassPathResource("redis/rotate-refresh-token.lua"),
                    Boolean.class
            );

    private static final RedisScript<Boolean> STORE_REFRESH_TOKEN_SCRIPT =
            RedisScript.of(
                    new ClassPathResource("redis/store-refresh-token.lua"),
                    Boolean.class
            );

    private static final RedisScript<Boolean> DELETE_REFRESH_TOKEN_SCRIPT =
            RedisScript.of(
                    new ClassPathResource("redis/del-refresh-token.lua"),
                    Boolean.class
            );

    private static final RedisScript<Long> DELETE_ALL_REFRESH_TOKENS_SCRIPT =
            RedisScript.of(
                    new ClassPathResource("redis/del-all-refresh-tokens.lua"),
                    Long.class
            );

    public boolean storeRefreshToken(Long userId, String refreshToken) {
        String refreshTokenHash = HashUtils.sha256Hex(refreshToken);

        String refreshTokenKey = TokenRedisKeys.refreshTokenKey(refreshTokenHash);
        String userRefreshTokenSetKey = TokenRedisKeys.userRefreshTokenSetKey(userId);
        return redis.execute(
                STORE_REFRESH_TOKEN_SCRIPT,
                List.of(refreshTokenKey, userRefreshTokenSetKey),
                userId.toString(), refreshTokenHash,
                localJwtProperties.refreshTokenExpireSeconds().toString()
        );
    }

    public Long getRefreshTokenOwnerId(String refreshToken) {
        String refreshTokenHash = HashUtils.sha256Hex(refreshToken);
        String idStr = redis.get(TokenRedisKeys.refreshTokenKey(refreshTokenHash));

        return idStr == null ? null : Long.valueOf(idStr);
    }

    public boolean rotateRefreshToken(Long userId, String oldRefreshToken, String newRefreshToken) {
        String oldRefreshTokenHash = HashUtils.sha256Hex(oldRefreshToken);
        String newRefreshTokenHash = HashUtils.sha256Hex(newRefreshToken);

        String oldRefreshTokenKey = TokenRedisKeys.refreshTokenKey(oldRefreshTokenHash);
        String newRefreshTokenKey = TokenRedisKeys.refreshTokenKey(newRefreshTokenHash);
        String userRefreshTokenSetKey = TokenRedisKeys.userRefreshTokenSetKey(userId);

        return redis.execute(
                ROTATE_REFRESH_TOKEN_SCRIPT,
                List.of(oldRefreshTokenKey, newRefreshTokenKey, userRefreshTokenSetKey),
                oldRefreshTokenHash, newRefreshTokenHash,
                localJwtProperties.refreshTokenExpireSeconds().toString(),
                userId.toString()
        );
    }

    public boolean deleteRefreshToken(Long userId, String refreshToken) {
        String refreshTokenHash = HashUtils.sha256Hex(refreshToken);

        String refreshTokenKey = TokenRedisKeys.refreshTokenKey(refreshTokenHash);
        String userRefreshTokenSetKey = TokenRedisKeys.userRefreshTokenSetKey(userId);

        return redis.execute(
                DELETE_REFRESH_TOKEN_SCRIPT,
                List.of(refreshTokenKey, userRefreshTokenSetKey),
                userId.toString(), refreshTokenHash
        );
    }

    public long deleteAllRefreshTokens(Long userId) {
        Long deleted = redis.execute(
                DELETE_ALL_REFRESH_TOKENS_SCRIPT,
                List.of(TokenRedisKeys.userRefreshTokenSetKey(userId)),
                TokenRedisKeys.refreshTokenKeyPrefix()
        );
        return deleted == null ? 0 : deleted;
    }

    public void storeTokenVersion(Long userId, Integer version) {
        redis.set(
                TokenRedisKeys.tokenVersionKey(userId),
                version.toString(),
                Duration.of(localJwtProperties.accessTokenExpireSeconds(), ChronoUnit.SECONDS)
        );
    }

    public Optional<Integer> getTokenVersion(Long userId) {
        return Optional.ofNullable(redis.get(TokenRedisKeys.tokenVersionKey(userId)))
                .map(Integer::valueOf);
    }

    public void deleteTokenVersionCache(Long userId) {
        redis.del(TokenRedisKeys.tokenVersionKey(userId));
    }

}
