package cn.sduonline.business.mapper;

import cn.sduonline.business.data.po.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface UserMapper extends BaseMapper<User> {

    @Select("select u.token_version from user u where id = #{userId}")
    Integer selectTokenVersionById(Long userId);

    @Update("update user set last_login_at = CURRENT_TIMESTAMP where id = #{userId}")
    void loginRecord(Long userId);

    void increaseTokenVersion(Long userId);

    void updateStatusAndIncreaseTokenVersion(
            @Param("userId") Long userId,
            @Param("fromStatus") Integer fromStatus,
            @Param("toStatus") Integer toStatus
    );

    User selectByCasId(String casId);

    boolean existsCasId(String casId);

    boolean existsPhone(String phone);

    void updatePhoneById(Long userId, String phone);
}
