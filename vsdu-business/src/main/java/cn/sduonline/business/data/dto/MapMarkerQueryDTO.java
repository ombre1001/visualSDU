package cn.sduonline.business.data.dto;

import lombok.Data;

@Data
public class MapMarkerQueryDTO {

    /**
     * 查询城市下的校区点位时传递。
     */
    private Long cityId;

    /**
     * 查询校区下的地点点位时传递。
     */
    private Long campusId;
}