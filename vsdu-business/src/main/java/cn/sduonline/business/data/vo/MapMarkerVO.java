package cn.sduonline.business.data.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class MapMarkerVO {

    private Long id;

    /**
     * CAMPUS：校区点位
     * LOCATION：地点点位
     */
    private String markerType;

    private Long campusId;

    private String name;

    private BigDecimal longitude;

    private BigDecimal latitude;

    private String coverUrl;
}