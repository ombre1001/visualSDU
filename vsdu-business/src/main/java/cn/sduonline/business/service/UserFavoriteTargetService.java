package cn.sduonline.business.service;

import cn.sduonline.business.data.enums.UserStatus;
import cn.sduonline.business.data.po.Campus;
import cn.sduonline.business.data.po.City;
import cn.sduonline.business.data.po.Location;
import cn.sduonline.business.data.po.Topic;
import cn.sduonline.business.data.po.User;
import cn.sduonline.business.data.vo.LocationFavoriteVO;
import cn.sduonline.business.data.vo.LocationListVO;
import cn.sduonline.business.data.vo.TopicFavoriteVO;
import cn.sduonline.business.data.vo.TopicSummaryVO;
import cn.sduonline.business.mapper.CampusMapper;
import cn.sduonline.business.mapper.CityMapper;
import cn.sduonline.business.mapper.LocationMapper;
import cn.sduonline.business.mapper.TopicMediaMapper;
import cn.sduonline.business.mapper.TopicMapper;
import cn.sduonline.business.mapper.UserFavoriteTargetMapper;
import cn.sduonline.business.mapper.UserMapper;
import cn.sduonline.business.security.context.CurrentUser;
import cn.sduonline.common.exception.BizCode;
import cn.sduonline.common.exception.BizException;
import cn.sduonline.common.result.PageResult;
import cn.sduonline.infrastructure.file.storage.FileStorage;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserFavoriteTargetService {

    private static final int ENABLED = 1;
    private static final String LOCATION = "LOCATION";
    private static final String TOPIC = "TOPIC";

    private final UserFavoriteTargetMapper favoriteTargetMapper;
    private final UserMapper userMapper;
    private final LocationMapper locationMapper;
    private final CampusMapper campusMapper;
    private final CityMapper cityMapper;
    private final TopicMapper topicMapper;
    private final TopicMediaMapper topicMediaMapper;
    private final FileStorage fileStorage;

    @Transactional
    public LocationFavoriteVO favoriteLocation(Long userId, Long locationId) {
        requireFormalUser(userId);
        requireEnabledLocation(locationId);

        if (favoriteTargetMapper.insertIgnore(userId, LOCATION, locationId) == 0) {
            throw new BizException(BizCode.FAVORITE_TARGET_ALREADY_FAVORITED);
        }

        return locationInteraction(userId, locationId);
    }

    @Transactional
    public LocationFavoriteVO unfavoriteLocation(Long userId, Long locationId) {
        requireFormalUser(userId);
        requireEnabledLocation(locationId);

        if (favoriteTargetMapper.deleteRelation(userId, LOCATION, locationId) == 0) {
            throw new BizException(BizCode.FAVORITE_TARGET_NOT_FAVORITED);
        }

        return locationInteraction(userId, locationId);
    }

    @Transactional
    public TopicFavoriteVO favoriteTopic(Long userId, Long topicId) {
        requireFormalUser(userId);
        requireEnabledTopic(topicId);

        if (favoriteTargetMapper.insertIgnore(userId, TOPIC, topicId) == 0) {
            throw new BizException(BizCode.FAVORITE_TARGET_ALREADY_FAVORITED);
        }

        return topicInteraction(userId, topicId);
    }

    @Transactional
    public TopicFavoriteVO unfavoriteTopic(Long userId, Long topicId) {
        requireFormalUser(userId);
        requireEnabledTopic(topicId);

        if (favoriteTargetMapper.deleteRelation(userId, TOPIC, topicId) == 0) {
            throw new BizException(BizCode.FAVORITE_TARGET_NOT_FAVORITED);
        }

        return topicInteraction(userId, topicId);
    }

    public PageResult<LocationListVO> listFavoriteLocations(
            Long userId,
            long page,
            long size
    ) {
        requireFormalUser(userId);

        long safePage = Math.max(page, 1);
        long safeSize = Math.clamp(size, 1, 50);
        long total = favoriteTargetMapper.countFavoriteLocations(userId);
        long offset = (safePage - 1) * safeSize;

        List<LocationListVO> items = total == 0
                ? List.of()
                : favoriteTargetMapper.selectFavoriteLocations(userId, offset, safeSize)
                        .stream()
                        .map(location -> toLocationListVO(location, userId, true))
                        .toList();

        return new PageResult<>(total, safePage, safeSize, items);
    }

    public PageResult<TopicSummaryVO> listFavoriteTopics(
            Long userId,
            long page,
            long size
    ) {
        requireFormalUser(userId);

        long safePage = Math.max(page, 1);
        long safeSize = Math.clamp(size, 1, 50);
        long total = favoriteTargetMapper.countFavoriteTopics(userId);
        long offset = (safePage - 1) * safeSize;

        List<TopicSummaryVO> items = total == 0
                ? List.of()
                : favoriteTargetMapper.selectFavoriteTopics(userId, offset, safeSize)
                        .stream()
                        .map(topic -> toTopicSummaryVO(topic, userId, true))
                        .toList();

        return new PageResult<>(total, safePage, safeSize, items);
    }

    public LocationFavoriteVO locationInteraction(Long userId, Long locationId) {
        return new LocationFavoriteVO(
                locationId,
                countLocationFavorites(locationId),
                isLocationFavorited(userId, locationId)
        );
    }

    public TopicFavoriteVO topicInteraction(Long userId, Long topicId) {
        return new TopicFavoriteVO(
                topicId,
                countTopicFavorites(topicId),
                isTopicFavorited(userId, topicId)
        );
    }

    public long countLocationFavorites(Long locationId) {
        return favoriteTargetMapper.countByTarget(LOCATION, locationId);
    }

    public long countTopicFavorites(Long topicId) {
        return favoriteTargetMapper.countByTarget(TOPIC, topicId);
    }

    public boolean isLocationFavorited(Long userId, Long locationId) {
        return userId != null
                && favoriteTargetMapper.existsByUserTarget(userId, LOCATION, locationId);
    }

    public boolean isTopicFavorited(Long userId, Long topicId) {
        return userId != null
                && favoriteTargetMapper.existsByUserTarget(userId, TOPIC, topicId);
    }

    public Long currentFormalUserIdOrNull() {
        if (!CurrentUser.isLogin()) {
            return null;
        }

        User user = userMapper.selectById(CurrentUser.id());
        return isFormalUser(user) ? user.getId() : null;
    }

    public LocationListVO toLocationListVO(
            Location location,
            Long userId,
            boolean favorited
    ) {
        return LocationListVO.builder()
                .id(location.getId())
                .campusId(location.getCampusId())
                .name(location.getName())
                .categoryCode(location.getCategoryCode())
                .address(location.getAddress())
                .longitude(location.getLongitude())
                .latitude(location.getLatitude())
                .coverUrl(url(location.getCoverKey()))
                .favoriteCount(countLocationFavorites(location.getId()))
                .favorited(favorited || isLocationFavorited(userId, location.getId()))
                .build();
    }

    public TopicSummaryVO toTopicSummaryVO(
            Topic topic,
            Long userId,
            boolean favorited
    ) {
        return new TopicSummaryVO(
                topic.getId(),
                topic.getName(),
                topic.getSlug(),
                topic.getDescription(),
                topic.getCoverUrl(),
                topicMediaMapper.countVisibleMedia(topic.getId()),
                countTopicFavorites(topic.getId()),
                favorited || isTopicFavorited(userId, topic.getId())
        );
    }

    private User requireFormalUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (!isFormalUser(user)) {
            throw new BizException(BizCode.FORMAL_USER_REQUIRED);
        }
        return user;
    }

    private boolean isFormalUser(User user) {
        return user != null
                && !Boolean.TRUE.equals(user.getDeleted())
                && user.getCasId() != null
                && !user.getCasId().isBlank()
                && user.getRole() != null
                && user.getStatus() == UserStatus.NORMAL;
    }

    private Location requireEnabledLocation(Long locationId) {
        if (locationId == null || locationId <= 0) {
            throw new BizException(BizCode.LOCATION_NOT_FOUND);
        }

        Location location = locationMapper.selectOne(
                new LambdaQueryWrapper<Location>()
                        .eq(Location::getId, locationId)
                        .eq(Location::getStatus, ENABLED)
        );

        if (location == null) {
            throw new BizException(BizCode.LOCATION_NOT_FOUND);
        }

        Campus campus = campusMapper.selectOne(
                new LambdaQueryWrapper<Campus>()
                        .eq(Campus::getId, location.getCampusId())
                        .eq(Campus::getStatus, ENABLED)
        );

        if (campus == null) {
            throw new BizException(BizCode.CAMPUS_NOT_FOUND);
        }

        City city = cityMapper.selectOne(
                new LambdaQueryWrapper<City>()
                        .eq(City::getId, campus.getCityId())
                        .eq(City::getStatus, ENABLED)
        );

        if (city == null) {
            throw new BizException(BizCode.CITY_NOT_FOUND);
        }

        return location;
    }

    private Topic requireEnabledTopic(Long topicId) {
        if (topicId == null || topicId <= 0) {
            throw new BizException(BizCode.TOPIC_NOT_FOUND);
        }

        Topic topic = topicMapper.selectOne(
                new LambdaQueryWrapper<Topic>()
                        .eq(Topic::getId, topicId)
                        .eq(Topic::getStatus, ENABLED)
        );

        if (topic == null) {
            throw new BizException(BizCode.TOPIC_NOT_FOUND);
        }

        return topic;
    }

    private String url(String coverKey) {
        return coverKey == null || coverKey.isBlank()
                ? null
                : fileStorage.getUrl(coverKey);
    }
}
