package cn.sduonline.business.data.projection;

import lombok.Data;

@Data
public class CitySummaryRow {
    private Long id;
    private String name;
    private String code;
    private String province;
    private String coverUrl;
    private String description;
    private Long campusCount;
}
