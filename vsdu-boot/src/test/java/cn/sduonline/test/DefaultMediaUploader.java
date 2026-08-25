package cn.sduonline.test;

import cn.sduonline.VisualSduApplication;
import cn.sduonline.infrastructure.file.model.UploadFile;
import cn.sduonline.infrastructure.file.storage.FileStorage;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;

@SpringBootTest(classes = VisualSduApplication.class)
@ActiveProfiles("local")
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

    private static final File defaultAvatar = new File("C:\\Users\\Sentimental\\Desktop\\视觉山大！\\default.png");
    private static final String defaultAvatarKey = "avatars/default.png";

    @Test
    public void uploadDefaultMedia() {
        localMediaUpload(defaultAvatar, defaultAvatarKey, "image/png");
    }
}
