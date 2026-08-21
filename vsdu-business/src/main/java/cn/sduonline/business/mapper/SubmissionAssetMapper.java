package cn.sduonline.business.mapper;

import cn.sduonline.business.data.po.SubmissionAsset;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SubmissionAssetMapper extends BaseMapper<SubmissionAsset> {

    @Delete("DELETE FROM submission_asset WHERE media_id = #{mediaId}")
    int deleteByMedia(@Param("mediaId") Long mediaId);
}
