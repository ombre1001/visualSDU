package cn.sduonline.business.util;

import cn.sduonline.business.data.dto.MediaTicketData;
import cn.sduonline.infrastructure.jwt.JwtTokenUtils;
import cn.sduonline.infrastructure.redis.RedisClient;
import cn.sduonline.infrastructure.redis.RedisKeys;
import cn.sduonline.infrastructure.utils.HashUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

/**
 * 整合MediaTicket的获取、存储、消费以及与之相关的redis操作。
 */
@Component
@RequiredArgsConstructor
public class MediaTicketUtils {

    private final RedisClient redis;
    private final JwtTokenUtils jwtTokenUtils;
    private final ObjectMapper objectMapper;

    @Value("${vsdu.media.ticket-expire-seconds}")
    private Long mediaTicketExpireSeconds;

    public String getStoredMediaTicket(MediaTicketData data) {

        String mediaTicket = jwtTokenUtils.generateMediaDownloadTicket();
        String mediaTicketHash = HashUtils.sha256Hex(mediaTicket);

        redis.set(
                MediaTicketRedisKeys.mediaTicketKey(mediaTicketHash),
                objectMapper.writeValueAsString(data),
                Duration.ofSeconds(mediaTicketExpireSeconds)
        );

        return mediaTicket;
    }

    public MediaTicketData consumeStoredMediaTicket(String mediaTicket) {

        String mediaTicketHash = HashUtils.sha256Hex(mediaTicket);
        String dataJson = redis.getdel(
                MediaTicketRedisKeys.mediaTicketKey(mediaTicketHash)
        );

        return dataJson == null
                ? null
                : objectMapper.readValue(dataJson, MediaTicketData.class);
    }

    static class MediaTicketRedisKeys extends RedisKeys {

        /**
         * vsdu:media:ticket:{mediaTicketHash}
         */
        static String mediaTicketKey(String mediaTicketHash) {
            return build("media", "ticket", mediaTicketHash);
        }
    }

}
