package cn.sduonline.business.controller;

import cn.sduonline.business.data.dto.RegisterRequest;
import cn.sduonline.business.data.vo.AuthResponse;
import cn.sduonline.business.service.AuthService;
import cn.sduonline.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @GetMapping("/sdupass-login")
    public Result<AuthResponse> sdupassLogin(@RequestParam String code) {
        return Result.success(authService.sdupassLogin(code));
    }

//    @PostMapping("/register")
    public Result<AuthResponse> register(RegisterRequest registerRequest) {
        return Result.success(authService.register(registerRequest));
    }
}
