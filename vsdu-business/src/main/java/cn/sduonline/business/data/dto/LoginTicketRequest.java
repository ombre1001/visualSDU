package cn.sduonline.business.data.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginTicketRequest(
        @NotBlank(message = "临时登录凭据不能为空")
        String loginTicket
) {
}
