package cn.sduonline.business.mapper;

import cn.sduonline.business.data.po.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;

public interface UserMapper extends BaseMapper<User> {

    @Select("select u.token_version from user u where id = #{userId}")
    Integer selectTokenVersionById(Long userId);

    User selectByCasId(String casId);

    boolean existsCasId(String casId);

    boolean existsPhone(String phone);

    void updatePhoneById(Long userId, String phone);
}
