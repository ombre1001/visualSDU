package cn.sduonline.business.mapper;

import cn.sduonline.business.data.po.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

public interface UserMapper extends BaseMapper<User> {

    User selectByCasId(String casId);

    boolean existsCasId(String casId);

    boolean existsPhone(String phone);

    void updatePhoneById(Long userId, String phone);
}
