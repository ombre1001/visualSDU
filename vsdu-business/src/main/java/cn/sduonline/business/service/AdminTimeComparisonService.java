package cn.sduonline.business.service;

import cn.sduonline.business.data.dto.AdminCreateTimeComparisonRequest;
import cn.sduonline.business.data.dto.AdminTimeComparisonItemRequest;
import cn.sduonline.business.data.po.Location;
import cn.sduonline.business.data.po.Media;
import cn.sduonline.business.data.po.TimeComparison;
import cn.sduonline.business.data.po.TimeComparisonItem;
import cn.sduonline.business.data.vo.AdminTimeComparisonItemVO;
import cn.sduonline.business.data.vo.AdminTimeComparisonVO;
import cn.sduonline.business.mapper.LocationMapper;
import cn.sduonline.business.mapper.MediaMapper;
import cn.sduonline.business.mapper.TimeComparisonItemMapper;
import cn.sduonline.business.mapper.TimeComparisonMapper;
import cn.sduonline.common.exception.BizCode;
import cn.sduonline.common.exception.BizException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AdminTimeComparisonService {
    private static final int ENABLED = 1;
    private final TimeComparisonMapper comparisonMapper;
    private final TimeComparisonItemMapper itemMapper;
    private final LocationMapper locationMapper;
    private final MediaMapper mediaMapper;

    @Transactional
    public AdminTimeComparisonVO create(AdminCreateTimeComparisonRequest r) {
        Location location = locationMapper.selectOne(new LambdaQueryWrapper<Location>()
                .eq(Location::getId, r.locationId()).eq(Location::getStatus, ENABLED));
        if (location == null) throw new BizException(BizCode.ADMIN_TIME_COMPARISON_LOCATION_INVALID);
        List<Long> ids = r.items().stream().map(AdminTimeComparisonItemRequest::mediaId).toList();
        if (new HashSet<>(ids).size() != ids.size()) throw new BizException(BizCode.BAD_REQUEST, "对比媒体不能重复");
        for (Long id : ids) {
            Media media = mediaMapper.selectById(id);
            if (media == null || !Objects.equals(media.getStatus(), ENABLED)
                    || !Objects.equals(media.getLocationId(), r.locationId())) {
                throw new BizException(BizCode.ADMIN_TIME_COMPARISON_MEDIA_INVALID);
            }
        }

        LocalDateTime now = LocalDateTime.now();
        TimeComparison comparison = new TimeComparison();
        comparison.setLocationId(r.locationId()); comparison.setTitle(r.title().strip());
        comparison.setDescription(nullable(r.description()));
        comparison.setStatus(Objects.requireNonNullElse(r.status(), ENABLED));
        comparison.setCreatedAt(now); comparison.setUpdatedAt(now);
        comparisonMapper.insert(comparison);

        List<AdminTimeComparisonItemVO> items = new ArrayList<>();
        for (int i = 0; i < r.items().size(); i++) {
            AdminTimeComparisonItemRequest source = r.items().get(i);
            TimeComparisonItem item = new TimeComparisonItem();
            item.setComparisonId(comparison.getId()); item.setMediaId(source.mediaId());
            item.setLabel(nullable(source.label())); item.setSortOrder(Objects.requireNonNullElse(source.sortOrder(), i));
            itemMapper.insert(item);
            items.add(new AdminTimeComparisonItemVO(item.getId(), item.getMediaId(), item.getLabel(), item.getSortOrder()));
        }
        return new AdminTimeComparisonVO(comparison.getId(), comparison.getLocationId(), comparison.getTitle(),
                comparison.getDescription(), comparison.getStatus(), items, comparison.getCreatedAt(), comparison.getUpdatedAt());
    }

    private String nullable(String value) { return value == null || value.isBlank() ? null : value.strip(); }
}
