package cn.sduonline.infrastructure.file.image;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Optional;

@Getter
@RequiredArgsConstructor
public enum LegalImageType {

    PNG("png", "image/png") {
        @Override
        protected boolean matchesMagicNumber(byte[] header) {
            return header.length >= 8
                    && unsigned(header[0]) == 0x89
                    && header[1] == 0x50
                    && header[2] == 0x4E
                    && header[3] == 0x47
                    && header[4] == 0x0D
                    && header[5] == 0x0A
                    && header[6] == 0x1A
                    && header[7] == 0x0A;
        }
    },
    JPEG("jpg", "image/jpeg") {
        @Override
        protected boolean matchesMagicNumber(byte[] header) {
            return header.length >= 3
                    && unsigned(header[0]) == 0xFF
                    && unsigned(header[1]) == 0xD8
                    && unsigned(header[2]) == 0xFF;
        }
    },
    WEBP("webp", "image/webp") {
        @Override
        protected boolean matchesMagicNumber(byte[] header) {
            return header.length >= 12
                    && header[0] == 'R'
                    && header[1] == 'I'
                    && header[2] == 'F'
                    && header[3] == 'F'
                    && header[8] == 'W'
                    && header[9] == 'E'
                    && header[10] == 'B'
                    && header[11] == 'P';
        }
    };

    private static int unsigned(byte b) {
        return b & 0xFF;
    }

    private final String extension;
    private final String contentType;

    protected abstract boolean matchesMagicNumber(byte[] header);

    public static Optional<LegalImageType> fromContentType(String contentType) {
        return Arrays.stream(values())
                .filter(type -> type.contentType.equalsIgnoreCase(contentType))
                .findAny();
    }

    private static final int MAGIC_HEADER_SIZE = 12;

    public static Optional<LegalImageType> detectImageType(InputStream inputStream) throws IOException {
        if (inputStream == null) return Optional.empty();

        byte[] header = inputStream.readNBytes(MAGIC_HEADER_SIZE);
        return Arrays.stream(values())
                .filter(type -> type.matchesMagicNumber(header))
                .findAny();
    }

}
