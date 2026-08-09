package cn.sduonline.business.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class TagCodec {
    private static final String SEPARATOR = "|";

    private TagCodec() {
    }

    public static String encode(List<String> tags) {
        if (tags == null || tags.isEmpty()) return null;
        return tags.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .map(tag -> tag.replace(SEPARATOR, ""))
                .distinct()
                .limit(20)
                .reduce((left, right) -> left + SEPARATOR + right)
                .orElse(null);
    }

    public static List<String> decode(String tags) {
        if (tags == null || tags.isBlank()) return Collections.emptyList();
        return Arrays.stream(tags.split("\\|"))
                .filter(tag -> !tag.isBlank())
                .toList();
    }
}
