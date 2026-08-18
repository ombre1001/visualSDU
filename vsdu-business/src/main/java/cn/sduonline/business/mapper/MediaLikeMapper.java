package cn.sduonline.business.mapper;

import cn.sduonline.business.data.po.MediaLike;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MediaLikeMapper extends BaseMapper<MediaLike> {

    @Insert("INSERT IGNORE INTO media_like(user_id, media_id) VALUES(#{userId}, #{mediaId})")
    int insertIgnore(@Param("userId") Long userId, @Param("mediaId") Long mediaId);

    @Delete("DELETE FROM media_like WHERE user_id = #{userId} AND media_id = #{mediaId}")
    int deleteRelation(@Param("userId") Long userId, @Param("mediaId") Long mediaId);

    @Delete("DELETE FROM media_like WHERE media_id = #{mediaId}")
    int deleteByMedia(@Param("mediaId") Long mediaId);
}
