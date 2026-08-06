package cn.sduonline.business.service;

import cn.sduonline.business.data.dto.RegisterRequest;
import cn.sduonline.business.data.enums.UserRole;
import cn.sduonline.business.data.po.User;
import cn.sduonline.business.data.vo.AuthResponse;
import cn.sduonline.business.mapper.UserMapper;
import cn.sduonline.common.exception.BizCode;
import cn.sduonline.common.exception.BizException;
import cn.sduonline.infrastructure.jwt.JwtTokenUtils;
import cn.sduonline.infrastructure.jwt.LocalJwtPayload;
import cn.sduonline.infrastructure.jwt.sdupass.SduPassClient;
import cn.sduonline.infrastructure.jwt.sdupass.SduPassJwtPayload;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final SduPassClient sduPassClient;
    private final JwtTokenUtils jwtTokenUtils;
    private final PasswordEncoder passwordEncoder;

    public AuthResponse sdupassLogin(String code) {
        String sduPassJwt = sduPassClient.getToken(code).token();
        SduPassJwtPayload jwtPayload =
                jwtTokenUtils.parseSduPassJwt(sduPassJwt);

        User u = userMapper.selectByCasId(jwtPayload.casID());

        if (u != null && u.getId() != null) {

            var localJwtPayload = LocalJwtPayload.builder()
                    .userId(u.getId())
                    .role(u.getRole().getValue())
                    .tokenVersion(u.getTokenVersion())
                    .build();

            return new AuthResponse(
                    jwtTokenUtils.generateAccessToken(localJwtPayload),
                    "敬请期待"
            );
        } else {
            return register(new RegisterRequest(sduPassJwt));
        }
    }

    private String defaultNickname(String casId) {
        return "用户" + casId;
    }

    public AuthResponse register(RegisterRequest registerRequest) {
        SduPassJwtPayload jwtPayload;
        try {
            jwtPayload =
                    jwtTokenUtils.parseSduPassJwt(registerRequest.sduPassJwt());
        } catch (JwtException je) {
            throw new BizException(BizCode.REGISTRATION_SDUPASS_JWT_INVALID);
        }

        User user = User.builder()
                .casId(jwtPayload.casID())
                .name(jwtPayload.name())
                .nickname(defaultNickname(jwtPayload.casID()))
                .build();
        userMapper.insert(user);

        var localJwtPayload = LocalJwtPayload.builder()
                .userId(user.getId())
                .role(UserRole.USER.getValue())
                .tokenVersion(0)
                .build();

        return new AuthResponse(
                jwtTokenUtils.generateAccessToken(localJwtPayload),
                "敬请期待"
        );
    }
}
