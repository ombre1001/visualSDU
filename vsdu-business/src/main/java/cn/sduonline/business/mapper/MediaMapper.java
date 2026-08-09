package cn.sduonline.business.mapper;

import cn.sduonline.business.data.po.Media;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface MediaMapper extends BaseMapper<Media> {

    @Update("UPDATE media SET view_count = view_count + 1 WHERE id = #{mediaId} AND status = 1")
    int increaseViewCount(@Param("mediaId") Long mediaId);

    @Update("UPDATE media SET like_count = like_count + #{delta} WHERE id = #{mediaId} AND status = 1")
    int increaseLikeCount(@Param("mediaId") Long mediaId, @Param("delta") long delta);

    @Update("UPDATE media SET like_count = GREATEST(like_count - #{delta}, 0) WHERE id = #{mediaId}")
    int decreaseLikeCount(@Param("mediaId") Long mediaId, @Param("delta") long delta);

    @Update("UPDATE media SET favorite_count = favorite_count + #{delta} WHERE id = #{mediaId} AND status = 1")
    int increaseFavoriteCount(@Param("mediaId") Long mediaId, @Param("delta") long delta);

    @Update("UPDATE media SET favorite_count = GREATEST(favorite_count - #{delta}, 0) WHERE id = #{mediaId}")
    int decreaseFavoriteCount(@Param("mediaId") Long mediaId, @Param("delta") long delta);

    @Update("UPDATE media SET download_count = download_count + 1 WHERE id = #{mediaId} AND status = 1")
    int increaseDownloadCount(@Param("mediaId") Long mediaId);
}
