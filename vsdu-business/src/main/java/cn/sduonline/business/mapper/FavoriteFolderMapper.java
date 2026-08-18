package cn.sduonline.business.mapper;

import cn.sduonline.business.data.po.FavoriteFolder;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface FavoriteFolderMapper extends BaseMapper<FavoriteFolder> {

    @Update("UPDATE favorite_folder SET cover_media_id = NULL WHERE cover_media_id = #{mediaId}")
    int clearCoverMedia(@Param("mediaId") Long mediaId);
}
