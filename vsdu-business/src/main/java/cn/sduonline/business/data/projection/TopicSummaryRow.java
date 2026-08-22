package cn.sduonline.business.data.projection;

import lombok.Data;

@Data
public class TopicSummaryRow {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private String coverUrl;
    private Long mediaCount;
}
