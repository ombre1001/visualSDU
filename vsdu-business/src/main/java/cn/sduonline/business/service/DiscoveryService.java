package cn.sduonline.business.service;

import cn.sduonline.business.data.dto.SearchMediaQueryDTO;
import cn.sduonline.business.data.po.Campus;
import cn.sduonline.business.data.po.City;
import cn.sduonline.business.data.projection.MediaSummaryRow;
import cn.sduonline.business.data.vo.DiscoveryCampusSectionVO;
import cn.sduonline.business.data.vo.DiscoveryHomeVO;
import cn.sduonline.business.data.vo.PopularTagVO;
import cn.sduonline.business.mapper.CampusMapper;
import cn.sduonline.business.mapper.CityMapper;
import cn.sduonline.business.mapper.MediaSearchMapper;
import cn.sduonline.business.util.TagCodec;
import cn.sduonline.common.exception.BizCode;
import cn.sduonline.common.exception.BizException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DiscoveryService {

    private static final int ENABLED = 1;

    private static final int FEATURED_SIZE = 12;
    private static final int LATEST_SIZE = 12;
    private static final int CAMPUS_SECTION_SIZE = 6;
    private static final int CAMPUS_MEDIA_SIZE = 6;
    private static final int POPULAR_TAG_SIZE = 12;
    private static final int TAG_STATISTIC_MEDIA_SIZE = 200;

    private final CityMapper cityMapper;
    private final CampusMapper campusMapper;
    private final MediaSearchMapper mediaSearchMapper;
    private final MediaService mediaService;
    private final TopicService topicService;

    public DiscoveryHomeVO home(Long cityId) {
        if (cityId != null) {
            requireEnabledCity(cityId);
        }

        List<MediaSummaryRow> hotMedia = selectMedia(
                cityId,
                null,
                "hot",
                FEATURED_SIZE
        );

        List<MediaSummaryRow> latestMedia = selectMedia(
                cityId,
                null,
                "newest",
                LATEST_SIZE
        );

        List<Campus> campuses = campusMapper.selectEnabledCampuses(
                null,
                cityId,
                CAMPUS_SECTION_SIZE
        );

        List<DiscoveryCampusSectionVO> campusSections =
                campuses.stream()
                        .map(campus -> new DiscoveryCampusSectionVO(
                                campus.getId(),
                                campus.getName(),
                                campus.getCoverUrl(),
                                selectMedia(
                                        cityId,
                                        campus.getId(),
                                        "hot",
                                        CAMPUS_MEDIA_SIZE
                                )
                                        .stream()
                                        .map(mediaService::toSummary)
                                        .toList()
                        ))
                        .toList();

        List<MediaSummaryRow> tagStatisticMedia = selectMedia(
                cityId,
                null,
                "hot",
                TAG_STATISTIC_MEDIA_SIZE
        );

        return new DiscoveryHomeVO(
                cityId,
                hotMedia.stream()
                        .map(mediaService::toSummary)
                        .toList(),
                latestMedia.stream()
                        .map(mediaService::toSummary)
                        .toList(),
                calculatePopularTags(tagStatisticMedia),
                topicService.list(),
                campusSections
        );
    }

    private List<MediaSummaryRow> selectMedia(
            Long cityId,
            Long campusId,
            String sort,
            long size
    ) {
        SearchMediaQueryDTO query = new SearchMediaQueryDTO();
        query.setCityId(cityId);
        query.setCampusId(campusId);
        query.setSort(sort);
        query.setPage(1);
        query.setSize(size);

        return mediaSearchMapper.searchMedia(
                query,
                sort,
                0,
                size
        );
    }

    private List<PopularTagVO> calculatePopularTags(
            List<MediaSummaryRow> media
    ) {
        Map<String, Long> counts = new LinkedHashMap<>();

        for (MediaSummaryRow item : media) {
            for (String tag : TagCodec.decode(item.getTags())) {
                if (tag == null || tag.isBlank()) {
                    continue;
                }

                counts.merge(tag.trim(), 1L, Long::sum);
            }
        }

        return counts.entrySet()
                .stream()
                .sorted(
                        Comparator
                                .<Map.Entry<String, Long>>
                                        comparingLong(Map.Entry::getValue)
                                .reversed()
                                .thenComparing(Map.Entry::getKey)
                )
                .limit(POPULAR_TAG_SIZE)
                .map(entry -> new PopularTagVO(
                        entry.getKey(),
                        entry.getValue()
                ))
                .toList();
    }

    private void requireEnabledCity(Long cityId) {
        City city = cityMapper.selectOne(
                new LambdaQueryWrapper<City>()
                        .eq(City::getId, cityId)
                        .eq(City::getStatus, ENABLED)
        );

        if (city == null) {
            throw new BizException(BizCode.CITY_NOT_FOUND);
        }
    }
}
