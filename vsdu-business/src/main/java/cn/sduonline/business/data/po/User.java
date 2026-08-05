package cn.sduonline.business.data.po;

import cn.sduonline.business.data.enums.UserRole;
import cn.sduonline.business.data.enums.UserStatus;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("user")
public class User {

    private Long id;
    private String phone;
    private String passwordHash;
    private String casId;
    private String name;
    private String nickname;
    private String avatarKey;
    private String bio;
    private UserRole role;
    private UserStatus status;
    private LocalDateTime frozenUntil;
    private String frozenReason;
    private Boolean allowUpload;
    private Boolean allowDownload;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean deleted;
}