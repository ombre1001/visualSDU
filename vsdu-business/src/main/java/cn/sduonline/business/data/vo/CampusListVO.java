package cn.sduonline.business.data.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CampusListVO {

    private Long id;

    private Long cityId;

    private String name;

    private String shortName;

    private String address;

    private BigDecimal longitude;

    private BigDecimal latitude;

    private String coverUrl;
}