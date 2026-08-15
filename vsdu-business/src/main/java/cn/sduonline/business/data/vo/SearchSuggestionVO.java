package cn.sduonline.business.data.vo;

public record SearchSuggestionVO(
        String type,
        Long id,
        String text,
        String subtitle
) {
}