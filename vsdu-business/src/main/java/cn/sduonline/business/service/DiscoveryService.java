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
import cn.sduonline.business.mapper.MediaTagMapper;
import cn.sduonline.common.exception.BizCode;
import cn.sduonline.common.exception.BizException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DiscoveryService {

    private static final int ENABLED = 1;

    private static final int FEATURED_SIZE = 12;
    private static final int LATEST_SIZE = 12;
    private static final int CAMPUS_SECTION_SIZE = 6;
    private static final int CAMPUS_MEDIA_SIZE = 6;
    private static final int POPULAR_TAG_SIZE = 12;

    private final CityMapper cityMapper;
    private final CampusMapper campusMapper;
    private final MediaSearchMapper mediaSearchMapper;
    private final MediaService mediaService;
    private final TopicService topicService;
    private final MediaTagMapper mediaTagMapper;

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
                                mediaService.toSummaries(selectMedia(
                                        cityId,
                                        campus.getId(),
                                        "hot",
                                        CAMPUS_MEDIA_SIZE
                                ))
                        ))
                        .toList();

        return new DiscoveryHomeVO(
                cityId,
                mediaService.toSummaries(hotMedia),
                mediaService.toSummaries(latestMedia),
                selectPopularTags(cityId),
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

    private List<PopularTagVO> selectPopularTags(Long cityId) {
        return mediaTagMapper.selectPopularTags(cityId, POPULAR_TAG_SIZE)
                .stream()
                .map(row -> new PopularTagVO(row.getId(), row.getName(), row.getMediaCount()))
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
