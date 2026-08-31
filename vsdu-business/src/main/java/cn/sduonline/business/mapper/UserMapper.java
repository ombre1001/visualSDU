package cn.sduonline.business.mapper;

import cn.sduonline.business.data.dto.AdminUpdateUserPermissionRequest;
import cn.sduonline.business.data.po.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

public interface UserMapper extends BaseMapper<User> {

    User selectByIdForUpdate(@Param("userId") Long userId);

    long countAdmin(
            @Param("keyword") String keyword,
            @Param("role") Integer role,
            @Param("status") Integer status
    );

    List<User> selectAdminPage(
            @Param("keyword") String keyword,
            @Param("role") Integer role,
            @Param("status") Integer status,
            @Param("offset") long offset,
            @Param("size") long size
    );

    int updateRoleAndIncreaseTokenVersion(
            @Param("userId") Long userId,
            @Param("expectedRole") Integer expectedRole,
            @Param("newRole") Integer newRole,
            @Param("updatedAt") LocalDateTime updatedAt
    );

    int updateAdminStatusAndIncreaseTokenVersion(
            @Param("userId") Long userId,
            @Param("status") Integer status,
            @Param("frozenUntil") LocalDateTime frozenUntil,
            @Param("frozenReason") String frozenReason,
            @Param("updatedAt") LocalDateTime updatedAt
    );

    int updatePermissionsPartial(
            @Param("userId") Long userId,
            @Param("request") AdminUpdateUserPermissionRequest request,
            @Param("updatedAt") LocalDateTime updatedAt
    );

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

    void userUnfrozen(Long userId);

    User selectByCasId(String casId);

    boolean existsCasId(String casId);

    boolean existsPhone(String phone);

    void updatePhoneById(
            @Param("userId") String userId,
            @Param("phone") String phone
    );

    long countRegisteredBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    long countRegisteredTotal();
}
