package cn.sduonline.business.data.enums;

import cn.sduonline.infrastructure.file.image.ImageUploadScene;
import org.springframework.util.unit.DataSize;

public enum SubmissionImageScene implements ImageUploadScene {
    INSTANCE;

    @Override
    public String sceneName() {
        return "submissions";
    }

    @Override
    public DataSize maxImageSize() {
        return DataSize.ofMegabytes(20);
    }
}
