package cn.sduonline.business.service;

import cn.sduonline.business.data.dto.AdminCreateLocationRequest;
import cn.sduonline.business.data.dto.AdminUpdateLocationRequest;
import cn.sduonline.business.data.po.Campus;
import cn.sduonline.business.data.po.Location;
import cn.sduonline.business.data.vo.AdminLocationVO;
import cn.sduonline.business.mapper.CampusMapper;
import cn.sduonline.business.mapper.LocationMapper;
import cn.sduonline.common.exception.BizCode;
import cn.sduonline.common.exception.BizException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AdminLocationService {
    private static final int ENABLED = 1;
    private final LocationMapper locationMapper;
    private final CampusMapper campusMapper;

    @Transactional
    public AdminLocationVO create(AdminCreateLocationRequest r) {
        requireCampus(r.campusId());
        LocalDateTime now = LocalDateTime.now();
        Location location = new Location();
        location.setCampusId(r.campusId()); location.setName(required(r.name(), "地点名称不能为空"));
        location.setCategoryCode(nullable(r.categoryCode())); location.setAddress(nullable(r.address()));
        location.setLongitude(r.longitude()); location.setLatitude(r.latitude());
        location.setCoverKey(nullable(r.coverUrl())); location.setDescription(nullable(r.description()));
        location.setSortOrder(Objects.requireNonNullElse(r.sortOrder(), 0));
        location.setStatus(Objects.requireNonNullElse(r.status(), ENABLED));
        location.setCreatedAt(now); location.setUpdatedAt(now);
        locationMapper.insert(location);
        return toVO(location);
    }

    @Transactional
    public AdminLocationVO update(Long id, AdminUpdateLocationRequest r) {
        Location location = locationMapper.selectById(id);
        if (location == null) throw new BizException(BizCode.ADMIN_LOCATION_NOT_FOUND);
        if (r.campusId() == null && r.name() == null && r.categoryCode() == null && r.address() == null
                && r.longitude() == null && r.latitude() == null && r.coverUrl() == null
                && r.description() == null && r.sortOrder() == null && r.status() == null) {
            throw new BizException(BizCode.ADMIN_LOCATION_UPDATE_EMPTY);
        }
        if (r.campusId() != null) { requireCampus(r.campusId()); location.setCampusId(r.campusId()); }
        if (r.name() != null) { location.setName(required(r.name(), "地点名称不能为空")); }
        if (r.categoryCode() != null) { location.setCategoryCode(nullable(r.categoryCode())); }
        if (r.address() != null) { location.setAddress(nullable(r.address())); }
        if (r.longitude() != null) { location.setLongitude(r.longitude()); }
        if (r.latitude() != null) { location.setLatitude(r.latitude()); }
        if (r.coverUrl() != null) { location.setCoverKey(nullable(r.coverUrl())); }
        if (r.description() != null) { location.setDescription(nullable(r.description())); }
        if (r.sortOrder() != null) { location.setSortOrder(r.sortOrder()); }
        if (r.status() != null) { location.setStatus(r.status()); }
        LocalDateTime now = LocalDateTime.now();
        location.setUpdatedAt(now);
        locationMapper.updatePartial(id, r, location, now);
        return toVO(location);
    }

    private void requireCampus(Long id) {
        Campus campus = campusMapper.selectOne(new LambdaQueryWrapper<Campus>()
                .eq(Campus::getId, id).eq(Campus::getStatus, ENABLED));
        if (campus == null) throw new BizException(BizCode.ADMIN_LOCATION_CAMPUS_INVALID);
    }

    private AdminLocationVO toVO(Location l) {
        return new AdminLocationVO(l.getId(), l.getCampusId(), l.getName(), l.getCategoryCode(),
                l.getAddress(), l.getLongitude(), l.getLatitude(), l.getCoverKey(), l.getDescription(),
                l.getSortOrder(), l.getStatus(), l.getCreatedAt(), l.getUpdatedAt());
    }

    private String required(String v, String message) {
        String result = nullable(v); if (result == null) throw new BizException(BizCode.BAD_REQUEST, message); return result;
    }
    private String nullable(String v) { return v == null || v.isBlank() ? null : v.strip(); }
}
