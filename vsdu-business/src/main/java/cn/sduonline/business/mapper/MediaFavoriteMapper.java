package cn.sduonline.business.mapper;

import cn.sduonline.business.data.po.MediaFavorite;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MediaFavoriteMapper extends BaseMapper<MediaFavorite> {

    @Insert("INSERT IGNORE INTO media_favorite(user_id, folder_id, media_id) " +
            "VALUES(#{userId}, #{folderId}, #{mediaId})")
    int insertIgnore(@Param("userId") Long userId,
                     @Param("folderId") Long folderId,
                     @Param("mediaId") Long mediaId);

    @Delete("DELETE FROM media_favorite WHERE user_id = #{userId} AND media_id = #{mediaId}")
    int deleteAllForUser(@Param("userId") Long userId, @Param("mediaId") Long mediaId);
}
