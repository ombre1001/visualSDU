package cn.sduonline.business.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MediaDownloadMapper {

    @Insert("INSERT INTO media_download(user_id, media_id, object_key) VALUES(#{userId}, #{mediaId}, #{objectKey})")
    int insertRecord(@Param("userId") Long userId,
                     @Param("mediaId") Long mediaId,
                     @Param("objectKey") String objectKey);

    @Delete("DELETE FROM media_download WHERE media_id = #{mediaId}")
    int deleteByMedia(@Param("mediaId") Long mediaId);
}
