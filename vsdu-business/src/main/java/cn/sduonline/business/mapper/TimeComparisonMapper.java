package cn.sduonline.business.mapper;

import cn.sduonline.business.data.po.TimeComparison;
import cn.sduonline.business.data.projection.TimeComparisonSummaryRow;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TimeComparisonMapper extends BaseMapper<TimeComparison> {
    List<TimeComparisonSummaryRow> selectSummaryRows(
            @Param("locationId") Long locationId,
            @Param("limit") int limit
    );
}
