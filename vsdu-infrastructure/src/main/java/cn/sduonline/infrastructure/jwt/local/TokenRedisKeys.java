package cn.sduonline.infrastructure.jwt.local;

import cn.sduonline.infrastructure.redis.RedisKeys;

public class TokenRedisKeys extends RedisKeys {

    /**
     * db:auth:token-version:{userId}
     */
    public static String tokenVersionKey(Long userId) {
        return build("auth", "token-version", userId.toString());
    }

    /**
     * db:auth:refresh:{refreshTokenHash}
     */
    public static String refreshTokenKey(String refreshTokenHash) {
        return build("auth", "refresh", refreshTokenHash);
    }

}
