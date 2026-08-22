package cn.sduonline.business.mapper;

import cn.sduonline.business.data.dto.AdminUpdateLocationRequest;
import cn.sduonline.business.data.projection.SearchSuggestionRow;
import cn.sduonline.business.data.po.Location;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.time.LocalDateTime;

@Mapper
public interface LocationMapper extends BaseMapper<Location> {

    List<SearchSuggestionRow> selectEnabledSuggestions(
            @Param("keyword") String keyword,
            @Param("limit") int limit
    );

    int updatePartial(
            @Param("id") Long id,
            @Param("request") AdminUpdateLocationRequest request,
            @Param("value") Location value,
            @Param("updatedAt") LocalDateTime updatedAt
    );
}
