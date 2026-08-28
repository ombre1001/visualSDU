SET NAMES utf8mb4;

create table announcement
(
    id           bigint auto_increment comment '公告ID'
        primary key,
    title        varchar(200)                                  not null comment '公告标题',
    summary      varchar(500)                                  null comment '公告摘要',
    content      mediumtext                                    not null comment '公告正文',
    status       tinyint unsigned default '0'                  not null comment '状态：0-草稿，1-已发布，2-已下线',
    is_pinned    tinyint(1)       default 0                    not null comment '是否置顶：0-否，1-是',
    sort_order   int unsigned     default '0'                  not null comment '排序值，越小越靠前',
    published_at datetime(3)                                   null comment '发布时间',
    created_by   bigint                                        not null comment '创建管理员ID',
    updated_by   bigint                                        not null comment '最后修改管理员ID',
    created_at   datetime(3)      default CURRENT_TIMESTAMP(3) not null comment '创建时间',
    updated_at   datetime(3)      default CURRENT_TIMESTAMP(3) not null on update CURRENT_TIMESTAMP(3) comment '更新时间',
    constraint chk_announcement_is_pinned
        check (`is_pinned` in (0, 1)),
    constraint chk_announcement_status
        check (`status` in (0, 1, 2))
)
    comment '公告表' collate = utf8mb4_unicode_ci;

create index idx_announcement_admin
    on announcement (status, updated_at, id);

create index idx_announcement_created_by
    on announcement (created_by);

create index idx_announcement_public
    on announcement (status, is_pinned, sort_order, published_at, id);

create table city
(
    id          bigint unsigned auto_increment comment '城市ID'
        primary key,
    name        varchar(50)                        not null comment '城市名称',
    code        varchar(32)                        not null comment '城市唯一编码，如 JINAN',
    province    varchar(50)                        null comment '所属省份',
    cover_url   varchar(500)                       null comment '城市封面图URL',
    description varchar(1000)                      null comment '城市简介',
    sort_order  int      default 0                 not null comment '排序值，越小越靠前',
    status      tinyint  default 1                 not null comment '状态：0停用，1启用',
    created_at  datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updated_at  datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint uk_city_code
        unique (code),
    constraint ck_city_status
        check (`status` in (0, 1))
)
    comment '城市表' collate = utf8mb4_unicode_ci;

create table campus
(
    id          bigint unsigned auto_increment comment '校区ID'
        primary key,
    city_id     bigint unsigned                    not null comment '所属城市ID',
    name        varchar(100)                       not null comment '校区完整名称',
    short_name  varchar(50)                        null comment '校区简称',
    address     varchar(255)                       null comment '校区详细地址',
    longitude   decimal(10, 7)                     not null comment '校区中心点经度',
    latitude    decimal(10, 7)                     not null comment '校区中心点纬度',
    cover_url   varchar(500)                       null comment '校区封面图URL',
    description text                               null comment '校区介绍',
    sort_order  int      default 0                 not null comment '同一城市内排序',
    status      tinyint  default 1                 not null comment '状态：0停用，1启用',
    created_at  datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updated_at  datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint uk_campus_city_name
        unique (city_id, name),
    constraint fk_campus_city
        foreign key (city_id) references city (id)
            on update cascade,
    constraint ck_campus_latitude
        check (`latitude` between -(90) and 90),
    constraint ck_campus_longitude
        check (`longitude` between -(180) and 180),
    constraint ck_campus_status
        check (`status` in (0, 1))
)
    comment '山东大学校区表' collate = utf8mb4_unicode_ci;

create index idx_campus_city_status
    on campus (city_id, status, sort_order, id);

create index idx_campus_city_status_sort
    on campus (city_id, status, sort_order, id);

create index idx_campus_coordinate
    on campus (longitude, latitude);

create index idx_city_status_sort
    on city (status, sort_order, id);

create table location
(
    id            bigint unsigned auto_increment comment '地点ID'
        primary key,
    campus_id     bigint unsigned                            not null comment '所属校区ID',
    name          varchar(100)                               not null comment '地点名称',
    category_code varchar(32)                                null comment '地点分类编码，如 LIBRARY、CANTEEN',
    address       varchar(255)                               null comment '详细地址或校内位置描述',
    longitude     decimal(10, 7)                             not null comment '地点经度',
    latitude      decimal(10, 7)                             not null comment '地点纬度',
    cover_key     varchar(500) default 'avatars/default.png' null comment '地点封面图ObjectKey',
    description   text                                       null comment '地点介绍',
    sort_order    int          default 0                     not null comment '地点排序值',
    status        tinyint      default 1                     not null comment '状态：0停用，1启用',
    created_at    datetime     default CURRENT_TIMESTAMP     not null comment '创建时间',
    updated_at    datetime     default CURRENT_TIMESTAMP     not null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint fk_location_campus
        foreign key (campus_id) references campus (id)
            on update cascade,
    constraint ck_location_latitude
        check (`latitude` between -(90) and 90),
    constraint ck_location_longitude
        check (`longitude` between -(180) and 180),
    constraint ck_location_status
        check (`status` in (0, 1))
)
    comment '校区地点及地图点位表' collate = utf8mb4_unicode_ci;

create index idx_location_campus_category
    on location (campus_id, category_code, status);

create index idx_location_campus_status
    on location (campus_id, status, sort_order, id);

create index idx_location_campus_status_sort
    on location (campus_id, status, sort_order, id);

create index idx_location_coordinate
    on location (longitude, latitude);

create index idx_location_name
    on location (name);

create table submission_review_log
(
    id                 bigint auto_increment comment '审核记录ID'
        primary key,
    submission_id      bigint                                   not null comment '稿件ID',
    round_no           int                                      not null comment '审核轮次，从1开始',
    submission_version int                                      not null comment '审核前的稿件版本',
    decision           tinyint                                  not null comment '审核决定：1=通过，2=退回修改，3=永久拒绝',
    reason             varchar(1000)                            null comment '审核原因；退回或拒绝时必填',
    before_status      tinyint                                  not null comment '审核前状态',
    after_status       tinyint                                  not null comment '审核后状态',
    reviewed_by        bigint                                   not null comment '审核管理员用户ID',
    reviewed_at        datetime(3) default CURRENT_TIMESTAMP(3) not null comment '审核时间',
    constraint uk_submission_review_round
        unique (submission_id, round_no),
    constraint uk_submission_review_version
        unique (submission_id, submission_version)
)
    comment '投稿审核记录';

create index idx_reviewer_time
    on submission_review_log (reviewed_by, reviewed_at);

create index idx_submission_review_time
    on submission_review_log (submission_id, reviewed_at, id);

create table tag
(
    id         bigint auto_increment
        primary key,
    name       varchar(32)                        not null,
    created_at datetime default CURRENT_TIMESTAMP not null,
    updated_at datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    constraint uk_tag_name
        unique (name)
)
    collate = utf8mb4_unicode_ci;

create table time_comparison
(
    id          bigint unsigned auto_increment
        primary key,
    location_id bigint unsigned                            not null,
    title       varchar(150)                               not null,
    description varchar(1000)                              null,
    status      tinyint unsigned default '1'               not null comment '0隐藏 1可见',
    created_at  datetime         default CURRENT_TIMESTAMP not null,
    updated_at  datetime         default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    constraint fk_comparison_location
        foreign key (location_id) references location (id),
    constraint ck_comparison_status
        check (`status` in (0, 1))
)
    comment '时光对比专题' collate = utf8mb4_unicode_ci;

create index idx_comparison_location_status
    on time_comparison (location_id, status, updated_at, id);

create table topic
(
    id          bigint auto_increment
        primary key,
    name        varchar(100)                       not null,
    slug        varchar(100)                       not null,
    description varchar(500)                       null,
    cover_url   varchar(500)                       null,
    status      tinyint  default 1                 not null comment '0-停用 1-启用',
    sort_order  int      default 0                 not null,
    created_at  datetime default CURRENT_TIMESTAMP not null,
    updated_at  datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    constraint uk_topic_slug
        unique (slug)
)
    collate = utf8mb4_unicode_ci;

create index idx_topic_status_sort
    on topic (status, sort_order, id);

create table user
(
    id             bigint unsigned auto_increment comment '用户ID'
        primary key,
    phone          varchar(20)                                           null comment '手机号，短信登录用户唯一标识',
    password_hash  varchar(100)                                          null comment 'BCrypt密码摘要',
    cas_id         varchar(64)                                           null comment '山大统一认证CAS ID，正式用户登录账号',
    name           varchar(64)                                           null comment '统一认证返回的真实姓名',
    nickname       varchar(64)                                           not null comment '用户昵称',
    avatar_key     varchar(512)     default 'avatars/default.png'        null comment '头像key',
    bio            varchar(255)     default '这个人很懒，什么也没有写哦~' null comment '个人简介',
    role           tinyint unsigned default '0'                          not null comment '角色：0正式用户，1管理员',
    status         tinyint unsigned default '1'                          not null comment '状态：0停用，1正常，2冻结',
    frozen_until   datetime                                              null comment '冻结截止时间；NULL表示永久冻结',
    frozen_reason  varchar(255)                                          null comment '冻结原因',
    allow_upload   tinyint(1)       default 1                            not null comment '是否允许上传',
    allow_download tinyint(1)       default 1                            not null comment '是否允许下载原图',
    last_login_at  datetime                                              null comment '最后登录时间',
    created_at     datetime         default CURRENT_TIMESTAMP            not null comment '创建时间',
    updated_at     datetime         default CURRENT_TIMESTAMP            not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted        tinyint(1)       default 0                            not null comment '逻辑删除：0否，1是',
    token_version  int              default 0                            null comment 'local JWT 代数',
    constraint uk_user_cas_id
        unique (cas_id),
    constraint uk_user_phone
        unique (phone),
    constraint chk_user_cas_name
        check ((`cas_id` is null) or (`name` is not null)),
    constraint chk_user_login_identity
        check ((`cas_id` is not null) or (`phone` is not null)),
    constraint chk_user_role
        check (`role` in (0, 1, 2))
)
    comment '用户账号' collate = utf8mb4_unicode_ci;

create table favorite_folder
(
    id             bigint unsigned auto_increment comment '收藏夹ID'
        primary key,
    user_id        bigint unsigned                      not null comment '所有者用户ID',
    name           varchar(64)                          not null comment '收藏夹名称',
    description    varchar(255)                         null comment '收藏夹描述',
    cover_media_id bigint unsigned                      null comment '封面媒体ID',
    is_default     tinyint(1) default 0                 not null comment '是否默认收藏夹',
    sort_order     int        default 0                 not null comment '排序值',
    created_at     datetime   default CURRENT_TIMESTAMP not null comment '创建时间',
    updated_at     datetime   default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted        tinyint(1) default 0                 not null comment '逻辑删除',
    constraint uk_folder_user_name_deleted
        unique (user_id, name, deleted),
    constraint fk_folder_user
        foreign key (user_id) references user (id)
)
    comment '用户收藏夹' collate = utf8mb4_unicode_ci;

create index idx_folder_user_sort
    on favorite_folder (user_id, sort_order, id);

create table media_favorite
(
    id         bigint unsigned auto_increment comment '收藏记录ID'
        primary key,
    user_id    bigint unsigned                    not null comment '用户ID',
    folder_id  bigint unsigned                    not null comment '收藏夹ID',
    media_id   bigint unsigned                    not null comment '媒体ID',
    created_at datetime default CURRENT_TIMESTAMP not null comment '收藏时间',
    constraint uk_favorite_folder_media
        unique (folder_id, media_id),
    constraint fk_favorite_folder
        foreign key (folder_id) references favorite_folder (id),
    constraint fk_favorite_user
        foreign key (user_id) references user (id)
)
    comment '媒体收藏关系' collate = utf8mb4_unicode_ci;

create index idx_favorite_media
    on media_favorite (media_id);

create index idx_favorite_user_created
    on media_favorite (user_id, created_at, id);

create index idx_favorite_user_folder_created
    on media_favorite (user_id, folder_id, created_at, id);

create table media_like
(
    id         bigint unsigned auto_increment comment '点赞记录ID'
        primary key,
    user_id    bigint unsigned                    not null comment '用户ID（游客手机号登录后也会生成用户记录）',
    media_id   bigint unsigned                    not null comment '媒体ID',
    created_at datetime default CURRENT_TIMESTAMP not null comment '点赞时间',
    constraint uk_like_user_media
        unique (user_id, media_id),
    constraint fk_like_user
        foreign key (user_id) references user (id)
)
    comment '媒体点赞关系' collate = utf8mb4_unicode_ci;

create index idx_like_media_created
    on media_like (media_id, created_at);

create table submission
(
    id            bigint unsigned auto_increment comment '稿件ID'
        primary key,
    user_id       bigint unsigned                            not null comment '投稿用户ID',
    location_id   bigint unsigned                            not null comment '拍摄地点ID',
    description   text                                       null comment '稿件描述',
    shot_at       datetime                                   null comment '拍摄时间',
    tags          varchar(1000)                              null comment '竖线分隔的标签',
    status        tinyint unsigned default '0'               not null comment '0待审 1通过 2退回 3撤回 4被拒',
    review_reason varchar(1000)                              null comment '退回原因',
    submitted_at  datetime                                   not null comment '最近提交审核时间',
    reviewed_by   bigint unsigned                            null comment '审核管理员ID',
    reviewed_at   datetime                                   null comment '审核时间',
    created_at    datetime         default CURRENT_TIMESTAMP not null,
    updated_at    datetime         default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    deleted       tinyint(1)       default 0                 not null,
    version       int              default 0                 not null comment '乐观锁版本号',
    constraint fk_submission_location
        foreign key (location_id) references location (id),
    constraint fk_submission_reviewer
        foreign key (reviewed_by) references user (id),
    constraint fk_submission_user
        foreign key (user_id) references user (id),
    constraint ck_submission_status
        check (`status` in (0, 1, 2, 3))
)
    comment '用户图片投稿' collate = utf8mb4_unicode_ci;

create table media
(
    id             bigint unsigned auto_increment comment '媒体ID'
        primary key,
    submission_id  bigint unsigned                            null comment '来源稿件ID',
    uploader_id    bigint unsigned                            null comment '投稿用户ID',
    location_id    bigint unsigned                            not null comment '地点ID',
    object_key     varchar(512)                               not null comment 'R2原图object key',
    thumbnail_key  varchar(512)                               null comment 'R2缩略图object key',
    title          varchar(150)                               null comment '标题',
    description    text                                       null comment '图片说明',
    shot_at        datetime                                   null comment '拍摄时间',
    tags           varchar(1000)                              null comment '竖线分隔的标签',
    status         tinyint unsigned default '1'               not null comment '0隐藏 1可见',
    view_count     bigint unsigned  default '0'               not null,
    like_count     bigint unsigned  default '0'               not null,
    favorite_count bigint unsigned  default '0'               not null,
    download_count bigint unsigned  default '0'               not null,
    created_at     datetime         default CURRENT_TIMESTAMP not null,
    updated_at     datetime         default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    constraint uk_media_object_key
        unique (object_key),
    constraint fk_media_location
        foreign key (location_id) references location (id),
    constraint fk_media_submission
        foreign key (submission_id) references submission (id),
    constraint fk_media_uploader
        foreign key (uploader_id) references user (id),
    constraint ck_media_status
        check (`status` in (0, 1))
)
    comment '审核通过后对外展示的媒体' collate = utf8mb4_unicode_ci;

create index idx_media_discovery
    on media (status, like_count, view_count, id);

create index idx_media_location_status
    on media (location_id, status, created_at, id);

create index idx_media_status_hot
    on media (status, favorite_count, like_count, view_count, id);

create index idx_media_status_location_created
    on media (status, location_id, created_at, id);

create index idx_media_status_shot_at
    on media (status, shot_at, id);

create index idx_media_submission
    on media (submission_id);

create table media_download
(
    id         bigint unsigned auto_increment
        primary key,
    user_id    bigint unsigned                    not null,
    media_id   bigint unsigned                    not null,
    object_key varchar(512)                       not null comment '申请时对应的原图key',
    created_at datetime default CURRENT_TIMESTAMP not null,
    constraint fk_media_download_media
        foreign key (media_id) references media (id),
    constraint fk_media_download_user
        foreign key (user_id) references user (id)
)
    comment '媒体下载审计记录' collate = utf8mb4_unicode_ci;

create index idx_media_download_media
    on media_download (media_id, created_at);

create index idx_media_download_user
    on media_download (user_id, created_at, id);

create index idx_submission_location
    on submission (location_id, status);

create index idx_submission_review
    on submission (status, submitted_at, id);

create index idx_submission_review_queue
    on submission (deleted, status, submitted_at, id);

create index idx_submission_user_status
    on submission (user_id, status, updated_at, id);

create table submission_asset
(
    id            bigint unsigned auto_increment comment '稿件图片ID'
        primary key,
    submission_id bigint unsigned                    not null,
    object_key    varchar(512)                       not null comment 'R2 object key',
    original_name varchar(255)                       null,
    content_type  varchar(100)                       not null,
    size_bytes    bigint unsigned                    not null,
    sort_order    int      default 0                 not null,
    media_id      bigint unsigned                    null comment '审核通过后生成的媒体ID',
    created_at    datetime default CURRENT_TIMESTAMP not null,
    constraint uk_submission_asset_key
        unique (object_key),
    constraint uk_submission_asset_media
        unique (media_id),
    constraint fk_submission_asset_media
        foreign key (media_id) references media (id),
    constraint fk_submission_asset_submission
        foreign key (submission_id) references submission (id)
)
    comment '稿件图片资源' collate = utf8mb4_unicode_ci;

create index idx_submission_asset_sort
    on submission_asset (submission_id, sort_order, id);

create table submission_review_setting
(
    id             tinyint unsigned                     not null comment '固定为1'
        primary key,
    review_enabled tinyint(1) default 1                 not null comment '1开启人工审核 0关闭审核并自动发布',
    updated_by     bigint unsigned                      null comment '最后操作管理员ID',
    updated_at     datetime   default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    constraint fk_submission_review_setting_user
        foreign key (updated_by) references user (id),
    constraint ck_submission_review_setting_enabled
        check (`review_enabled` in (0, 1)),
    constraint ck_submission_review_setting_id
        check (`id` = 1)
)
    comment '投稿审核开关' collate = utf8mb4_unicode_ci;

create table time_comparison_item
(
    id            bigint unsigned auto_increment
        primary key,
    comparison_id bigint unsigned not null,
    media_id      bigint unsigned not null,
    label         varchar(100)    null comment '例如1985年、2026年',
    sort_order    int default 0   not null,
    constraint uk_comparison_media
        unique (comparison_id, media_id),
    constraint fk_comparison_item_comparison
        foreign key (comparison_id) references time_comparison (id),
    constraint fk_comparison_item_media
        foreign key (media_id) references media (id)
)
    comment '时光对比媒体项' collate = utf8mb4_unicode_ci;

create index idx_comparison_item_sort
    on time_comparison_item (comparison_id, sort_order, id);

create table topic_media
(
    topic_id   bigint                             not null,
    media_id   bigint unsigned                    not null,
    sort_order int      default 0                 not null,
    created_at datetime default CURRENT_TIMESTAMP not null,
    primary key (topic_id, media_id),
    constraint fk_topic_media_media
        foreign key (media_id) references media (id),
    constraint fk_topic_media_topic
        foreign key (topic_id) references topic (id)
)
    collate = utf8mb4_unicode_ci;

create index idx_topic_media_media
    on topic_media (media_id);

create index idx_topic_media_sort
    on topic_media (topic_id, sort_order, media_id);

create index idx_user_created_at
    on user (created_at);

create index idx_user_role_status
    on user (role, status);

create table user_admin_operation_log
(
    id             bigint unsigned auto_increment comment '日志ID'
        primary key,
    operator_id    bigint unsigned                    not null comment '操作管理员ID',
    target_user_id bigint unsigned                    not null comment '目标用户ID',
    operation_type varchar(32)                        not null comment 'FREEZE/UNFREEZE/REVOKE_ROLE/UPDATE_PERMISSION',
    reason         varchar(255)                       null comment '操作原因',
    detail_json    json                               null comment '变更明细',
    created_at     datetime default CURRENT_TIMESTAMP not null comment '操作时间',
    constraint fk_user_admin_log_operator
        foreign key (operator_id) references user (id),
    constraint fk_user_admin_log_target
        foreign key (target_user_id) references user (id)
)
    comment '管理员用户操作审计日志' collate = utf8mb4_unicode_ci;

create index idx_user_admin_log_operator
    on user_admin_operation_log (operator_id, created_at);

create index idx_user_admin_log_target
    on user_admin_operation_log (target_user_id, created_at);

create table user_browse_history
(
    id              bigint unsigned auto_increment comment '足迹ID'
        primary key,
    user_id         bigint unsigned                        not null comment '用户ID',
    media_id        bigint unsigned                        not null comment '媒体ID',
    view_count      int unsigned default '1'               not null comment '浏览次数',
    first_viewed_at datetime     default CURRENT_TIMESTAMP not null comment '首次浏览时间',
    last_viewed_at  datetime     default CURRENT_TIMESTAMP not null comment '最近浏览时间',
    constraint uk_history_user_media
        unique (user_id, media_id),
    constraint fk_history_user
        foreign key (user_id) references user (id)
)
    comment '用户浏览足迹' collate = utf8mb4_unicode_ci;

create index idx_history_media
    on user_browse_history (media_id);

create index idx_history_user_last_viewed
    on user_browse_history (user_id, last_viewed_at, id);


-- MySQL dump 10.13  Distrib 8.4.11, for Linux (aarch64)
--
-- Host: localhost    Database: vsdu-db
-- ------------------------------------------------------
-- Server version	8.4.11

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Dumping data for table `announcement`
--

LOCK TABLES `announcement` WRITE;
/*!40000 ALTER TABLE `announcement` DISABLE KEYS */;
/*!40000 ALTER TABLE `announcement` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `campus`
--

LOCK TABLES `campus` WRITE;
/*!40000 ALTER TABLE `campus` DISABLE KEYS */;
INSERT INTO `campus` (`id`, `city_id`, `name`, `short_name`, `address`, `longitude`, `latitude`, `cover_url`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (1,1,'山东大学中心校区','中心校区','山东省济南市山大南路27号',117.0600268,36.6757805,NULL,'截至2025年9月，校区下设哲学与社会发展学院、经济学院、文学院、历史学院、考古学院、数学学院、物理学院、化学与化工学院、管理学院、马克思主义学院、国际教育学院、新闻传播学院、经济研究院、儒学高等研究院（文史哲研究院）、外国语学院（大学外语教学部）、晶体材料研究院、中泰证券金融研究院、新一代半导体材料研究院、人工智能学院、国家卓越工程师学院、教育高等研究院、黄河国家战略研究院、智能通信技术研究院等23个学院。',1,1,'2026-08-01 01:07:34','2026-08-01 01:07:34');
INSERT INTO `campus` (`id`, `city_id`, `name`, `short_name`, `address`, `longitude`, `latitude`, `cover_url`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (2,1,'山东大学软件园校区','软件园校区','山东省济南市舜华路1500号',117.1384996,36.6669768,NULL,'截至2025年9月，校区下设软件学院、集成电路学院、人工智能国际联合研究院、智能创新研究院等4个学院和1个中加合作办学项目。',2,1,'2026-08-01 01:07:34','2026-08-01 01:07:34');
INSERT INTO `campus` (`id`, `city_id`, `name`, `short_name`, `address`, `longitude`, `latitude`, `cover_url`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (3,1,'山东大学洪家楼校区','洪家楼校区','山东省济南市洪家楼5号',117.0660701,36.6862478,NULL,'截至2025年9月，校区下设外国语学院和艺术学院等2个学院。',3,1,'2026-08-01 01:07:34','2026-08-01 01:07:34');
INSERT INTO `campus` (`id`, `city_id`, `name`, `short_name`, `address`, `longitude`, `latitude`, `cover_url`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (4,1,'山东大学千佛山校区','千佛山校区','山东省济南市历下区经十路17923号',117.0289869,36.6490126,NULL,'截至2025年9月，校区下设材料科学与工程学院、机械工程学院、控制科学与工程学院、核科学与能源动力学院、电气工程学院、土建与水利学院、体育学院等7个学院。',4,1,'2026-08-01 01:07:34','2026-08-01 01:07:34');
INSERT INTO `campus` (`id`, `city_id`, `name`, `short_name`, `address`, `longitude`, `latitude`, `cover_url`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (5,1,'山东大学趵突泉校区','趵突泉校区','山东省济南市历下区文化西路44号',117.0196517,36.6530261,NULL,'趵突泉校区也被称为齐鲁医学院，其前身为1864年创办的山东登州文会馆，截至2025年3月，下设基础医学院、公共卫生学院、口腔医学院、护理与康复学院、药学院、第一临床学院、第二临床学院、生物医学工程学院（筹）、精神与心理健康学院（研究院）（筹）9个学院及医学融合与实践中心。',5,1,'2026-08-01 01:07:34','2026-08-01 01:07:34');
INSERT INTO `campus` (`id`, `city_id`, `name`, `short_name`, `address`, `longitude`, `latitude`, `cover_url`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (6,1,'山东大学兴隆山校区','兴隆山校区','山东省济南市市中区二环东路12550号',117.0492186,36.5992778,NULL,'截至2025年9月，校区下设材料科学与工程学院、机械工程学院、控制科学与工程学院、核科学与能源动力学院、电气工程学院、土建与水利学院、齐鲁交通学院、未来技术学院等8个学院。',6,1,'2026-08-01 01:07:34','2026-08-01 01:07:34');
INSERT INTO `campus` (`id`, `city_id`, `name`, `short_name`, `address`, `longitude`, `latitude`, `cover_url`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (7,2,'山东大学青岛校区','青岛校区','山东省青岛市即墨区滨海路72号',120.6876844,36.3615080,NULL,'截至2025年9月，校区下设政治学与公共管理学院、法学院、信息科学与工程学院、计算机科学与技术学院、生命科学学院、环境科学与工程学院、国际创新转化学院、网络空间安全学院、前沿交叉科学青岛研究院、人文艺术研究院、微生物技术研究院、环境研究院、海洋研究院、数学与交叉科学研究中心等14个学院。',7,1,'2026-08-01 01:07:34','2026-08-01 01:07:34');
INSERT INTO `campus` (`id`, `city_id`, `name`, `short_name`, `address`, `longitude`, `latitude`, `cover_url`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (8,3,'山东大学威海校区','威海校区','山东省威海市环翠区文化西路180号',122.0602513,37.5291383,NULL,'截至2025年9月，下设东北亚学院、翻译学院、空间科学与技术学院、商学院、艺术学院、数学与统计学院、纪检监察学院、海洋学院、文化传播学院、机电与信息工程学院、马克思主义学院、山东大学澳国立联合理学院等12学院和1个体育教学部。',8,1,'2026-08-01 01:07:34','2026-08-01 01:07:34');
/*!40000 ALTER TABLE `campus` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `city`
--

LOCK TABLES `city` WRITE;
/*!40000 ALTER TABLE `city` DISABLE KEYS */;
INSERT INTO `city` (`id`, `name`, `code`, `province`, `cover_url`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (1,'济南','JINAN','山东省',NULL,'山东大学济南地区校区',1,1,'2026-08-01 01:06:45','2026-08-01 01:06:45');
INSERT INTO `city` (`id`, `name`, `code`, `province`, `cover_url`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (2,'青岛','QINGDAO','山东省',NULL,'山东大学青岛校区',2,1,'2026-08-01 01:06:45','2026-08-01 01:06:45');
INSERT INTO `city` (`id`, `name`, `code`, `province`, `cover_url`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (3,'威海','WEIHAI','山东省',NULL,'山东大学威海校区',3,1,'2026-08-01 01:06:45','2026-08-01 01:06:45');
/*!40000 ALTER TABLE `city` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `favorite_folder`
--

LOCK TABLES `favorite_folder` WRITE;
/*!40000 ALTER TABLE `favorite_folder` DISABLE KEYS */;
INSERT INTO `favorite_folder` (`id`, `user_id`, `name`, `description`, `cover_media_id`, `is_default`, `sort_order`, `created_at`, `updated_at`, `deleted`) VALUES (1,2,'默认收藏夹','系统默认收藏夹',NULL,1,0,'2026-08-26 00:44:59','2026-08-26 00:44:59',0);
INSERT INTO `favorite_folder` (`id`, `user_id`, `name`, `description`, `cover_media_id`, `is_default`, `sort_order`, `created_at`, `updated_at`, `deleted`) VALUES (2,3,'默认收藏夹','系统默认收藏夹',NULL,1,0,'2026-08-26 13:12:25','2026-08-26 13:12:25',0);
/*!40000 ALTER TABLE `favorite_folder` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `location`
--

LOCK TABLES `location` WRITE;
/*!40000 ALTER TABLE `location` DISABLE KEYS */;
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (1,1,'中心校区北门','GATE','中心校区北侧主要出入口',117.0580373,36.6791828,'山东大学中心校区北侧主要校门',1,0,'2026-08-01 00:00:00','2026-08-27 19:23:44');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (2,1,'中心校区西门','GATE','中心校区西侧主要出入口',117.0563198,36.6747525,'山东大学中心校区西侧主要校门',2,0,'2026-08-01 00:00:00','2026-08-27 19:30:32');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (3,1,'中心校区南门','GATE','中心校区南侧主要出入口',117.0601030,36.6721210,'山东大学中心校区南侧主要校门',3,1,'2026-08-01 00:00:00','2026-08-01 00:00:00');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (4,1,'中心校区西南门','GATE','中心校区西南侧出入口',117.0592341,36.6720257,'山东大学中心校区西南侧校门',4,1,'2026-08-01 00:00:00','2026-08-01 00:00:00');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (5,1,'知新楼A座','BUILDING','中心校区知新楼A座',117.0598576,36.6768597,'知新楼组成建筑之一，主要用于教学、科研及办公',5,1,'2026-08-01 00:00:00','2026-08-01 00:00:00');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (6,1,'知新楼B座','BUILDING','中心校区知新楼B座',117.0608282,36.6768611,'知新楼组成建筑之一，主要用于教学、科研及办公',6,1,'2026-08-01 00:00:00','2026-08-01 00:00:00');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (7,1,'知新楼C座','BUILDING','中心校区知新楼C座',117.0616492,36.6768683,'知新楼组成建筑之一，主要用于教学、科研及办公',7,1,'2026-08-01 00:00:00','2026-08-01 00:00:00');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (8,1,'知新楼D座','BUILDING','中心校区知新楼D座',117.0610808,36.6763799,'知新楼组成建筑之一，蒋震图书馆所在建筑',8,1,'2026-08-01 00:00:00','2026-08-01 00:00:00');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (9,1,'邵逸夫科学馆','BUILDING','中心校区邵逸夫科学馆',117.0619800,36.6763267,'中心校区教学、科研及公共服务建筑',9,1,'2026-08-01 00:00:00','2026-08-01 00:00:00');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (10,1,'明德楼A座','BUILDING','中心校区明德楼A座',117.0600734,36.6758681,'中心校区行政办公建筑',10,1,'2026-08-01 00:00:00','2026-08-01 00:00:00');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (11,1,'明德楼B座','BUILDING','中心校区明德楼B座',117.0600734,36.6758681,'中心校区行政及师生公共服务建筑',11,1,'2026-08-01 00:00:00','2026-08-01 00:00:00');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (12,1,'明德楼C座','BUILDING','中心校区明德楼C座',117.0600734,36.6758681,'中心校区行政办公及公共服务建筑',12,1,'2026-08-01 00:00:00','2026-08-01 00:00:00');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (13,1,'公教楼','BUILDING','中心校区公共教学楼',117.0588610,36.6732711,'中心校区主要公共教学建筑',13,1,'2026-08-01 00:00:00','2026-08-01 00:00:00');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (14,1,'理综楼','BUILDING','中心校区理综楼',117.0571103,36.6732063,'中心校区理工类教学科研建筑',14,1,'2026-08-01 00:00:00','2026-08-01 00:00:00');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (15,1,'化学楼','BUILDING','中心校区化学楼',117.0601393,36.6749488,'中心校区化学相关教学科研建筑',15,1,'2026-08-01 00:00:00','2026-08-01 00:00:00');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (16,1,'生命科学楼','BUILDING','中心校区生命科学楼',117.0631224,36.6731298,'中心校区生命科学相关教学科研建筑',16,1,'2026-08-01 00:00:00','2026-08-01 00:00:00');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (17,1,'中心校区图书馆','LIBRARY','中心校区图书馆',117.0540762,36.6737910,'山东大学中心校区主要图书馆',17,1,'2026-08-01 00:00:00','2026-08-27 19:31:08');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (18,1,'蒋震图书馆','LIBRARY','知新楼D座蒋震图书馆',117.0616822,36.6764084,'位于知新楼D座的图书馆',18,1,'2026-08-01 00:00:00','2026-08-01 00:00:00');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (19,1,'齐园','CANTEEN','中心校区齐园餐厅',117.0589407,36.6767593,'中心校区主要学生食堂',19,1,'2026-08-01 00:00:00','2026-08-01 00:00:00');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (20,1,'中心校区体育馆','SPORTS','中心校区体育馆',117.0603302,36.6781079,'中心校区室内体育活动场馆',20,1,'2026-08-01 00:00:00','2026-08-01 00:00:00');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (21,1,'中心校区体育场','SPORTS','中心校区室外体育场',117.0589501,36.6781426,'中心校区田径、足球及日常运动场地',21,1,'2026-08-01 00:00:00','2026-08-01 00:00:00');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (22,1,'风雨操场','SPORTS','中心校区风雨操场',117.0589125,36.6787664,'中心校区体育活动场地',22,1,'2026-08-01 00:00:00','2026-08-01 00:00:00');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (23,1,'篮球场','SPORTS','中心校区篮球场',117.0590252,36.6775531,'中心校区室外篮球运动场地',23,1,'2026-08-01 00:00:00','2026-08-01 00:00:00');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (24,1,'网球场','SPORTS','中心校区网球场',117.0614740,36.6779773,'中心校区室外网球运动场地',24,1,'2026-08-01 00:00:00','2026-08-01 00:00:00');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (25,1,'1号学生公寓','DORMITORY','中心校区1号学生公寓',117.0586724,36.6755850,'中心校区学生宿舍',25,1,'2026-08-01 00:00:00','2026-08-01 00:00:00');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (26,1,'2号学生公寓','DORMITORY','中心校区2号学生公寓',117.0587362,36.6759483,'中心校区学生宿舍',26,1,'2026-08-01 00:00:00','2026-08-01 00:00:00');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (27,1,'3号学生公寓','DORMITORY','中心校区3号学生公寓',117.0578551,36.6759451,'中心校区学生宿舍',27,1,'2026-08-01 00:00:00','2026-08-01 00:00:00');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (28,1,'4号学生公寓','DORMITORY','中心校区4号学生公寓',117.0579138,36.6755906,'中心校区学生宿舍',28,1,'2026-08-01 00:00:00','2026-08-01 00:00:00');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (29,1,'5号学生公寓','DORMITORY','中心校区5号学生公寓',117.0568943,36.6755725,'中心校区学生宿舍',29,1,'2026-08-01 00:00:00','2026-08-01 00:00:00');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (30,1,'6号学生公寓','DORMITORY','中心校区6号学生公寓',117.0568401,36.6760175,'中心校区学生宿舍',30,1,'2026-08-01 00:00:00','2026-08-01 00:00:00');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (31,1,'7号学生公寓','DORMITORY','中心校区7号学生公寓',117.0569001,36.6764913,'中心校区学生宿舍',31,1,'2026-08-01 00:00:00','2026-08-01 00:00:00');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (32,1,'8号学生公寓','DORMITORY','中心校区8号学生公寓',117.0569993,36.6768639,'中心校区学生宿舍',32,1,'2026-08-01 00:00:00','2026-08-01 00:00:00');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (33,1,'9号学生公寓','DORMITORY','中心校区9号学生公寓',117.0568314,36.6773123,'中心校区学生宿舍',33,1,'2026-08-01 00:00:00','2026-08-01 00:00:00');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (34,1,'10号学生公寓','DORMITORY','中心校区10号学生公寓',117.0569081,36.6777284,'中心校区学生宿舍',34,1,'2026-08-01 00:00:00','2026-08-01 00:00:00');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (35,1,'11号学生公寓','DORMITORY','中心校区11号学生公寓',117.0643790,36.6757508,'中心校区学生宿舍',35,1,'2026-08-01 00:00:00','2026-08-01 00:00:00');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (36,1,'12号学生公寓','DORMITORY','中心校区12号学生公寓',117.0643521,36.6760519,'中心校区学生宿舍',36,1,'2026-08-01 00:00:00','2026-08-01 00:00:00');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (37,1,'13号学生公寓','DORMITORY','中心校区13号学生公寓',117.0643772,36.6764460,'中心校区学生宿舍',37,1,'2026-08-01 00:00:00','2026-08-01 00:00:00');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (38,1,'14号学生公寓','DORMITORY','中心校区14号学生公寓',117.0630629,36.6770913,'中心校区学生宿舍',38,1,'2026-08-01 00:00:00','2026-08-01 00:00:00');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (39,1,'15号学生公寓','DORMITORY','中心校区15号学生公寓',117.0635410,36.6773803,'中心校区学生宿舍',39,1,'2026-08-01 00:00:00','2026-08-01 00:00:00');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (40,1,'16号学生公寓','DORMITORY','中心校区16号学生公寓',117.0634397,36.6783011,'中心校区学生宿舍',40,1,'2026-08-01 00:00:00','2026-08-01 00:00:00');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (41,1,'17号学生公寓','DORMITORY','中心校区17号学生公寓',117.0650329,36.6783436,'中心校区学生宿舍',41,1,'2026-08-01 00:00:00','2026-08-01 00:00:00');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (42,1,'18号学生公寓','DORMITORY','中心校区18号学生公寓',117.0648499,36.6732994,'中心校区学生宿舍',42,1,'2026-08-01 00:00:00','2026-08-01 00:00:00');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (43,1,'中心校区校医院','OTHER','中心校区北门附近校医院',117.0571806,36.6796322,'为中心校区师生提供医疗及健康服务',43,1,'2026-08-01 00:00:00','2026-08-01 00:00:00');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (44,1,'校园卡服务大厅','OTHER','中心校区明德楼B座师生服务大厅',117.0578676,36.6768202,'提供校园卡相关线下服务',44,0,'2026-08-01 00:00:00','2026-08-27 19:23:52');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (45,1,'一站式学生社区服务中心','OTHER','中心校区学生生活区',117.0590606,36.6768151,'为学生提供综合事务及社区服务',45,1,'2026-08-01 00:00:00','2026-08-01 00:00:00');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (46,2,'一号食堂','LANDMARK',NULL,117.1344805,36.6659744,NULL,0,1,'2026-08-27 19:38:45','2026-08-27 19:38:45');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (47,2,'二号食堂','LANDMARK',NULL,117.1349831,36.6657549,NULL,0,1,'2026-08-27 19:38:58','2026-08-27 19:38:58');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (48,2,'行政办公楼','LANDMARK',NULL,117.1334335,36.6658754,NULL,0,1,'2026-08-27 19:39:15','2026-08-27 19:39:15');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (49,2,'六区','LANDMARK',NULL,117.1333472,36.6670712,NULL,0,1,'2026-08-27 19:39:37','2026-08-27 19:39:37');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (50,2,'实验楼','LANDMARK',NULL,117.1317166,36.6673984,NULL,0,1,'2026-08-27 19:39:49','2026-08-27 19:39:49');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (51,2,'一号公寓','LANDMARK',NULL,117.1349563,36.6666454,NULL,0,1,'2026-08-27 19:40:07','2026-08-27 19:40:07');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (52,2,'校医','LANDMARK',NULL,117.1352299,36.6681133,NULL,0,1,'2026-08-27 19:40:34','2026-08-27 19:40:34');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (53,2,'软件园图书馆','LANDMARK',NULL,117.1313901,36.6685393,NULL,0,1,'2026-08-27 19:40:58','2026-08-27 19:40:58');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (54,3,'外国语学院','LANDMARK',NULL,117.0617005,36.6863376,NULL,0,1,'2026-08-27 19:41:20','2026-08-27 19:41:20');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (55,3,'洪楼图书馆','LANDMARK',NULL,117.0627137,36.6858859,NULL,0,1,'2026-08-27 19:41:32','2026-08-27 19:41:32');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (56,3,'洪楼教堂','LANDMARK',NULL,117.0600217,36.6857092,NULL,0,1,'2026-08-27 19:41:54','2026-08-27 19:41:54');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (57,3,'篮球场','LANDMARK',NULL,117.0595223,36.6870766,NULL,0,1,'2026-08-27 19:42:16','2026-08-27 19:42:16');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (58,3,'田径场','LANDMARK',NULL,117.0602464,36.6875945,NULL,0,1,'2026-08-27 19:42:27','2026-08-27 19:42:27');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (59,3,'7号食堂','LANDMARK',NULL,117.0639475,36.6884968,NULL,0,1,'2026-08-27 19:42:45','2026-08-27 19:42:45');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (60,4,'北校主楼','LANDMARK',NULL,117.0228213,36.6509887,NULL,0,1,'2026-08-27 19:45:36','2026-08-27 19:45:36');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (61,4,'北校金属成形实验室','LANDMARK',NULL,117.0250331,36.6490122,NULL,0,1,'2026-08-27 19:46:20','2026-08-27 19:46:20');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (62,4,'北校大学生活动中心','LANDMARK',NULL,117.0242240,36.6511671,NULL,0,1,'2026-08-27 19:47:09','2026-08-27 19:47:09');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (63,4,'南校图书馆','LANDMARK',NULL,117.0231616,36.6462328,NULL,0,1,'2026-08-27 19:47:36','2026-08-27 19:47:36');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (64,4,'游泳馆','LANDMARK',NULL,117.0242403,36.6471292,NULL,0,1,'2026-08-27 19:47:57','2026-08-27 19:47:57');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (65,4,'田径场','LANDMARK',NULL,117.0216134,36.6463556,NULL,0,1,'2026-08-27 19:48:13','2026-08-27 19:48:13');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (66,5,'田径场','LANDMARK',NULL,117.0127289,36.6503953,NULL,0,1,'2026-08-27 19:48:51','2026-08-27 19:48:51');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (67,5,'北门','LANDMARK',NULL,117.0107062,36.6546499,NULL,0,1,'2026-08-27 19:49:21','2026-08-27 19:49:21');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (68,5,'行政楼','LANDMARK',NULL,117.0117786,36.6540111,NULL,0,1,'2026-08-27 19:49:40','2026-08-27 19:49:40');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (69,5,'梦迪音乐厅','LANDMARK',NULL,117.0148389,36.6524617,NULL,0,1,'2026-08-27 19:49:57','2026-08-27 19:49:57');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (70,5,'中心花园喷泉','LANDMARK',NULL,117.0119072,36.6529017,NULL,0,1,'2026-08-27 19:50:24','2026-08-27 19:50:24');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (71,5,'山医小区','LANDMARK',NULL,117.0100467,36.6504249,NULL,0,1,'2026-08-27 19:51:00','2026-08-27 19:51:00');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (72,6,'图书馆','LANDMARK',NULL,117.0457211,36.6006289,NULL,0,1,'2026-08-27 19:51:17','2026-08-27 19:55:18');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (73,6,'教学实验楼','LANDMARK',NULL,117.0455743,36.5987279,NULL,0,1,'2026-08-27 19:51:31','2026-08-27 19:51:31');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (74,6,'食堂欣园','LANDMARK',NULL,117.0473821,36.5985643,NULL,0,1,'2026-08-27 19:52:18','2026-08-27 19:52:18');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (75,6,'小树林','LANDMARK',NULL,117.0468403,36.5991198,NULL,0,1,'2026-08-27 19:52:25','2026-08-27 19:52:25');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (76,6,'天工湖','LANDMARK',NULL,117.0425532,36.6016194,NULL,0,1,'2026-08-27 19:52:39','2026-08-27 19:52:39');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (77,6,'八角山','LANDMARK',NULL,117.0494610,36.6009889,NULL,0,1,'2026-08-27 19:52:50','2026-08-27 19:52:50');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (78,6,'一多广场','LANDMARK',NULL,117.0450276,36.5995296,NULL,0,1,'2026-08-27 19:53:10','2026-08-27 19:53:10');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (79,6,'讲学堂','LANDMARK',NULL,117.0440329,36.5994030,NULL,0,1,'2026-08-27 19:53:28','2026-08-27 19:53:28');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (80,6,'工程训练中心','LANDMARK',NULL,117.0482140,36.6046684,NULL,0,1,'2026-08-27 19:53:51','2026-08-27 19:53:51');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (81,6,'经济工程实验室','LANDMARK',NULL,117.0500319,36.6053136,NULL,0,1,'2026-08-27 19:54:26','2026-08-27 19:54:26');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (82,6,'风雨操场','LANDMARK',NULL,117.0446268,36.5972635,NULL,0,1,'2026-08-27 19:54:45','2026-08-27 19:54:45');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (83,6,'体育馆','LANDMARK',NULL,117.0457354,36.5973585,NULL,0,1,'2026-08-27 19:55:08','2026-08-27 19:55:08');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (84,7,'计算机科学与技术学院','LANDMARK',NULL,120.6863659,36.3631421,NULL,0,1,'2026-08-27 19:57:56','2026-08-27 19:57:56');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (85,7,'格物路','LANDMARK',NULL,120.6883919,36.3635213,NULL,0,1,'2026-08-27 19:58:36','2026-08-27 19:58:36');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (86,7,'振声苑','LANDMARK',NULL,120.6818650,36.3626025,NULL,0,1,'2026-08-27 19:59:02','2026-08-27 19:59:02');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (87,7,'博物馆','LANDMARK',NULL,120.6828371,36.3608557,NULL,0,1,'2026-08-27 19:59:12','2026-08-27 19:59:12');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (88,7,'校医院','LANDMARK',NULL,120.6851631,36.3586546,NULL,0,1,'2026-08-27 19:59:25','2026-08-27 19:59:25');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (89,7,'风雨操场','LANDMARK',NULL,120.6787717,36.3615519,NULL,0,1,'2026-08-27 19:59:39','2026-08-27 19:59:39');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (90,8,'文心湖','LANDMARK',NULL,122.0544913,37.5258202,NULL,0,1,'2026-08-27 20:00:01','2026-08-27 20:00:01');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (91,8,'图书馆','LANDMARK',NULL,122.0551578,37.5309872,NULL,0,1,'2026-08-27 20:00:12','2026-08-27 20:00:12');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (92,8,'法学院','LANDMARK',NULL,122.0543262,37.5303308,NULL,0,1,'2026-08-27 20:00:23','2026-08-27 20:00:23');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (93,8,'数学与统计学院','LANDMARK',NULL,122.0560428,37.5303478,NULL,0,1,'2026-08-27 20:00:42','2026-08-27 20:00:42');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (94,8,'艺术学院','LANDMARK',NULL,122.0579469,37.5293579,NULL,0,1,'2026-08-27 20:00:57','2026-08-27 20:00:57');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (95,8,'校医院','LANDMARK',NULL,122.0597976,37.5288763,NULL,0,1,'2026-08-27 20:01:10','2026-08-27 20:01:10');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (96,8,'利群超市','LANDMARK',NULL,122.0601721,37.5268299,NULL,0,1,'2026-08-27 20:02:08','2026-08-27 20:02:08');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (97,8,'雀园餐厅','LANDMARK',NULL,122.0525291,37.5300756,NULL,0,1,'2026-08-27 20:02:47','2026-08-27 20:02:47');
INSERT INTO `location` (`id`, `campus_id`, `name`, `category_code`, `address`, `longitude`, `latitude`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES (98,2,'篮球场','SPORTS','软件园校区篮球场',117.1406902,36.6664185,'山东大学软件园校区的篮球场',0,1,'2026-08-28 15:36:13','2026-08-28 15:37:21');
/*!40000 ALTER TABLE `location` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `media`
--

LOCK TABLES `media` WRITE;
/*!40000 ALTER TABLE `media` DISABLE KEYS */;
/*!40000 ALTER TABLE `media` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `media_download`
--

LOCK TABLES `media_download` WRITE;
/*!40000 ALTER TABLE `media_download` DISABLE KEYS */;
/*!40000 ALTER TABLE `media_download` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `media_favorite`
--

LOCK TABLES `media_favorite` WRITE;
/*!40000 ALTER TABLE `media_favorite` DISABLE KEYS */;
/*!40000 ALTER TABLE `media_favorite` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `media_like`
--

LOCK TABLES `media_like` WRITE;
/*!40000 ALTER TABLE `media_like` DISABLE KEYS */;
/*!40000 ALTER TABLE `media_like` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `submission`
--

LOCK TABLES `submission` WRITE;
/*!40000 ALTER TABLE `submission` DISABLE KEYS */;
/*!40000 ALTER TABLE `submission` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `submission_asset`
--

LOCK TABLES `submission_asset` WRITE;
/*!40000 ALTER TABLE `submission_asset` DISABLE KEYS */;
/*!40000 ALTER TABLE `submission_asset` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `submission_review_setting`
--

LOCK TABLES `submission_review_setting` WRITE;
/*!40000 ALTER TABLE `submission_review_setting` DISABLE KEYS */;
INSERT INTO `submission_review_setting` (`id`, `review_enabled`, `updated_by`, `updated_at`) VALUES (1,0,2,'2026-08-27 20:55:54');
/*!40000 ALTER TABLE `submission_review_setting` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `tag`
--

LOCK TABLES `tag` WRITE;
/*!40000 ALTER TABLE `tag` DISABLE KEYS */;
/*!40000 ALTER TABLE `tag` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `time_comparison`
--

LOCK TABLES `time_comparison` WRITE;
/*!40000 ALTER TABLE `time_comparison` DISABLE KEYS */;
/*!40000 ALTER TABLE `time_comparison` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `time_comparison_item`
--

LOCK TABLES `time_comparison_item` WRITE;
/*!40000 ALTER TABLE `time_comparison_item` DISABLE KEYS */;
/*!40000 ALTER TABLE `time_comparison_item` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `topic`
--

LOCK TABLES `topic` WRITE;
/*!40000 ALTER TABLE `topic` DISABLE KEYS */;
/*!40000 ALTER TABLE `topic` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `topic_media`
--

LOCK TABLES `topic_media` WRITE;
/*!40000 ALTER TABLE `topic_media` DISABLE KEYS */;
/*!40000 ALTER TABLE `topic_media` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `user`
--

LOCK TABLES `user` WRITE;
/*!40000 ALTER TABLE `user` DISABLE KEYS */;
INSERT INTO `user` (`id`, `phone`, `password_hash`, `cas_id`, `name`, `nickname`, `avatar_key`, `bio`, `role`, `status`, `frozen_until`, `frozen_reason`, `allow_upload`, `allow_download`, `last_login_at`, `created_at`, `updated_at`, `deleted`, `token_version`) VALUES (1,NULL,NULL,'202500550476','张源朔','狐狸','avatars/1/20260824/439dcc95-26a8-4a6e-8b82-d8d871d4ee68.png','秋叶不寒霜飞晚，留得枯荷听雨声',1,1,NULL,NULL,1,1,'2026-08-25 18:58:16','2026-08-24 20:39:44','2026-08-26 21:37:25',0,0);
INSERT INTO `user` (`id`, `phone`, `password_hash`, `cas_id`, `name`, `nickname`, `avatar_key`, `bio`, `role`, `status`, `frozen_until`, `frozen_reason`, `allow_upload`, `allow_download`, `last_login_at`, `created_at`, `updated_at`, `deleted`, `token_version`) VALUES (2,NULL,NULL,'202400300355','陈子瑞','今天加两蛋','avatars/2/20260826/6662c796-669b-4148-af59-851a6a65ea7b.jpg','这个人很懒',1,1,NULL,NULL,1,1,'2026-08-25 17:06:19','2026-08-25 16:28:37','2026-08-27 21:37:20',0,13);
INSERT INTO `user` (`id`, `phone`, `password_hash`, `cas_id`, `name`, `nickname`, `avatar_key`, `bio`, `role`, `status`, `frozen_until`, `frozen_reason`, `allow_upload`, `allow_download`, `last_login_at`, `created_at`, `updated_at`, `deleted`, `token_version`) VALUES (3,NULL,NULL,'202500550254','申珐成','用户202500550254','avatars/default.png','记录校园里的建筑、街巷与故事',1,1,NULL,NULL,1,1,NULL,'2026-08-26 13:12:22','2026-08-27 19:37:18',0,3);
/*!40000 ALTER TABLE `user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `user_admin_operation_log`
--

LOCK TABLES `user_admin_operation_log` WRITE;
/*!40000 ALTER TABLE `user_admin_operation_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `user_admin_operation_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `user_browse_history`
--

LOCK TABLES `user_browse_history` WRITE;
/*!40000 ALTER TABLE `user_browse_history` DISABLE KEYS */;
/*!40000 ALTER TABLE `user_browse_history` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-08-28 16:02:47


















