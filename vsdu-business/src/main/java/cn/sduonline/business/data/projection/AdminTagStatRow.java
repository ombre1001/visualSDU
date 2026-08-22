package cn.sduonline.business.data.projection;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminTagStatRow {
    private Long id;
    private String name;
    private Long mediaCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
