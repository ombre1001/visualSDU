package cn.sduonline.business.data.dto;

import lombok.Builder;
import lombok.NonNull;

@Builder
public record MediaTicketData(
        @NonNull Long userId,
        @NonNull Long mediaId,
        @NonNull String objectKey
) {
}
