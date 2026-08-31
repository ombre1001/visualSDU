package cn.sduonline.business.data.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class LocationListVO {

    private Long id;

    private Long campusId;

    private String name;

    private String categoryCode;

    private String address;

    private BigDecimal longitude;

    private BigDecimal latitude;

    private String coverUrl;

    private long favoriteCount;

    private boolean favorited;
}
