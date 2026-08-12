package cn.sduonline.business.controller;

import cn.sduonline.business.data.dto.RefreshTokenRequest;
import cn.sduonline.business.data.dto.RegisterRequest;
import cn.sduonline.business.data.vo.AuthResponse;
import cn.sduonline.business.security.anno.PublicApi;
import cn.sduonline.business.service.AuthService;
import cn.sduonline.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Sdu Pass回调接口
     * <p>
     * 学生完成统一认证登录后，获取并解析sdu pass token，签发access token和refresh token并响应。
     * 首次访问vsdu的学生，将再解析sdu pass token后自动创建账户。
     * <p>
     * 测试环境下浏览器访问<a href="https://i.sdu.edu.cn/pass-api/login/page?forward=http%3A%2F%2Flocalhost%3A8080%2Fauth%2Fsdupass-login">...</a>即可
     * @param code sdu pass回调时携带的，用于获取sdu pass token的code凭证
     * @return 包含access token和refresh token
     */
    @PublicApi
    @GetMapping("/sdupass-login")
    public Result<AuthResponse> sdupassLogin(@RequestParam String code) {
        return Result.success(authService.sdupassLogin(code));
    }

    @PublicApi
    @PostMapping("/refresh")
    public Result<AuthResponse> refresh(@RequestBody @Validated RefreshTokenRequest refreshTokenRequest) {
        return Result.success(authService.refresh(refreshTokenRequest));
    }

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
    public Result<AuthResponse> register(RegisterRequest registerRequest) {
        return Result.success(authService.register(registerRequest));
    }
}
