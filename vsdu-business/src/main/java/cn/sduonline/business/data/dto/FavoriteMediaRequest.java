package cn.sduonline.business.data.dto;

import jakarta.validation.constraints.Positive;

public record FavoriteMediaRequest(
        @Positive(message = "收藏夹ID必须为正数") Long folderId
) {
}
