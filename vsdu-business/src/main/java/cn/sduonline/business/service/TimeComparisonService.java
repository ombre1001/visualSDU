package cn.sduonline.business.service;

import cn.sduonline.business.data.po.Location;
import cn.sduonline.business.data.po.TimeComparison;
import cn.sduonline.business.data.po.TimeComparisonItem;
import cn.sduonline.business.data.projection.MediaSummaryRow;
import cn.sduonline.business.data.projection.TimeComparisonSummaryRow;
import cn.sduonline.business.data.vo.MediaSummaryVO;
import cn.sduonline.business.data.vo.TimeComparisonDetailVO;
import cn.sduonline.business.data.vo.TimeComparisonSummaryVO;
import cn.sduonline.business.mapper.LocationMapper;
import cn.sduonline.business.mapper.TimeComparisonItemMapper;
import cn.sduonline.business.mapper.TimeComparisonMapper;
import cn.sduonline.common.exception.BizCode;
import cn.sduonline.common.exception.BizException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TimeComparisonService {
    private static final int VISIBLE = 1;

    private final TimeComparisonMapper comparisonMapper;
    private final TimeComparisonItemMapper itemMapper;
    private final LocationMapper locationMapper;
    private final MediaService mediaService;

    public List<TimeComparisonSummaryVO> list(Long locationId, int size) {
        int safeSize = Math.clamp(size, 1, 100);
        List<TimeComparisonSummaryRow> rows = comparisonMapper.selectSummaryRows(locationId, safeSize);
        Map<Long, TimeComparisonSummaryRow> headers = new LinkedHashMap<>();
        Map<Long, List<MediaSummaryVO>> mediaByComparison = new LinkedHashMap<>();
        for (TimeComparisonSummaryRow row : rows) {
            headers.putIfAbsent(row.getComparisonId(), row);
            List<MediaSummaryVO> media = mediaByComparison.computeIfAbsent(
                    row.getComparisonId(), _ -> new java.util.ArrayList<>()
            );
            if (row.getMediaId() != null) media.add(toMediaSummary(row));
        }
        return headers.values().stream()
                .map(row -> new TimeComparisonSummaryVO(
                        row.getComparisonId(), row.getComparisonLocationId(),
                        row.getComparisonLocationName(), row.getComparisonTitle(),
                        row.getComparisonDescription(), mediaByComparison.get(row.getComparisonId())
                ))
                .toList();
    }

    public TimeComparisonDetailVO detail(Long comparisonId, Long optionalUserId) {
        TimeComparison comparison = requireVisible(comparisonId);
        Location location = locationMapper.selectById(comparison.getLocationId());
        List<TimeComparisonDetailVO.Item> items = listItems(comparisonId).stream()
                .map(item -> new TimeComparisonDetailVO.Item(
                        item.getLabel(),
                        mediaService.detail(item.getMediaId(), optionalUserId)
                ))
                .toList();
        return new TimeComparisonDetailVO(
                comparison.getId(), comparison.getLocationId(), location == null ? null : location.getName(),
                comparison.getTitle(), comparison.getDescription(), items
        );
    }

    private MediaSummaryVO toMediaSummary(TimeComparisonSummaryRow source) {
        MediaSummaryRow row = new MediaSummaryRow();
        row.setId(source.getMediaId());
        row.setTitle(source.getMediaTitle());
        row.setLocationId(source.getMediaLocationId());
        row.setLocationName(source.getMediaLocationName());
        row.setThumbnailKey(source.getMediaThumbnailKey());
        row.setShotAt(source.getMediaShotAt());
        row.setViewCount(source.getMediaViewCount());
        row.setLikeCount(source.getMediaLikeCount());
        row.setFavoriteCount(source.getMediaFavoriteCount());
        return mediaService.toSummary(row);
    }

    private TimeComparison requireVisible(Long comparisonId) {
        TimeComparison comparison = comparisonMapper.selectOne(new LambdaQueryWrapper<TimeComparison>()
                .eq(TimeComparison::getId, comparisonId)
                .eq(TimeComparison::getStatus, VISIBLE));
        if (comparison == null) throw new BizException(BizCode.TIME_COMPARISON_NOT_FOUND);
        return comparison;
    }

    private List<TimeComparisonItem> listItems(Long comparisonId) {
        return itemMapper.selectList(new LambdaQueryWrapper<TimeComparisonItem>()
                .eq(TimeComparisonItem::getComparisonId, comparisonId)
                .orderByAsc(TimeComparisonItem::getSortOrder)
                .orderByAsc(TimeComparisonItem::getId));
    }
}
