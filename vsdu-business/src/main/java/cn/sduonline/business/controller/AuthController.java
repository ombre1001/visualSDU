package cn.sduonline.business.controller;

import cn.sduonline.business.data.dto.LoginTicketRequest;
import cn.sduonline.business.data.dto.RefreshTokenRequest;
import cn.sduonline.business.data.dto.RegisterRequest;
import cn.sduonline.business.data.vo.AuthResponse;
import cn.sduonline.business.security.anno.PublicApi;
import cn.sduonline.business.service.AuthService;
import cn.sduonline.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 前端登录页重定向地址
     */
    @Value("${vsdu.auth.frontend-return-url}")
    private String frontendReturnUrl;

    /**
     * Sdu Pass回调接口
     * <p>
     * 学生完成统一认证登录后，获取并解析sdu pass token，生成一个有效期为120秒的一次性登录凭据loginTicket，返回302重定向到前端登录完成页面，并将loginTicket作为请求参数。
     * 首次访问vsdu的学生，将在解析sdu pass token后自动创建账户。
     * <p>
     * 测试环境下浏览器访问<a href="https://i.sdu.edu.cn/pass-api/login/page?forward=http%3A%2F%2Flocalhost%3A8080%2Fauth%2Fsdupass%2Fcallback">...</a>即可
     * @param code sdu pass回调时携带的，用于获取sdu pass token的code凭证
     * @return 返回HTTP302，重定向到前端登录完成页，并将票据作为前端登陆完成也的QueryParam。本接口不直接签发accessToken和refreshToken。
     */
    @PublicApi
    @GetMapping("/sdupass/callback")
    public ResponseEntity<Void> sdupassLogin(@RequestParam String code) {
        String loginTicket = authService.sdupassCallback(code);

        URI location = UriComponentsBuilder
                .fromUriString(frontendReturnUrl)
                .queryParam("loginTicket", loginTicket)
                .build()
                .encode()
                .toUri();

        return ResponseEntity
                .status(HttpStatus.FOUND)
                .location(location)
                .cacheControl(CacheControl.noStore())
                .build();
    }

    /**
     * 一次性票据登录接口
     * 登录票据校验，校验成功签发accessToken和refreshToken
     */
    @PublicApi
    @PostMapping("/login")
    public Result<AuthResponse> ticketExchange(@Validated @RequestBody LoginTicketRequest loginTicketRequest) {
        return Result.success(authService.login(loginTicketRequest));
    }

    /**
     * 刷新令牌
     * 使用 refresh token 轮换签发一对新令牌；前端必须同时替换本地的 access token 和 refresh token。
     */
    @PublicApi
    @PostMapping("/refresh")
    public Result<AuthResponse> refresh(@RequestBody @Validated RefreshTokenRequest refreshTokenRequest) {
        return Result.success(authService.refresh(refreshTokenRequest));
    }

    /**
     * 退出登录
     * 注销属于当前用户的 refresh token 并使已有访问凭证失效；成功后前端应清理本地凭证。
     */
    @DeleteMapping("/logout")
    public Result<Void> logout(@RequestBody @Validated RefreshTokenRequest refreshTokenRequest) {
        authService.logout(refreshTokenRequest);
        return Result.ok();
    }

    /**
     * 账号注册接口 目前不对外开放
     * 只有/auth/sdupass-login可以创建账户
     */
//    @PostMapping("/register")
    public Result<String> register(RegisterRequest registerRequest) {
        return Result.success(authService.register(registerRequest));
    }
}
