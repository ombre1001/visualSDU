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
    constraint chk_announcement_iQ_pinned
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
    cover_key   varchar(512)                       null comment '城市封面图R2 ObjectKey',
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

create table location_category
(
    id         bigint unsigned auto_increment comment '地点分类ID'
        primary key,
    code       varchar(32)                        not null comment '稳定业务编码，如 BUILDING',
    name       varchar(50)                        not null comment '分类展示名称',
    sort_order int      default 0                 not null comment '排序值，越小越靠前',
    status     tinyint  default 1                 not null comment '状态：0停用，1启用',
    created_at datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updated_at datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint uk_location_category_code
        unique (code),
    constraint uk_location_category_name
        unique (name),
    constraint ck_location_category_status
        check (`status` in (0, 1))
)
    comment '地点分类表' collate = utf8mb4_unicode_ci;

create index idx_location_category_status_sort
    on location_category (status, sort_order, id);

create table location
(
    id            bigint unsigned auto_increment comment '地点ID'
        primary key,
    campus_id     bigint unsigned                            not null comment '所属校区ID',
    name          varchar(100)                               not null comment '地点名称',
    category_code varchar(32)                                not null comment '地点分类编码，如 LIBRARY、CANTEEN',
    address       varchar(255)                               null comment '详细地址或校内位置描述',
    longitude     decimal(10, 7)                             not null comment '地点经度',
    latitude      decimal(10, 7)                             not null comment '地点纬度',
    cover_key     varchar(512) default 'avatars/default.png' null comment '地点封面图R2 ObjectKey',
    description   text                                       null comment '地点介绍',
    sort_order    int          default 0                     not null comment '地点排序值',
    status        tinyint      default 1                     not null comment '状态：0停用，1启用',
    created_at    datetime     default CURRENT_TIMESTAMP     not null comment '创建时间',
    updated_at    datetime     default CURRENT_TIMESTAMP     not null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint fk_location_campus
        foreign key (campus_id) references campus (id)
            on update cascade,
    constraint fk_location_category
        foreign key (category_code) references location_category (code)
            on update restrict
            on delete restrict,
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

create table report_reason_type
(
    code        varchar(32)                            not null comment '稳定的理由编码'
        primary key,
    name        varchar(50)                            not null comment '展示名称',
    description varchar(255)                           null comment '理由说明',
    enabled     tinyint(1)   default 1                 not null comment '0停用 1启用',
    sort_order  int unsigned default 0                 not null comment '展示顺序，越小越靠前',
    created_at  datetime(3)  default CURRENT_TIMESTAMP(3) not null,
    updated_at  datetime(3)  default CURRENT_TIMESTAMP(3) not null
        on update CURRENT_TIMESTAMP(3),
    constraint ck_report_reason_enabled
        check (enabled in (0, 1))
)
    comment '举报理由字典'
    collate = utf8mb4_unicode_ci;

create index idx_report_reason_enabled_sort
    on report_reason_type (enabled, sort_order, code);


create table report
(
    id              bigint unsigned auto_increment comment '举报ID'
        primary key,
    reporter_id     bigint unsigned                           not null comment '举报用户ID',
    target_type     varchar(32)                               not null comment '目标类型，V1支持MEDIA、USER',
    target_id       bigint unsigned                           not null comment '目标对象ID',
    reason_code     varchar(32)                               not null comment '举报理由编码',
    description     varchar(1000)                             null comment '举报补充说明',

    status          tinyint unsigned default 0                not null
        comment '状态：0待处理 1处理中 2举报成立 3举报不成立 4已关闭',
    decision_reason varchar(1000)                             null comment '最终处理理由',
    processed_by    bigint unsigned                           null comment '最终处理管理员ID',
    processed_at    datetime(3)                               null comment '最终处理时间',

    version         int unsigned default 0                    not null comment '乐观锁版本',
    submit_ip       varchar(45)                               null comment '提交IP，兼容IPv4和IPv6',
    created_at      datetime(3) default CURRENT_TIMESTAMP(3)  not null,
    updated_at      datetime(3) default CURRENT_TIMESTAMP(3)  not null
        on update CURRENT_TIMESTAMP(3),

    active_marker   tinyint generated always as
        (
        case
            when status in (0, 1) then 1
            end
        ) stored comment '进行中举报防重标记',

    constraint fk_report_reporter
        foreign key (reporter_id) references user (id),
    constraint fk_report_reason
        foreign key (reason_code) references report_reason_type (code),
    constraint fk_report_processor
        foreign key (processed_by) references user (id),

    constraint uk_report_active_duplicate
        unique (reporter_id, target_type, target_id, active_marker),

    constraint ck_report_status
        check (status in (0, 1, 2, 3, 4)),
    constraint ck_report_target_type_not_blank
        check (char_length(trim(target_type)) > 0)
)
    comment '内容举报'
    collate = utf8mb4_unicode_ci;

create index idx_report_admin_list
    on report (status, created_at, id);

create index idx_report_target_status
    on report (target_type, target_id, status, created_at);

create index idx_report_reporter_time
    on report (reporter_id, created_at, id);

create index idx_report_reason_status
    on report (reason_code, status, created_at, id);

create index idx_report_processor_time
    on report (processed_by, processed_at, id);

create table report_operation_log
(
    id              bigint unsigned auto_increment comment '举报操作日志ID'
        primary key,
    report_id       bigint unsigned                           not null comment '举报ID',
    operator_id     bigint unsigned                           not null comment '操作管理员ID',
    operation_type  varchar(32)                               not null
        comment '操作类型：START_PROCESSING/DECISION',

    decision        tinyint unsigned                          null
        comment '处理决定：1成立 2不成立 3关闭',
    before_status   tinyint unsigned                          not null comment '操作前状态',
    after_status    tinyint unsigned                          not null comment '操作后状态',
    reason          varchar(1000)                             null comment '操作或决定理由',

    actions_json    json                                      null
        comment '处置动作及参数，如HIDE_MEDIA、FREEZE_USER',
    result_json     json                                      null comment '处置动作执行结果',

    report_version  int unsigned                              not null comment '操作前举报版本',
    created_at      datetime(3) default CURRENT_TIMESTAMP(3)  not null comment '操作时间',

    constraint fk_report_operation_report
        foreign key (report_id) references report (id),
    constraint fk_report_operation_operator
        foreign key (operator_id) references user (id),

    constraint uk_report_operation_version
        unique (report_id, report_version),

    constraint ck_report_operation_decision
        check (decision is null or decision in (1, 2, 3)),
    constraint ck_report_operation_before_status
        check (before_status in (0, 1, 2, 3, 4)),
    constraint ck_report_operation_after_status
        check (after_status in (0, 1, 2, 3, 4))
)
    comment '举报处理操作日志'
    collate = utf8mb4_unicode_ci;

create index idx_report_operation_time
    on report_operation_log (report_id, created_at, id);

create index idx_report_operator_time
    on report_operation_log (operator_id, created_at, id);

-- 地点分类字典
insert into location_category (code, name, sort_order, status) values
    ('BUILDING', '教学及办公建筑', 10, 1),
    ('LIBRARY', '图书馆', 20, 1),
    ('CANTEEN', '食堂', 30, 1),
    ('DORMITORY', '宿舍', 40, 1),
    ('SPORTS', '体育设施', 50, 1),
    ('LANDMARK', '校园地标', 60, 1),
    ('GATE', '校门', 70, 1),
    ('SCENERY', '景观', 80, 1),
    ('OTHER', '其他', 90, 1);

-- 举报原因字典
insert into report_reason_type (code, name, description, enabled, sort_order) values
    ('COPYRIGHT', '侵犯版权', '未经授权使用他人享有著作权的内容', 1, 10),
    ('ILLEGAL_CONTENT', '违法违规', '涉嫌违反法律法规或平台规范', 1, 20),
    ('FALSE_INFORMATION', '虚假信息', '内容包含虚假或误导性信息', 1, 30),
    ('PRIVACY', '隐私泄露', '未经授权公开个人隐私信息', 1, 40),
    ('OTHER', '其他', '不属于以上分类的问题', 1, 100);

-- 城市基础数据
insert into city (id, name, code, province, cover_key, description, sort_order, status) values
    (1, '济南', 'JINAN', '山东省', NULL, '山东大学济南地区校区', 1, 1),
    (2, '青岛', 'QINGDAO', '山东省', NULL, '山东大学青岛校区', 2, 1),
    (3, '威海', 'WEIHAI', '山东省', NULL, '山东大学威海校区', 3, 1);

-- 校区基础数据
insert into campus (id, city_id, name, short_name, address, longitude, latitude, cover_url, description, sort_order, status) values
    (1, 1, '山东大学中心校区', '中心校区', '山东省济南市山大南路27号', 117.0600268, 36.6757805, NULL, '截至2025年9月，校区下设哲学与社会发展学院、经济学院、文学院、历史学院、考古学院、数学学院、物理学院、化学与化工学院、管理学院、马克思主义学院、国际教育学院、新闻传播学院、经济研究院、儒学高等研究院（文史哲研究院）、外国语学院（大学外语教学部）、晶体材料研究院、中泰证券金融研究院、新一代半导体材料研究院、人工智能学院、国家卓越工程师学院、教育高等研究院、黄河国家战略研究院、智能通信技术研究院等23个学院。', 1, 1),
    (2, 1, '山东大学软件园校区', '软件园校区', '山东省济南市舜华路1500号', 117.1384996, 36.6669768, NULL, '截至2025年9月，校区下设软件学院、集成电路学院、人工智能国际联合研究院、智能创新研究院等4个学院和1个中加合作办学项目。', 2, 1),
    (3, 1, '山东大学洪家楼校区', '洪家楼校区', '山东省济南市洪家楼5号', 117.0660701, 36.6862478, NULL, '截至2025年9月，校区下设外国语学院和艺术学院等2个学院。', 3, 1),
    (4, 1, '山东大学千佛山校区', '千佛山校区', '山东省济南市历下区经十路17923号', 117.0289869, 36.6490126, NULL, '截至2025年9月，校区下设材料科学与工程学院、机械工程学院、控制科学与工程学院、核科学与能源动力学院、电气工程学院、土建与水利学院、体育学院等7个学院。', 4, 1),
    (5, 1, '山东大学趵突泉校区', '趵突泉校区', '山东省济南市历下区文化西路44号', 117.0196517, 36.6530261, NULL, '趵突泉校区也被称为齐鲁医学院，其前身为1864年创办的山东登州文会馆，截至2025年3月，下设基础医学院、公共卫生学院、口腔医学院、护理与康复学院、药学院、第一临床学院、第二临床学院、生物医学工程学院（筹）、精神与心理健康学院（研究院）（筹）9个学院及医学融合与实践中心。', 5, 1),
    (6, 1, '山东大学兴隆山校区', '兴隆山校区', '山东省济南市市中区二环东路12550号', 117.0492186, 36.5992778, NULL, '截至2025年9月，校区下设材料科学与工程学院、机械工程学院、控制科学与工程学院、核科学与能源动力学院、电气工程学院、土建与水利学院、齐鲁交通学院、未来技术学院等8个学院。', 6, 1),
    (7, 2, '山东大学青岛校区', '青岛校区', '山东省青岛市即墨区滨海路72号', 120.6876844, 36.3615080, NULL, '截至2025年9月，校区下设政治学与公共管理学院、法学院、信息科学与工程学院、计算机科学与技术学院、生命科学学院、环境科学与工程学院、国际创新转化学院、网络空间安全学院、前沿交叉科学青岛研究院、人文艺术研究院、微生物技术研究院、环境研究院、海洋研究院、数学与交叉科学研究中心等14个学院。', 7, 1),
    (8, 3, '山东大学威海校区', '威海校区', '山东省威海市环翠区文化西路180号', 122.0602513, 37.5291383, NULL, '截至2025年9月，下设东北亚学院、翻译学院、空间科学与技术学院、商学院、艺术学院、数学与统计学院、纪检监察学院、海洋学院、文化传播学院、机电与信息工程学院、马克思主义学院、山东大学澳国立联合理学院等12学院和1个体育教学部。', 8, 1);

-- 地点基础数据
insert into location (id, campus_id, name, category_code, address, longitude, latitude, description, sort_order, status) values
    (1, 1, '中心校区北门', 'GATE', '中心校区北侧主要出入口', 117.0580373, 36.6791828, '山东大学中心校区北侧主要校门', 1, 0),
    (2, 1, '中心校区西门', 'GATE', '中心校区西侧主要出入口', 117.0563198, 36.6747525, '山东大学中心校区西侧主要校门', 2, 0),
    (3, 1, '中心校区南门', 'GATE', '中心校区南侧主要出入口', 117.0601030, 36.6721210, '山东大学中心校区南侧主要校门', 3, 1),
    (4, 1, '中心校区西南门', 'GATE', '中心校区西南侧出入口', 117.0592341, 36.6720257, '山东大学中心校区西南侧校门', 4, 1),
    (5, 1, '知新楼A座', 'BUILDING', '中心校区知新楼A座', 117.0598576, 36.6768597, '知新楼组成建筑之一，主要用于教学、科研及办公', 5, 1),
    (6, 1, '知新楼B座', 'BUILDING', '中心校区知新楼B座', 117.0608282, 36.6768611, '知新楼组成建筑之一，主要用于教学、科研及办公', 6, 1),
    (7, 1, '知新楼C座', 'BUILDING', '中心校区知新楼C座', 117.0616492, 36.6768683, '知新楼组成建筑之一，主要用于教学、科研及办公', 7, 1),
    (8, 1, '知新楼D座', 'BUILDING', '中心校区知新楼D座', 117.0610808, 36.6763799, '知新楼组成建筑之一，蒋震图书馆所在建筑', 8, 1),
    (9, 1, '邵逸夫科学馆', 'BUILDING', '中心校区邵逸夫科学馆', 117.0619800, 36.6763267, '中心校区教学、科研及公共服务建筑', 9, 1),
    (10, 1, '明德楼A座', 'BUILDING', '中心校区明德楼A座', 117.0600734, 36.6758681, '中心校区行政办公建筑', 10, 1),
    (11, 1, '明德楼B座', 'BUILDING', '中心校区明德楼B座', 117.0600734, 36.6758681, '中心校区行政及师生公共服务建筑', 11, 1),
    (12, 1, '明德楼C座', 'BUILDING', '中心校区明德楼C座', 117.0600734, 36.6758681, '中心校区行政办公及公共服务建筑', 12, 1),
    (13, 1, '公教楼', 'BUILDING', '中心校区公共教学楼', 117.0588610, 36.6732711, '中心校区主要公共教学建筑', 13, 1),
    (14, 1, '理综楼', 'BUILDING', '中心校区理综楼', 117.0571103, 36.6732063, '中心校区理工类教学科研建筑', 14, 1),
    (15, 1, '化学楼', 'BUILDING', '中心校区化学楼', 117.0601393, 36.6749488, '中心校区化学相关教学科研建筑', 15, 1),
    (16, 1, '生命科学楼', 'BUILDING', '中心校区生命科学楼', 117.0631224, 36.6731298, '中心校区生命科学相关教学科研建筑', 16, 1),
    (17, 1, '中心校区图书馆', 'LIBRARY', '中心校区图书馆', 117.0540762, 36.6737910, '山东大学中心校区主要图书馆', 17, 1),
    (18, 1, '蒋震图书馆', 'LIBRARY', '知新楼D座蒋震图书馆', 117.0616822, 36.6764084, '位于知新楼D座的图书馆', 18, 1),
    (19, 1, '齐园', 'CANTEEN', '中心校区齐园餐厅', 117.0589407, 36.6767593, '中心校区主要学生食堂', 19, 1),
    (20, 1, '中心校区体育馆', 'SPORTS', '中心校区体育馆', 117.0603302, 36.6781079, '中心校区室内体育活动场馆', 20, 1),
    (21, 1, '中心校区体育场', 'SPORTS', '中心校区室外体育场', 117.0589501, 36.6781426, '中心校区田径、足球及日常运动场地', 21, 1),
    (22, 1, '风雨操场', 'SPORTS', '中心校区风雨操场', 117.0589125, 36.6787664, '中心校区体育活动场地', 22, 1),
    (23, 1, '篮球场', 'SPORTS', '中心校区篮球场', 117.0590252, 36.6775531, '中心校区室外篮球运动场地', 23, 1),
    (24, 1, '网球场', 'SPORTS', '中心校区网球场', 117.0614740, 36.6779773, '中心校区室外网球运动场地', 24, 1),
    (25, 1, '1号学生公寓', 'DORMITORY', '中心校区1号学生公寓', 117.0586724, 36.6755850, '中心校区学生宿舍', 25, 1),
    (26, 1, '2号学生公寓', 'DORMITORY', '中心校区2号学生公寓', 117.0587362, 36.6759483, '中心校区学生宿舍', 26, 1),
    (27, 1, '3号学生公寓', 'DORMITORY', '中心校区3号学生公寓', 117.0578551, 36.6759451, '中心校区学生宿舍', 27, 1),
    (28, 1, '4号学生公寓', 'DORMITORY', '中心校区4号学生公寓', 117.0579138, 36.6755906, '中心校区学生宿舍', 28, 1),
    (29, 1, '5号学生公寓', 'DORMITORY', '中心校区5号学生公寓', 117.0568943, 36.6755725, '中心校区学生宿舍', 29, 1),
    (30, 1, '6号学生公寓', 'DORMITORY', '中心校区6号学生公寓', 117.0568401, 36.6760175, '中心校区学生宿舍', 30, 1),
    (31, 1, '7号学生公寓', 'DORMITORY', '中心校区7号学生公寓', 117.0569001, 36.6764913, '中心校区学生宿舍', 31, 1),
    (32, 1, '8号学生公寓', 'DORMITORY', '中心校区8号学生公寓', 117.0569993, 36.6768639, '中心校区学生宿舍', 32, 1),
    (33, 1, '9号学生公寓', 'DORMITORY', '中心校区9号学生公寓', 117.0568314, 36.6773123, '中心校区学生宿舍', 33, 1),
    (34, 1, '10号学生公寓', 'DORMITORY', '中心校区10号学生公寓', 117.0569081, 36.6777284, '中心校区学生宿舍', 34, 1),
    (35, 1, '11号学生公寓', 'DORMITORY', '中心校区11号学生公寓', 117.0643790, 36.6757508, '中心校区学生宿舍', 35, 1),
    (36, 1, '12号学生公寓', 'DORMITORY', '中心校区12号学生公寓', 117.0643521, 36.6760519, '中心校区学生宿舍', 36, 1),
    (37, 1, '13号学生公寓', 'DORMITORY', '中心校区13号学生公寓', 117.0643772, 36.6764460, '中心校区学生宿舍', 37, 1),
    (38, 1, '14号学生公寓', 'DORMITORY', '中心校区14号学生公寓', 117.0630629, 36.6770913, '中心校区学生宿舍', 38, 1),
    (39, 1, '15号学生公寓', 'DORMITORY', '中心校区15号学生公寓', 117.0635410, 36.6773803, '中心校区学生宿舍', 39, 1),
    (40, 1, '16号学生公寓', 'DORMITORY', '中心校区16号学生公寓', 117.0634397, 36.6783011, '中心校区学生宿舍', 40, 1),
    (41, 1, '17号学生公寓', 'DORMITORY', '中心校区17号学生公寓', 117.0650329, 36.6783436, '中心校区学生宿舍', 41, 1),
    (42, 1, '18号学生公寓', 'DORMITORY', '中心校区18号学生公寓', 117.0648499, 36.6732994, '中心校区学生宿舍', 42, 1),
    (43, 1, '中心校区校医院', 'OTHER', '中心校区北门附近校医院', 117.0571806, 36.6796322, '为中心校区师生提供医疗及健康服务', 43, 1),
    (44, 1, '校园卡服务大厅', 'OTHER', '中心校区明德楼B座师生服务大厅', 117.0578676, 36.6768202, '提供校园卡相关线下服务', 44, 0),
    (45, 1, '一站式学生社区服务中心', 'OTHER', '中心校区学生生活区', 117.0590606, 36.6768151, '为学生提供综合事务及社区服务', 45, 1),
    (46, 2, '一号食堂', 'CANTEEN', NULL, 117.1344805, 36.6659744, NULL, 0, 1),
    (47, 2, '二号食堂', 'CANTEEN', NULL, 117.1349831, 36.6657549, NULL, 0, 1),
    (48, 2, '行政办公楼', 'BUILDING', NULL, 117.1334335, 36.6658754, NULL, 0, 1),
    (49, 2, '六区', 'LANDMARK', NULL, 117.1333472, 36.6670712, NULL, 0, 1),
    (50, 2, '实验楼', 'BUILDING', NULL, 117.1317166, 36.6673984, NULL, 0, 1),
    (51, 2, '一号公寓', 'DORMITORY', NULL, 117.1349563, 36.6666454, NULL, 0, 1),
    (52, 2, '校医', 'OTHER', NULL, 117.1352299, 36.6681133, NULL, 0, 1),
    (53, 2, '软件园图书馆', 'LIBRARY', NULL, 117.1313901, 36.6685393, NULL, 0, 1),
    (54, 3, '外国语学院', 'BUILDING', NULL, 117.0617005, 36.6863376, NULL, 0, 1),
    (55, 3, '洪楼图书馆', 'LIBRARY', NULL, 117.0627137, 36.6858859, NULL, 0, 1),
    (56, 3, '洪楼教堂', 'LANDMARK', NULL, 117.0600217, 36.6857092, NULL, 0, 1),
    (57, 3, '篮球场', 'SPORTS', NULL, 117.0595223, 36.6870766, NULL, 0, 1),
    (58, 3, '田径场', 'SPORTS', NULL, 117.0602464, 36.6875945, NULL, 0, 1),
    (59, 3, '7号食堂', 'CANTEEN', NULL, 117.0639475, 36.6884968, NULL, 0, 1),
    (60, 4, '北校主楼', 'BUILDING', NULL, 117.0228213, 36.6509887, NULL, 0, 1),
    (61, 4, '北校金属成形实验室', 'BUILDING', NULL, 117.0250331, 36.6490122, NULL, 0, 1),
    (62, 4, '北校大学生活动中心', 'OTHER', NULL, 117.0242240, 36.6511671, NULL, 0, 1),
    (63, 4, '南校图书馆', 'LIBRARY', NULL, 117.0231616, 36.6462328, NULL, 0, 1),
    (64, 4, '游泳馆', 'SPORTS', NULL, 117.0242403, 36.6471292, NULL, 0, 1),
    (65, 4, '田径场', 'SPORTS', NULL, 117.0216134, 36.6463556, NULL, 0, 1),
    (66, 5, '田径场', 'SPORTS', NULL, 117.0127289, 36.6503953, NULL, 0, 1),
    (67, 5, '北门', 'GATE', NULL, 117.0107062, 36.6546499, NULL, 0, 1),
    (68, 5, '行政楼', 'BUILDING', NULL, 117.0117786, 36.6540111, NULL, 0, 1),
    (69, 5, '梦迪音乐厅', 'LANDMARK', NULL, 117.0148389, 36.6524617, NULL, 0, 1),
    (70, 5, '中心花园喷泉', 'SCENERY', NULL, 117.0119072, 36.6529017, NULL, 0, 1),
    (71, 5, '山医小区', 'DORMITORY', NULL, 117.0100467, 36.6504249, NULL, 0, 1),
    (72, 6, '图书馆', 'LIBRARY', NULL, 117.0457211, 36.6006289, NULL, 0, 1),
    (73, 6, '教学实验楼', 'BUILDING', NULL, 117.0455743, 36.5987279, NULL, 0, 1),
    (74, 6, '食堂欣园', 'CANTEEN', NULL, 117.0473821, 36.5985643, NULL, 0, 1),
    (75, 6, '小树林', 'SCENERY', NULL, 117.0468403, 36.5991198, NULL, 0, 1),
    (76, 6, '天工湖', 'SCENERY', NULL, 117.0425532, 36.6016194, NULL, 0, 1),
    (77, 6, '八角山', 'SCENERY', NULL, 117.0494610, 36.6009889, NULL, 0, 1),
    (78, 6, '一多广场', 'LANDMARK', NULL, 117.0450276, 36.5995296, NULL, 0, 1),
    (79,6,'讲学堂','BUILDING',NULL,117.0440329,36.5994030,NULL,0,1),
    (80,6,'工程训练中心','BUILDING',NULL,117.0482140,36.6046684,NULL,0,1),
    (81,6,'经济工程实验室','BUILDING',NULL,117.0500319,36.6053136,NULL,0,1),
    (82,6,'风雨操场','SPORTS',NULL,117.0446268,36.5972635,NULL,0,1),
    (83,6,'体育馆','SPORTS',NULL,117.0457354,36.5973585,NULL,0,1),
    (84,7,'计算机科学与技术学院','BUILDING',NULL,120.6863659,36.3631421,NULL,0,1),
    (85,7,'格物路','LANDMARK',NULL,120.6883919,36.3635213,NULL,0,1),
    (86,7,'振声苑','DORMITORY',NULL,120.6818650,36.3626025,NULL,0,1),
    (87,7,'博物馆','LANDMARK',NULL,120.6828371,36.3608557,NULL,0,1),
    (88,7,'校医院','OTHER',NULL,120.6851631,36.3586546,NULL,0,1),
    (89,7,'风雨操场','SPORTS',NULL,120.6787717,36.3615519,NULL,0,1),
    (90,8,'文心湖','SCENERY',NULL,122.0544913,37.5258202,NULL,0,1),
    (91,8,'图书馆','LIBRARY',NULL,122.0551578,37.5309872,NULL,0,1),
    (92,8,'法学院','BUILDING',NULL,122.0543262,37.5303308,NULL,0,1),
    (93,8,'数学与统计学院','BUILDING',NULL,122.0560428,37.5303478,NULL,0,1),
    (94,8,'艺术学院','BUILDING',NULL,122.0579469,37.5293579,NULL,0,1),
    (95,8,'校医院','OTHER',NULL,122.0597976,37.5288763,NULL,0,1),
    (96,8,'利群超市','OTHER',NULL,122.0601721,37.5268299,NULL,0,1),
    (97,8,'雀园餐厅','CANTEEN',NULL,122.0525291,37.5300756,NULL,0,1),
    (98,2,'篮球场','SPORTS','软件园校区篮球场',117.1406902,36.6664185,'山东大学软件园校区的篮球场',0,1);

-- 投稿审核默认开启
insert into submission_review_setting (id, review_enabled, updated_by) values
    (1, 1, null);
