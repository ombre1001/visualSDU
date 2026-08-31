package cn.sduonline.business.service;

import cn.sduonline.business.data.dto.LoginTicketRequest;
import cn.sduonline.business.data.dto.RefreshTokenRequest;
import cn.sduonline.business.data.dto.RegisterRequest;
import cn.sduonline.business.data.po.User;
import cn.sduonline.business.data.vo.AuthResponse;
import cn.sduonline.business.mapper.UserMapper;
import cn.sduonline.business.security.context.CurrentUser;
import cn.sduonline.common.exception.BizCode;
import cn.sduonline.common.exception.BizException;
import cn.sduonline.infrastructure.jwt.JwtTokenUtils;
import cn.sduonline.infrastructure.jwt.local.LocalJwtPayload;
import cn.sduonline.infrastructure.jwt.local.TokenRedisOperator;
import cn.sduonline.infrastructure.jwt.sdupass.SduPassClient;
import cn.sduonline.infrastructure.jwt.sdupass.SduPassJwtPayload;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final SduPassClient sduPassClient;
    private final JwtTokenUtils jwtTokenUtils;
    private final TokenRedisOperator tokenRedisOperator;

    private void frozenUserCheck(User u) {
        switch (u.getStatus()) {
            case FROZEN :
                if (u.getFrozenUntil() == null || LocalDateTime.now().isAfter(u.getFrozenUntil())) {
                    userMapper.userUnfrozen(u.getId());
                    break;
                }
            case DISABLED :
                throw new BizException(BizCode.FROZEN_USER);
        }
    }

    @Transactional
    public String sdupassCallback(String code) {

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

            frozenUserCheck(u);

            String loginTicket = jwtTokenUtils.generateLoginTicket();
            tokenRedisOperator.storeLoginTicket(u.getId(), loginTicket);

            return loginTicket;
        } else {
            return register(new RegisterRequest(sduPassJwt));
        }
    }

    private String defaultNickname(String casId) {
        return "用户" + casId;
    }

    public String register(RegisterRequest registerRequest) {
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

        String loginTicket = jwtTokenUtils.generateLoginTicket();
        tokenRedisOperator.storeLoginTicket(user.getId(), loginTicket);

        return loginTicket;
    }

    public AuthResponse login(LoginTicketRequest loginTicketRequest) {

        Long userId = Optional.ofNullable(
                tokenRedisOperator.consumeLoginTicket(loginTicketRequest.loginTicket())
        ).orElseThrow(() -> new BizException(BizCode.AUTH_LOGIN_TICKET_INVALID));

        User u = userMapper.selectById(userId);

        frozenUserCheck(u);
        userMapper.loginRecord(userId);

        var localJwtPayload = LocalJwtPayload.builder()
                .userId(u.getId())
                .role(u.getRole().getValue())
                .tokenVersion(u.getTokenVersion())
                .build();
        String accessToken = jwtTokenUtils.generateAccessToken(localJwtPayload);

        String refreshToken = jwtTokenUtils.generateRefreshToken();
        if (
                !tokenRedisOperator.storeRefreshToken(u.getId(), refreshToken)
        ) throw new BizException(BizCode.INTERNAL_SERVER_ERROR, "登录失败，请稍后再试");

        return new AuthResponse(
                accessToken, refreshToken
        );
    }

    public AuthResponse refresh(RefreshTokenRequest refreshTokenRequest) {

        String oldRefreshToken = refreshTokenRequest.refreshToken();
        User u = Optional.ofNullable(
                tokenRedisOperator.getRefreshTokenOwnerId(oldRefreshToken)
        )
                .map(userMapper::selectById)
                .orElseThrow(() -> new BizException(BizCode.AUTH_REFRESH_TOKEN_INVALID));

        frozenUserCheck(u);

        String newRefreshToken = jwtTokenUtils.generateRefreshToken();
        if (!tokenRedisOperator.rotateRefreshToken(
                u.getId(), oldRefreshToken, newRefreshToken
        )) throw new BizException(BizCode.AUTH_TOKEN_ROTATE_FAIL);

        var localJwtPayload = LocalJwtPayload.builder()
                .userId(u.getId())
                .role(u.getRole().getValue())
                .tokenVersion(u.getTokenVersion())
                .build();
        String accessToken = jwtTokenUtils.generateAccessToken(localJwtPayload);

        return new AuthResponse(accessToken, newRefreshToken);
    }

    public void logout(RefreshTokenRequest refreshTokenRequest) {

        String refreshToken = refreshTokenRequest.refreshToken();
        Long userId = CurrentUser.id();

        if (tokenRedisOperator.deleteRefreshToken(userId, refreshToken)) {
            userMapper.increaseTokenVersion(userId);
            tokenRedisOperator.deleteTokenVersionCache(userId);
        }
    }
}
