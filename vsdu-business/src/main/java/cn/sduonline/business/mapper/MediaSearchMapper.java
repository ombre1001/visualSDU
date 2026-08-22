package cn.sduonline.business.mapper;

import cn.sduonline.business.data.dto.SearchMediaQueryDTO;
import cn.sduonline.business.data.projection.MediaSummaryRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MediaSearchMapper {

    long countSearchMedia(
            @Param("query") SearchMediaQueryDTO query
    );

    List<MediaSummaryRow> searchMedia(
            @Param("query") SearchMediaQueryDTO query,
            @Param("sort") String sort,
            @Param("offset") long offset,
            @Param("limit") long limit
    );
}
