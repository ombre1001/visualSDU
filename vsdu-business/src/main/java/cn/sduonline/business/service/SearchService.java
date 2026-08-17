package cn.sduonline.business.service;

import cn.sduonline.business.data.dto.SearchMediaQueryDTO;
import cn.sduonline.business.data.po.Campus;
import cn.sduonline.business.data.po.Location;
import cn.sduonline.business.data.po.Media;
import cn.sduonline.business.data.vo.MediaSummaryVO;
import cn.sduonline.business.data.vo.SearchSuggestionVO;
import cn.sduonline.business.mapper.CampusMapper;
import cn.sduonline.business.mapper.LocationMapper;
import cn.sduonline.business.mapper.MediaMapper;
import cn.sduonline.business.mapper.MediaSearchMapper;
import cn.sduonline.business.util.TagCodec;
import cn.sduonline.common.exception.BizCode;
import cn.sduonline.common.exception.BizException;
import cn.sduonline.common.result.PageResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SearchService {

    private static final int ENABLED = 1;

    private final MediaSearchMapper mediaSearchMapper;
    private final MediaMapper mediaMapper;
    private final LocationMapper locationMapper;
    private final CampusMapper campusMapper;
    private final MediaService mediaService;

    public List<SearchSuggestionVO> suggestions(
            String rawKeyword,
            int limit
    ) {
        String keyword = normalizeKeyword(rawKeyword);
        boolean hasKeyword = StringUtils.hasText(keyword);

        int safeLimit = Math.clamp(limit, 1, 20);
        int candidateLimit = Math.min(safeLimit * 4, 80);

        LinkedHashMap<String, SearchSuggestionVO> suggestions =
                new LinkedHashMap<>();

        List<Location> locations = locationMapper.selectEnabledSuggestions(
                keyword,
                candidateLimit
        );

        Map<Long, Campus> locationCampuses = locations.stream()
                .map(Location::getCampusId)
                .distinct()
                .map(campusMapper::selectById)
                .filter(campus -> campus != null
                        && Integer.valueOf(ENABLED).equals(campus.getStatus()))
                .collect(
                        java.util.stream.Collectors.toMap(
                                Campus::getId,
                                campus -> campus,
                                (left, _) -> left
                        )
                );

        for (Location location : locations) {
            Campus campus = locationCampuses.get(location.getCampusId());

            if (campus == null) {
                continue;
            }

            addSuggestion(
                    suggestions,
                    new SearchSuggestionVO(
                            "LOCATION",
                            location.getId(),
                            location.getName(),
                            campus.getName()
                    )
            );
        }

        List<Campus> campuses = campusMapper.selectEnabledCampuses(
                keyword,
                null,
                candidateLimit
        );

        for (Campus campus : campuses) {
            addSuggestion(
                    suggestions,
                    new SearchSuggestionVO(
                            "CAMPUS",
                            campus.getId(),
                            campus.getName(),
                            campus.getAddress()
                    )
            );
        }

        List<Media> media = mediaMapper.selectList(
                new LambdaQueryWrapper<Media>()
                        .eq(Media::getStatus, ENABLED)
                        .and(
                                hasKeyword,
                                wrapper -> wrapper
                                        .like(Media::getTitle, keyword)
                                        .or()
                                        .like(Media::getDescription, keyword)
                                        .or()
                                        .like(Media::getTags, keyword)
                        )
                        .orderByDesc(Media::getFavoriteCount)
                        .orderByDesc(Media::getLikeCount)
                        .orderByDesc(Media::getViewCount)
                        .orderByDesc(Media::getId)
                        .last("LIMIT " + candidateLimit)
        );

        Map<Long, Location> mediaLocations = media.stream()
                .map(Media::getLocationId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .map(locationMapper::selectById)
                .filter(location -> location != null
                        && Integer.valueOf(ENABLED).equals(location.getStatus()))
                .collect(
                        java.util.stream.Collectors.toMap(
                                Location::getId,
                                location -> location,
                                (left, _) -> left
                        )
                );

        // 从当前media.tags字段解析标签建议。
        for (Media item : media) {
            for (String tag : TagCodec.decode(item.getTags())) {
                if (!hasKeyword
                        || tag.toLowerCase(Locale.ROOT)
                        .contains(keyword.toLowerCase(Locale.ROOT))) {
                    addSuggestion(
                            suggestions,
                            new SearchSuggestionVO(
                                    "TAG",
                                    null,
                                    tag,
                                    "标签"
                            )
                    );
                }
            }
        }

        for (Media item : media) {
            if (!StringUtils.hasText(item.getTitle())) {
                continue;
            }

            Location location = mediaLocations.get(item.getLocationId());

            addSuggestion(
                    suggestions,
                    new SearchSuggestionVO(
                            "MEDIA",
                            item.getId(),
                            item.getTitle(),
                            location == null ? null : location.getName()
                    )
            );
        }

        return suggestions.values()
                .stream()
                .limit(safeLimit)
                .toList();
    }

    public PageResult<MediaSummaryVO> searchMedia(
            SearchMediaQueryDTO query
    ) {
        normalizeQuery(query);

        long offset = (query.getPage() - 1) * query.getSize();

        long total = mediaSearchMapper.countSearchMedia(query);

        if (total == 0) {
            return new PageResult<>(
                    0,
                    query.getPage(),
                    query.getSize(),
                    List.of()
            );
        }

        List<MediaSummaryVO> items =
                mediaSearchMapper.searchMedia(
                                query,
                                query.getSort(),
                                offset,
                                query.getSize()
                        )
                        .stream()
                        .map(mediaService::toSummary)
                        .toList();

        return new PageResult<>(
                total,
                query.getPage(),
                query.getSize(),
                items
        );
    }

    private void normalizeQuery(SearchMediaQueryDTO query) {
        query.setQ(normalizeKeyword(query.getQ()));
        query.setTag(normalizeText(query.getTag()));

        String sort = normalizeText(query.getSort());

        if (sort == null) {
            sort = "relevance";
        } else {
            sort = sort.toLowerCase(Locale.ROOT);
        }

        if (!List.of(
                "relevance",
                "newest",
                "oldest",
                "hot"
        ).contains(sort)) {
            throw new BizException(BizCode.SEARCH_SORT_INVALID);
        }

        query.setSort(sort);
        query.setPage(Math.max(query.getPage(), 1));
        query.setSize(Math.clamp(query.getSize(), 1, 50));
    }

    private String normalizeKeyword(String value) {
        String keyword = normalizeText(value);

        if (keyword != null && keyword.length() > 50) {
            throw new BizException(BizCode.SEARCH_KEYWORD_TOO_LONG);
        }

        return keyword;
    }

    private String normalizeText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        return value.trim();
    }

    private void addSuggestion(
            Map<String, SearchSuggestionVO> suggestions,
            SearchSuggestionVO suggestion
    ) {
        if (!StringUtils.hasText(suggestion.text())) {
            return;
        }

        String key = suggestion.type()
                + ":"
                + suggestion.text()
                .trim()
                .toLowerCase(Locale.ROOT);

        suggestions.putIfAbsent(key, suggestion);
    }
}
