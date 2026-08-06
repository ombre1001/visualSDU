package cn.sduonline.business.security.context;

import cn.sduonline.business.data.enums.UserRole;
import cn.sduonline.common.exception.BizCode;
import cn.sduonline.common.exception.BizException;

public final class CurrentUser {

    private static final ThreadLocal<CurrentUserDetails> currentUser = new ThreadLocal<>();

    private CurrentUser() {
    }

    public static void setDetail(Long userId, UserRole userRole) {
        currentUser.set(
                new CurrentUserDetails(userId, userRole)
        );
    }

    public static boolean isLogin() {
        CurrentUserDetails details = currentUser.get();
        return details != null && details.userId != null;
    }

    private static CurrentUserDetails currentUser() {
        if (isLogin()) return currentUser.get();
        throw new BizException(BizCode.UNAUTHORIZED);
    }

    public static Long id() {
        return currentUser().userId;
    }

    public static UserRole role() {
        return currentUser().role;
    }

    public static boolean isAdmin() {
        return currentUser().role == UserRole.ADMIN;
    }

    public static void removeDetail() {
        currentUser.remove();
    }

    record CurrentUserDetails(
            Long userId,
            UserRole role
    ) {}
}
