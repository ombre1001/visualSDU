package cn.sduonline.business.mapper;

import cn.sduonline.business.data.po.Media;
import cn.sduonline.business.data.projection.MediaSummaryRow;
import cn.sduonline.business.data.projection.MediaTagPatch;
import cn.sduonline.business.data.projection.SearchSuggestionRow;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface MediaMapper extends BaseMapper<Media> {

    List<SearchSuggestionRow> selectSearchSuggestions(
            @Param("keyword") String keyword,
            @Param("limit") int limit
    );

    List<MediaSummaryRow> selectRelatedMedia(
            @Param("sourceId") Long sourceId,
            @Param("locationId") Long locationId,
            @Param("tag") String tag,
            @Param("limit") int limit
    );

    long countVisibleByLocation(@Param("locationId") Long locationId);

    List<MediaSummaryRow> selectVisibleByLocationPage(
            @Param("locationId") Long locationId,
            @Param("offset") long offset,
            @Param("size") long size
    );

    long countAdmin(
            @Param("keyword") String keyword,
            @Param("locationId") Long locationId,
            @Param("status") Integer status
    );

    List<Media> selectAdminPage(
            @Param("keyword") String keyword,
            @Param("locationId") Long locationId,
            @Param("status") Integer status,
            @Param("offset") long offset,
            @Param("size") long size
    );

    List<Media> selectByExactTag(@Param("tag") String tag);

    Media selectByIdForUpdate(@Param("mediaId") Long mediaId);

    int batchUpdateTags(
            @Param("patches") List<MediaTagPatch> patches,
            @Param("updatedAt") LocalDateTime updatedAt
    );

    @Update("UPDATE media SET view_count = view_count + 1 WHERE id = #{mediaId} AND status = 1")
    void increaseViewCount(@Param("mediaId") Long mediaId);

    @Update("UPDATE media SET like_count = like_count + #{delta} WHERE id = #{mediaId} AND status = 1")
    void increaseLikeCount(@Param("mediaId") Long mediaId, @Param("delta") long delta);

    @Update("UPDATE media SET like_count = GREATEST(like_count - #{delta}, 0) WHERE id = #{mediaId}")
    void decreaseLikeCount(@Param("mediaId") Long mediaId, @Param("delta") long delta);

    @Update("UPDATE media SET favorite_count = favorite_count + #{delta} WHERE id = #{mediaId} AND status = 1")
    void increaseFavoriteCount(@Param("mediaId") Long mediaId, @Param("delta") long delta);

    @Update("UPDATE media SET favorite_count = GREATEST(favorite_count - #{delta}, 0) WHERE id = #{mediaId}")
    void decreaseFavoriteCount(@Param("mediaId") Long mediaId, @Param("delta") long delta);

    @Update("UPDATE media SET download_count = download_count + 1 WHERE id = #{mediaId} AND status = 1")
    void increaseDownloadCount(@Param("mediaId") Long mediaId);
}
