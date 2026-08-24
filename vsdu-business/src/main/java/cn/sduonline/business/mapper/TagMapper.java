package cn.sduonline.business.mapper;

import cn.sduonline.business.data.po.Tag;
import cn.sduonline.business.data.projection.AdminTagStatRow;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TagMapper extends BaseMapper<Tag> {
    List<AdminTagStatRow> selectAdminTagStats(@Param("keyword") String keyword);

    List<String> selectUsedTagSuggestions(
            @Param("keyword") String keyword,
            @Param("limit") int limit
    );
}
