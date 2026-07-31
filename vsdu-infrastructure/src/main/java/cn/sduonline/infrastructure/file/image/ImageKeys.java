package cn.sduonline.infrastructure.file.image;

import java.time.LocalDate;

public class ImageKeys {

    /**
     * Object Keys 标准分隔符
     */
    private static final String SEP = "/";

    /**
     * 点
     */
    private static final String DOT = ".";

    /**
     * 生成Object key，格式：{scene.bizName}/{userId}/{yyyyMMdd}/{uuid}.{ext}
     * @param userId 用户 id
     * @param imageType 文件类型
     */
    public static String build(Long userId, LegalImageType imageType, ImageUploadScene scene) {

        LocalDate d = LocalDate.now();
        String datePath = String.format("%s%04d%02d%02d%s",
                SEP, d.getYear(), d.getMonthValue(), d.getDayOfMonth(), SEP);

        return scene.sceneName() + SEP + userId + datePath + java.util.UUID.randomUUID() + DOT + imageType.getExtension();
    }

}
