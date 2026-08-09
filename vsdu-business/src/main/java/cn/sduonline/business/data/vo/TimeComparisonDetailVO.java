package cn.sduonline.business.data.vo;

import java.util.List;

public record TimeComparisonDetailVO(
        Long id,
        Long locationId,
        String locationName,
        String title,
        String description,
        List<Item> items
) {
    public record Item(String label, MediaDetailVO media) {
    }
}
