package cn.sduonline.business.mapper;

import cn.sduonline.business.data.po.MediaTag;
import cn.sduonline.business.data.projection.PopularTagStatRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

@Mapper
public interface MediaTagMapper {
    List<MediaTag> selectByMediaId(@Param("mediaId") Long mediaId);

    List<MediaTag> selectByMediaIds(@Param("mediaIds") Collection<Long> mediaIds);

    List<Long> selectMediaIdsByTagId(@Param("tagId") Long tagId);

    List<MediaTag> selectByMediaIdsForUpdate(@Param("mediaIds") Collection<Long> mediaIds);

    int insertBatch(@Param("relations") List<MediaTag> relations);

    int deleteByMediaId(@Param("mediaId") Long mediaId);

    int deleteByMediaIds(@Param("mediaIds") Collection<Long> mediaIds);

    long countByTagId(@Param("tagId") Long tagId);

    Long selectFirstTagId(@Param("mediaId") Long mediaId);

    List<PopularTagStatRow> selectPopularTags(
            @Param("cityId") Long cityId,
            @Param("tagLimit") int tagLimit
    );

    int deleteByTagId(@Param("tagId") Long tagId);
}
