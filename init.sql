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
    campus_id     bigint unsigned                    not null comment '所属校区ID',
    name          varchar(100)                       not null comment '地点名称',
    category_code varchar(32)                        null comment '地点分类编码，如 LIBRARY、CANTEEN',
    address       varchar(255)                       null comment '详细地址或校内位置描述',
    longitude     decimal(10, 7)                     not null comment '地点经度',
    latitude      decimal(10, 7)                     not null comment '地点纬度',
    cover_url     varchar(500)                       null comment '地点封面图URL',
    description   text                               null comment '地点介绍',
    sort_order    int      default 0                 not null comment '地点排序值',
    status        tinyint  default 1                 not null comment '状态：0停用，1启用',
    created_at    datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updated_at    datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
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
    status        tinyint unsigned default '0'               not null comment '0待审 1通过 2退回 3撤回',
    review_reason varchar(1000)                              null comment '退回原因',
    submitted_at  datetime                                   not null comment '最近提交审核时间',
    reviewed_by   bigint unsigned                            null comment '审核管理员ID',
    reviewed_at   datetime                                   null comment '审核时间',
    created_at    datetime         default CURRENT_TIMESTAMP not null,
    updated_at    datetime         default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    deleted       tinyint(1)       default 0                 not null,
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

