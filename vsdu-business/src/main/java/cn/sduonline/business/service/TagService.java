package cn.sduonline.business.service;

import cn.sduonline.business.data.po.Tag;
import cn.sduonline.business.data.vo.TagVO;
import cn.sduonline.business.mapper.TagMapper;
import cn.sduonline.common.exception.BizCode;
import cn.sduonline.common.exception.BizException;
import cn.sduonline.common.result.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TagService {
    private final TagMapper tagMapper;

    public PageResult<TagVO> list(String rawKeyword, String rawSort, long page, long size) {
        String keyword = rawKeyword == null || rawKeyword.isBlank() ? null : rawKeyword.strip();
        String sort = rawSort == null || rawSort.isBlank()
                ? "name"
                : rawSort.strip().toLowerCase(Locale.ROOT);
        if (!List.of("name", "popular").contains(sort)) {
            throw new BizException(BizCode.BAD_REQUEST, "标签排序方式不正确");
        }

        long safePage = Math.clamp(page, 1, 10000);
        long safeSize = Math.clamp(size, 1, 100);
        long total = tagMapper.countPublicTags(keyword);
        long offset = (safePage - 1) * safeSize;
        List<TagVO> items = total == 0 || offset >= total
                ? List.of()
                : tagMapper.selectPublicTagPage(keyword, sort, offset, safeSize).stream()
                .map(this::toVO)
                .toList();
        return new PageResult<>(total, safePage, safeSize, items);
    }

    public List<TagVO> lookup(List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty() || tagIds.size() > TagRelationService.MAX_TAGS) {
            throw new BizException(BizCode.BAD_REQUEST, "一次必须查询1至20个标签");
        }
        if (tagIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new BizException(BizCode.BAD_REQUEST, "标签ID必须为正数");
        }
        LinkedHashSet<Long> uniqueIds = new LinkedHashSet<>(tagIds);
        if (uniqueIds.size() != tagIds.size()) {
            throw new BizException(BizCode.BAD_REQUEST, "标签ID不能重复");
        }
        Map<Long, Tag> tagsById = tagMapper.selectByIds(uniqueIds).stream()
                .collect(Collectors.toMap(Tag::getId, Function.identity()));
        return uniqueIds.stream().map(id -> {
            Tag tag = tagsById.get(id);
            if (tag == null) throw new BizException(BizCode.TAG_NOT_FOUND);
            return toVO(tag);
        }).toList();
    }

    private TagVO toVO(Tag tag) {
        return new TagVO(tag.getId(), tag.getName());
    }
}
