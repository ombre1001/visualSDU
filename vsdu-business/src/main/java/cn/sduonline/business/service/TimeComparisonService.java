package cn.sduonline.business.service;

import cn.sduonline.business.data.po.Location;
import cn.sduonline.business.data.po.TimeComparison;
import cn.sduonline.business.data.po.TimeComparisonItem;
import cn.sduonline.business.data.vo.MediaSummaryVO;
import cn.sduonline.business.data.vo.TimeComparisonDetailVO;
import cn.sduonline.business.data.vo.TimeComparisonSummaryVO;
import cn.sduonline.business.mapper.LocationMapper;
import cn.sduonline.business.mapper.MediaMapper;
import cn.sduonline.business.mapper.TimeComparisonItemMapper;
import cn.sduonline.business.mapper.TimeComparisonMapper;
import cn.sduonline.common.exception.BizCode;
import cn.sduonline.common.exception.BizException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TimeComparisonService {
    private static final int VISIBLE = 1;

    private final TimeComparisonMapper comparisonMapper;
    private final TimeComparisonItemMapper itemMapper;
    private final MediaMapper mediaMapper;
    private final LocationMapper locationMapper;
    private final MediaService mediaService;

    public List<TimeComparisonSummaryVO> list(Long locationId, int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        return comparisonMapper.selectList(new LambdaQueryWrapper<TimeComparison>()
                        .eq(TimeComparison::getStatus, VISIBLE)
                        .eq(locationId != null, TimeComparison::getLocationId, locationId)
                        .orderByDesc(TimeComparison::getUpdatedAt)
                        .last("LIMIT " + safeSize))
                .stream()
                .map(this::toSummary)
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

    private TimeComparisonSummaryVO toSummary(TimeComparison comparison) {
        Location location = locationMapper.selectById(comparison.getLocationId());
        List<MediaSummaryVO> media = listItems(comparison.getId()).stream()
                .map(TimeComparisonItem::getMediaId)
                .map(mediaMapper::selectById)
                .filter(item -> item != null && item.getStatus() == VISIBLE)
                .map(mediaService::toSummary)
                .toList();
        return new TimeComparisonSummaryVO(
                comparison.getId(), comparison.getLocationId(), location == null ? null : location.getName(),
                comparison.getTitle(), comparison.getDescription(), media
        );
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
