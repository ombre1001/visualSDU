package cn.sduonline.infrastructure.file.image;

import cn.sduonline.infrastructure.file.exception.BadFileException;
import cn.sduonline.infrastructure.file.exception.FileStorageException;
import cn.sduonline.infrastructure.file.model.UploadFile;
import cn.sduonline.infrastructure.file.storage.FileStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

import static cn.sduonline.infrastructure.file.exception.BadFileException.BadFileErrorCode.*;

@RequiredArgsConstructor
public class ImageFileUpload {

    private final FileStorage fileStorage;

    public LegalImageType validate(MultipartFile file, ImageUploadScene scene) {
        validateNotEmpty(file);
        validateSize(file, scene);
        LegalImageType declaredType = validateContentType(file);

        try (InputStream inputStream = file.getInputStream()) {
            LegalImageType actualType = LegalImageType.detectImageType(inputStream)
                    .orElseThrow(() -> new BadFileException(FILE_TYPE_NOT_SUPPORT));

            if (declaredType != actualType) {
                throw new BadFileException(FILE_TYPE_NOT_SUPPORT);
            }

            return actualType;
        } catch (IOException e) {
            throw new FileStorageException();
        }
    }

    private void validateNotEmpty(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadFileException(FILE_EMPTY);
        }
    }

    private void validateSize(MultipartFile file, ImageUploadScene scene) {
        if (scene.maxImageSize().toBytes() < file.getSize()) {
            throw new BadFileException(FILE_TOO_LARGE);
        }
    }

    private LegalImageType validateContentType(MultipartFile file) {
        return LegalImageType.fromContentType(file.getContentType())
                .orElseThrow(() -> new BadFileException(FILE_TYPE_NOT_SUPPORT));
    }

    public String uploadImageFile(ImageUploadScene scene, Long userId, MultipartFile file) {

        LegalImageType imageType = validate(file, scene);
        String objectKey = ImageKeys.build(userId, imageType, scene);

        try (InputStream inputStream = file.getInputStream()) {

            UploadFile uploadFile = UploadFile.builder()
                    .objectKey(objectKey)
                    .contentType(imageType.getContentType())
                    .size(file.getSize())
                    .inputStream(inputStream)
                    .build();

            fileStorage.storage(uploadFile);

        } catch (IOException ioe) {
            throw new FileStorageException();
        }

        return objectKey;
    }

}
