package cn.sduonline.infrastructure.jwt.sdupass;

public record StudentInfo(
        String userId,
        String username,
        String studentNumber,
        String email,
        Integer status,
        String token
) {
}
