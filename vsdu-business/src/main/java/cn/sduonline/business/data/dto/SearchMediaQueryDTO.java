package cn.sduonline.business.data.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SearchMediaQueryDTO {

    /**
     * 同时匹配媒体标题、描述、标签、地点、校区和城市。
     */
    @Size(max = 50, message = "搜索关键词长度不能超过50个字符")
    private String q;

    @Positive(message = "cityId必须为正整数")
    private Long cityId;

    @Positive(message = "campusId必须为正整数")
    private Long campusId;

    @Positive(message = "locationId必须为正整数")
    private Long locationId;

    @Positive(message = "topicId必须为正整数")
    private Long topicId;

    @Size(max = 30, message = "标签长度不能超过30个字符")
    private String tag;

    @Min(value = 1900, message = "拍摄年份不能早于1900年")
    @Max(value = 2100, message = "拍摄年份不能晚于2100年")
    private Integer shotYear;

    /**
     * relevance：相关度
     * newest：最新
     * oldest：最早
     * hot：最热门
     */
    private String sort = "relevance";

    @Min(value = 1, message = "page不能小于1")
    @Max(value = 10000, message = "page不能大于10000")
    private long page = 1;

    @Min(value = 1, message = "size不能小于1")
    @Max(value = 50, message = "size不能大于50")
    private long size = 20;
}