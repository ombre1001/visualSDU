package cn.sduonline.business.mapper;

import cn.sduonline.business.data.po.Campus;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CampusMapper extends BaseMapper<Campus> {

    List<Campus> selectEnabledCampuses(
            @Param("keyword") String keyword,
            @Param("cityId") Long cityId,
            @Param("limit") int limit
    );
}
