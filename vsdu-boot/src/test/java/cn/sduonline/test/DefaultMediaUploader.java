package cn.sduonline.test;

import cn.sduonline.infrastructure.file.model.UploadFile;
import cn.sduonline.infrastructure.file.storage.FileStorage;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

@SpringBootTest
public class DefaultMediaUploader {

    @Resource
    private FileStorage fileStorage;

    public void localMediaUpload(File file, String objectKey, String contentType) {
        try (
                BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))
        ) {

            UploadFile uploadFile = UploadFile.builder()
                    .objectKey(objectKey)
                    .contentType(contentType)
                    .size(file.length())
                    .inputStream(bis)
                    .build();
            fileStorage.storage(uploadFile);


        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        System.out.println(fileStorage.getUrl(objectKey));
    }

    private static final File defaultAvatar = new File("D:\\resources\\dssm\\default.png");
    private static final String defaultAvatarKey = "avatars/default.png";

    @Test
    public void uploadDefaultMedia() {
        localMediaUpload(defaultAvatar, defaultAvatarKey, "image/png");
    }
}
