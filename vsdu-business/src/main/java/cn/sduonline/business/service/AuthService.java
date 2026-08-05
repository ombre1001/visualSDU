package cn.sduonline.business.service;

import cn.sduonline.business.data.dto.RegisterRequest;
import cn.sduonline.business.data.po.User;
import cn.sduonline.business.data.vo.AuthResponse;
import cn.sduonline.business.mapper.UserMapper;
import cn.sduonline.common.exception.BizCode;
import cn.sduonline.common.exception.BizException;
import cn.sduonline.infrastructure.jwt.JwtTokenUtils;
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


        return null;
    }

    private String defaultNickname(String casId) {
        return "用户" + casId;
    }

    public AuthResponse register(RegisterRequest registerRequest) {
        SduPassJwtPayload jwtPayload = null;
        try {
            jwtPayload =
                    jwtTokenUtils.parseSduPassJwt(registerRequest.sduPassJwt());
        } catch (JwtException je) {
            throw new BizException(BizCode.REGISTRATION_SDUPASS_JWT_INVALID);
        }

        String passwordRaw = registerRequest.password();
        if (!registerRequest.confirmPassword().equals(passwordRaw)) {
            throw new BizException(BizCode.REGISTRATION_PASSWORD_CONFIRM_FAILED);
        }

        String passwordHash = passwordEncoder.encode(passwordRaw);

        User user = User.builder()
                .passwordHash(passwordHash)
                .casId(jwtPayload.casID())
                .name(jwtPayload.name())
                .nickname(defaultNickname(jwtPayload.casID()))
                .build();
        userMapper.insert(user);

        Long userId = user.getId();

    }
}
