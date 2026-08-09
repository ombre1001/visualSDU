package cn.sduonline.business.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserBrowseHistoryMapper {

    @Insert("INSERT INTO user_browse_history(user_id, media_id) VALUES(#{userId}, #{mediaId}) " +
            "ON DUPLICATE KEY UPDATE view_count = view_count + 1, last_viewed_at = CURRENT_TIMESTAMP")
    int upsertView(@Param("userId") Long userId, @Param("mediaId") Long mediaId);
}
