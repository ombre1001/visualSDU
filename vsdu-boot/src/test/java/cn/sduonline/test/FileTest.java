package cn.sduonline.test;

import cn.sduonline.VisualSduApplication;
import cn.sduonline.infrastructure.file.image.ImageFileUpload;
import cn.sduonline.infrastructure.file.model.UploadFile;
import cn.sduonline.infrastructure.file.storage.FileStorage;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;

@SpringBootTest(classes = VisualSduApplication.class)
public class FileTest {

    File testSrc = new File("D:\\resources\\default.jpg");
    String testObjectKey = "demo/demo.png";

    @Resource
    private FileStorage fileStorage;

    @Test
    void uploadTest() {
        try (
                BufferedInputStream bis = new BufferedInputStream(new FileInputStream(testSrc))
        ) {

            UploadFile up = UploadFile.builder()
                    .objectKey(testObjectKey)
                    .contentType("image/png")
                    .size(testSrc.length())
                    .inputStream(bis)
                    .build();

            fileStorage.storage(up);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void deleteTest() {
        fileStorage.delete(testObjectKey);
    }

    @Test
    void getUrlTest() {
        String url = fileStorage.getUrl(testObjectKey);
        System.out.println(url);
    }

    @Resource
    private ImageFileUpload imageFileUpload;

}

