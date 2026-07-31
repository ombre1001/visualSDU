package cn.sduonline.infrastructure.file.image;

import org.springframework.util.unit.DataSize;

public interface ImageUploadScene {
    String sceneName();
    DataSize maxImageSize();
}
