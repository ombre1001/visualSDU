package cn.sduonline.infrastructure.jwt.local;

import cn.sduonline.infrastructure.redis.RedisKeys;

public class TokenRedisKeys extends RedisKeys {

    /**
     * vsdu:auth:tv:{userId}
     */
    public static String tokenVersionKey(Long userId) {
        return build("auth", "tv", userId.toString());
    }

    /**
     * vsdu:auth:rt:{refreshTokenHash}
     */
    public static String refreshTokenKey(String refreshTokenHash) {
        return build("auth", "rt", refreshTokenHash);
    }

    public static String refreshTokenKeyPrefix() {
        return build("auth", "rt") + ":";
    }

    /**
     * vsdu:auth:rt-set:{userId}
     */
    public static String userRefreshTokenSetKey(Long userId) {
        return build("auth", "rt-set", userId.toString());
    }

    /**
     * vsdu:auth:lt:{loginTicketHash}
     */
    public static String loginTicketKey(String loginTicketHash) {
        return build("auth", "lt", loginTicketHash);
    }

}
