package cn.sduonline.common.result;

import java.util.List;

public record PageResult<T>(
        long total,
        long page,
        long size,
        List<T> items
) {
}
