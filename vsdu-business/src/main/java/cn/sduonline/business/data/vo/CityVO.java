package cn.sduonline.business.data.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CityVO {

    private Long id;

    private String name;

    private String code;

    private String province;

    private String coverUrl;

    private String description;

    private Long campusCount;
}