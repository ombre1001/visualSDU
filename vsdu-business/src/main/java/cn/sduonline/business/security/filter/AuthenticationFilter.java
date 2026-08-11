package cn.sduonline.business.security.filter;

import cn.sduonline.business.data.enums.UserRole;
import cn.sduonline.business.mapper.UserMapper;
import cn.sduonline.business.security.context.CurrentUser;
import cn.sduonline.common.exception.BizCode;
import cn.sduonline.common.exception.BizException;
import cn.sduonline.infrastructure.jwt.JwtTokenUtils;
import cn.sduonline.infrastructure.jwt.LocalJwtPayload;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

@Component
public class AuthenticationFilter extends OncePerRequestFilter {

    private static final String ACCESS_TOKEN_HEADER = "token";

    private final JwtTokenUtils jwtTokenUtils;
    private final UserMapper userMapper;
    private final HandlerExceptionResolver exceptionResolver;

    public AuthenticationFilter(
            JwtTokenUtils jwtTokenUtils,
            UserMapper userMapper,
            @Qualifier("handlerExceptionResolver")
            HandlerExceptionResolver exceptionResolver
    ) {
        this.jwtTokenUtils = jwtTokenUtils;
        this.userMapper = userMapper;
        this.exceptionResolver = exceptionResolver;
    }

    private Integer loadTokenVersion(Long userId) {
        return Optional.ofNullable(userMapper.selectTokenVersionById(userId))
                .orElseThrow(() -> new BizException(BizCode.AUTH_USER_NOT_FOUND));
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        try {

            String accessToken = request.getHeader(ACCESS_TOKEN_HEADER);
            if (accessToken == null || accessToken.isBlank()) {
                filterChain.doFilter(request, response);
                return;
            }

            LocalJwtPayload payload;
            try {
                 payload = jwtTokenUtils.parseAccessToken(accessToken);
            } catch (ExpiredJwtException eje) {
                throw new BizException(BizCode.AUTH_ACCESS_TOKEN_EXPIRED);
            } catch (JwtException je) {
                throw new BizException(BizCode.AUTH_ACCESS_TOKEN_INVALID);
            }

            if (!Objects.equals(
                    payload.tokenVersion(),
                    loadTokenVersion(payload.userId())
            )) throw new BizException(BizCode.AUTH_ACCESS_TOKEN_INVALID);

            CurrentUser.setDetail(
                    payload.userId(),
                    UserRole.valueOf(payload.role())
            );

            filterChain.doFilter(request, response);

        } catch (BizException be) {
            if (exceptionResolver.resolveException(
                    request, response, null, be
            ) == null) throw be;
        } finally {
            CurrentUser.removeDetail();
        }
    }


}
