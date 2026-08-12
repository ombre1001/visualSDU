package cn.sduonline.infrastructure.jwt.local;

import cn.sduonline.infrastructure.redis.RedisClient;
import cn.sduonline.infrastructure.utils.HashUtils;
import lombok.RequiredArgsConstructor;

import java.time.Duration;
import java.util.Optional;

@RequiredArgsConstructor
public class TokenRedisOperator {

    private final RedisClient redis;
    private final LocalJwtProperties localJwtProperties;

    public void storeRefreshToken(Long userId, String refreshToken) {
        String refreshTokenHash = HashUtils.sha256Hex(refreshToken);
        redis.set(
                TokenRedisKeys.refreshTokenKey(refreshTokenHash),
                userId.toString(),
                Duration.ofSeconds(localJwtProperties.refreshTokenExpireSeconds())
        );
    }

    public Long consumeRefreshToken(String refreshToken) {
        String refreshTokenHash = HashUtils.sha256Hex(refreshToken);
        String idStr = redis.getdel(TokenRedisKeys.refreshTokenKey(refreshTokenHash));

        return idStr == null ? null : Long.valueOf(idStr);
    }

    public void storeTokenVersion(Long userId, Integer version) {
        redis.set(TokenRedisKeys.tokenVersionKey(userId), version.toString());
    }

    public Optional<Integer> getTokenVersion(Long userId) {
        return Optional.ofNullable(redis.get(TokenRedisKeys.tokenVersionKey(userId)))
                .map(Integer::valueOf);
    }

    public void deleteTokenVersionCache(Long userId) {
        redis.del(TokenRedisKeys.tokenVersionKey(userId));
    }

}
