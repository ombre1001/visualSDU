package cn.sduonline.business.mapper;

import cn.sduonline.business.data.po.TimeComparisonItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TimeComparisonItemMapper extends BaseMapper<TimeComparisonItem> {

    @Select("SELECT DISTINCT comparison_id FROM time_comparison_item WHERE media_id = #{mediaId}")
    List<Long> selectComparisonIdsByMedia(@Param("mediaId") Long mediaId);

    @Delete("DELETE FROM time_comparison_item WHERE media_id = #{mediaId}")
    int deleteByMedia(@Param("mediaId") Long mediaId);

    @Select("SELECT COUNT(*) FROM time_comparison_item WHERE comparison_id = #{comparisonId}")
    long countByComparison(@Param("comparisonId") Long comparisonId);

    @Delete("DELETE FROM time_comparison_item WHERE comparison_id = #{comparisonId}")
    int deleteByComparison(@Param("comparisonId") Long comparisonId);
}
