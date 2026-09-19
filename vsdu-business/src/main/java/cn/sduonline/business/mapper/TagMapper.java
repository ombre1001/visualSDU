package cn.sduonline.business.mapper;

import cn.sduonline.business.data.po.Tag;
import cn.sduonline.business.data.projection.AdminTagStatRow;
import cn.sduonline.business.data.projection.SearchSuggestionRow;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

@Mapper
public interface TagMapper extends BaseMapper<Tag> {
    List<Tag> selectByIdsForUpdate(@Param("tagIds") Collection<Long> tagIds);

    long countPublicTags(@Param("keyword") String keyword);

    List<Tag> selectPublicTagPage(
            @Param("keyword") String keyword,
            @Param("sort") String sort,
            @Param("offset") long offset,
            @Param("size") long size
    );

    List<AdminTagStatRow> selectAdminTagStats(@Param("keyword") String keyword);

    List<SearchSuggestionRow> selectUsedTagSuggestions(
            @Param("keyword") String keyword,
            @Param("limit") int limit
    );
}
