package cn.sduonline.business.data.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class UpdateSubmissionRequest {
    @Positive(message = "拍摄地点ID必须为正数")
    private Long locationId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime shotAt;

    @Size(max = 20, message = "标签最多20个")
    private List<@Size(max = 32, message = "单个标签不能超过32个字符") String> tags;

    @Size(max = 2000, message = "稿件描述不能超过2000个字符")
    private String description;

    @Size(max = 9, message = "单次投稿最多上传9张图片")
    private List<MultipartFile> files = new ArrayList<>();

    /** 有新文件时是否替换全部旧文件；默认追加。 */
    private Boolean replaceFiles = false;
}
