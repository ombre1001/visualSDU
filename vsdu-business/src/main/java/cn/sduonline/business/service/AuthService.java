package cn.sduonline.business.service;

import cn.sduonline.business.data.dto.RefreshTokenRequest;
import cn.sduonline.business.data.dto.RegisterRequest;
import cn.sduonline.business.data.enums.UserRole;
import cn.sduonline.business.data.po.User;
import cn.sduonline.business.data.vo.AuthResponse;
import cn.sduonline.business.mapper.UserMapper;
import cn.sduonline.common.exception.BizCode;
import cn.sduonline.common.exception.BizException;
import cn.sduonline.infrastructure.jwt.JwtTokenUtils;
import cn.sduonline.infrastructure.jwt.local.LocalJwtPayload;
import cn.sduonline.infrastructure.jwt.local.TokenRedisOperator;
import cn.sduonline.infrastructure.jwt.sdupass.SduPassClient;
import cn.sduonline.infrastructure.jwt.sdupass.SduPassJwtPayload;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final SduPassClient sduPassClient;
    private final JwtTokenUtils jwtTokenUtils;
    private final TokenRedisOperator tokenRedisOperator;
    private final PasswordEncoder passwordEncoder;

    public AuthResponse sdupassLogin(String code) {

        String sduPassJwt;
        try {
            sduPassJwt = sduPassClient.getToken(code).token();
        } catch (SduPassClient.SduPassClientException se) {
            throw new BizException(BizCode.SDUPASS_LOGIN_FAILED, se.getMessage());
        }


        SduPassJwtPayload jwtPayload =
                jwtTokenUtils.parseSduPassJwt(sduPassJwt);

        User u = userMapper.selectByCasId(jwtPayload.casID());

        if (u != null && u.getId() != null) {

            var localJwtPayload = LocalJwtPayload.builder()
                    .userId(u.getId())
                    .role(u.getRole().getValue())
                    .tokenVersion(u.getTokenVersion())
                    .build();

            String refreshToken = jwtTokenUtils.generateRefreshToken();
            tokenRedisOperator.storeRefreshToken(u.getId(), refreshToken);

            userMapper.loginRecord(u.getId());

            return new AuthResponse(
                    jwtTokenUtils.generateAccessToken(localJwtPayload),
                    refreshToken
            );
        } else {
            return register(new RegisterRequest(sduPassJwt));
        }
    }

    private String defaultNickname(String casId) {
        return "用户" + casId;
    }

    private static final Integer DEFAULT_TOKEN_VERSION = 0;

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
                .tokenVersion(DEFAULT_TOKEN_VERSION)
                .build();

        String refreshToken = jwtTokenUtils.generateRefreshToken();
        tokenRedisOperator.storeRefreshToken(user.getId(), refreshToken);

        return new AuthResponse(
                jwtTokenUtils.generateAccessToken(localJwtPayload),
                refreshToken
        );
    }

    public AuthResponse refresh(RefreshTokenRequest refreshTokenRequest) {

        Long userId = Optional.ofNullable(
                tokenRedisOperator.consumeRefreshToken(refreshTokenRequest.refreshToken())
        ).orElseThrow(() -> new BizException(BizCode.AUTH_REFRESH_TOKEN_INVALID));

        User u = Optional.ofNullable(
                userMapper.selectById(userId)
        ).orElseThrow(() -> new BizException(BizCode.AUTH_USER_NOT_FOUND));

        var localJwtPayload = LocalJwtPayload.builder()
                .userId(userId)
                .role(u.getRole().getValue())
                .tokenVersion(u.getTokenVersion())
                .build();

        String accessToken = jwtTokenUtils.generateAccessToken(localJwtPayload);
        String refreshToken = jwtTokenUtils.generateRefreshToken();
        tokenRedisOperator.storeRefreshToken(userId, refreshToken);

        return new AuthResponse(accessToken, refreshToken);
    }

    public void logout(RefreshTokenRequest refreshTokenRequest) {

        Long userId = tokenRedisOperator.consumeRefreshToken(refreshTokenRequest.refreshToken());
        if (userId == null) return;

        userMapper.increaseTokenVersion(userId);

        tokenRedisOperator.deleteTokenVersionCache(userId);
    }
}
