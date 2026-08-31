package cn.sduonline.business.data.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class LocationDetailVO {

    private Long id;

    private Long campusId;

    private String campusName;

    private Long cityId;

    private String cityName;

    private String name;

    private String categoryCode;

    private String address;

    private BigDecimal longitude;

    private BigDecimal latitude;

    private String coverUrl;

    private String description;

    private long favoriteCount;

    private boolean favorited;
}

