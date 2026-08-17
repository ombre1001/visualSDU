package cn.sduonline.business.data.enums;

import cn.sduonline.infrastructure.file.image.ImageUploadScene;
import lombok.RequiredArgsConstructor;
import org.springframework.util.unit.DataSize;

@RequiredArgsConstructor
public enum ImageScene implements ImageUploadScene {

    SUBMISSION("submission", DataSize.ofMegabytes(20)),
    AVATAR("avatars", DataSize.ofMegabytes(5)),

    ;

    private final String sceneName;
    private final DataSize maxImageSize;

    @Override
    public String sceneName() {
        return sceneName;
    }

    @Override
    public DataSize maxImageSize() {
        return maxImageSize;
    }
}
