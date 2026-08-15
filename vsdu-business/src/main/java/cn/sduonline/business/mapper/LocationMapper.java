package cn.sduonline.business.mapper;

import cn.sduonline.business.data.po.Location;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LocationMapper extends BaseMapper<Location> {

    List<Location> selectEnabledSuggestions(
            @Param("keyword") String keyword,
            @Param("limit") int limit
    );
}
