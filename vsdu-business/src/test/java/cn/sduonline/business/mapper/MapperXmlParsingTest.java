package cn.sduonline.business.mapper;

import cn.sduonline.business.data.dto.AdminUpdateLocationRequest;
import cn.sduonline.business.data.dto.AdminUpdateTopicRequest;
import cn.sduonline.business.data.dto.AdminUpdateUserPermissionRequest;
import cn.sduonline.business.data.dto.SearchMediaQueryDTO;
import cn.sduonline.business.data.enums.SubmissionStatus;
import cn.sduonline.business.data.enums.ReportStatus;
import cn.sduonline.business.data.po.Location;
import cn.sduonline.business.data.po.Submission;
import cn.sduonline.business.data.po.Topic;
import cn.sduonline.business.data.projection.MediaTagPatch;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MapperXmlParsingTest {

    @Test
    void allMapperXmlFilesShouldParse() throws Exception {
        assertThat(parseMappers().getMappedStatementNames()).isNotEmpty();
    }

    @Test
    void dynamicMapperStatementsShouldBuildSql() throws Exception {
        MybatisConfiguration configuration = parseMappers();

        Location location = new Location();
        location.setCoverKey(null);
        Map<String, Object> locationParams = new HashMap<>();
        locationParams.put("id", 1L);
        locationParams.put("request", new AdminUpdateLocationRequest(
                null, null, null, null, null, null, "", null, null, null
        ));
        locationParams.put("value", location);
        locationParams.put("updatedAt", LocalDateTime.now());

        Topic topic = new Topic();
        topic.setCoverUrl(null);
        Map<String, Object> topicParams = new HashMap<>();
        topicParams.put("id", 1L);
        topicParams.put("request", new AdminUpdateTopicRequest(
                null, null, null, "", null, null, null
        ));
        topicParams.put("value", topic);
        topicParams.put("updatedAt", LocalDateTime.now());

        assertSqlContains(configuration, "cn.sduonline.business.mapper.LocationMapper.updatePartial",
                locationParams, "cover_key = ?");
        assertSqlContains(configuration, "cn.sduonline.business.mapper.TopicMapper.updatePartial",
                topicParams, "cover_url = ?");
        assertSqlContains(configuration, "cn.sduonline.business.mapper.UserMapper.updatePermissionsPartial",
                Map.of(
                        "userId", 1L,
                        "request", new AdminUpdateUserPermissionRequest(true, null),
                        "updatedAt", LocalDateTime.now()
                ), "allow_upload = ?");
        assertSqlContains(configuration, "cn.sduonline.business.mapper.MediaMapper.batchUpdateTags",
                Map.of(
                        "patches", List.of(new MediaTagPatch(1L, "校园|建筑")),
                        "updatedAt", LocalDateTime.now()
                ), "CASE id");

        SearchMediaQueryDTO query = new SearchMediaQueryDTO();
        query.setQ("校园");
        assertSqlContains(configuration, "cn.sduonline.business.mapper.MediaSearchMapper.searchMedia",
                Map.of("query", query, "sort", "relevance", "offset", 0L, "limit", 20L),
                "ORDER BY");
        assertSqlContains(configuration, "cn.sduonline.business.mapper.TagMapper.selectUsedTagSuggestions",
                Map.of("keyword", "校园", "limit", 20), "ROW_NUMBER()");
        assertSqlContains(configuration, "cn.sduonline.business.mapper.SubmissionMapper.updateReviewWithVersion",
                Map.of(
                        "submissionId", 1L,
                        "expectedVersion", 2,
                        "beforeStatus", SubmissionStatus.PENDING.getValue(),
                        "afterStatus", SubmissionStatus.APPROVED.getValue(),
                        "reviewReason", "审核通过",
                        "reviewedBy", 7L,
                        "reviewedAt", LocalDateTime.now()
                ), "version = version + 1");
        assertSqlContains(configuration, "cn.sduonline.business.mapper.SubmissionMapper.selectAdminSubmissionPage",
                Map.of(
                        "status", SubmissionStatus.PENDING.getValue(),
                        "keyword", "校园",
                        "userId", 1L,
                        "locationId", 2L,
                        "submittedFrom", LocalDateTime.now().minusDays(7),
                        "submittedTo", LocalDateTime.now(),
                        "sort", "newest",
                        "offset", 0L,
                        "size", 20L
                ), "ORDER BY s.submitted_at DESC");
        assertSqlContains(configuration, "cn.sduonline.business.mapper.SubmissionReviewLogMapper.selectPageBySubmission",
                Map.of("submissionId", 1L, "offset", 0L, "size", 20L),
                "ORDER BY log.round_no DESC");
        assertSqlContains(configuration, "cn.sduonline.business.mapper.ReportMapper.updateDecisionWithVersion",
                Map.of(
                        "reportId", 1L,
                        "expectedVersion", 2,
                        "afterStatus", ReportStatus.CONFIRMED.getValue(),
                        "decisionReason", "举报成立",
                        "processedBy", 7L,
                        "processedAt", LocalDateTime.now()
                ), "version = version + 1");
        assertSqlContains(configuration, "cn.sduonline.business.mapper.ReportMapper.selectAdminReportPage",
                Map.of(
                        "status", ReportStatus.PENDING.getValue(),
                        "targetType", "MEDIA",
                        "reasonCode", "COPYRIGHT",
                        "reporterId", 2L,
                        "createdFrom", LocalDateTime.now().minusDays(7),
                        "createdTo", LocalDateTime.now(),
                        "offset", 0L,
                        "size", 20L
                ), "ORDER BY r.created_at DESC");
        assertSqlContains(configuration, "cn.sduonline.business.mapper.MediaMapper.selectByIdForUpdate",
                Map.of("mediaId", 1L), "FOR UPDATE");

        Submission submission = Submission.builder()
                .id(1L)
                .userId(2L)
                .locationId(3L)
                .description("修改后的描述")
                .status(SubmissionStatus.PENDING)
                .version(4)
                .updatedAt(LocalDateTime.now())
                .build();
        assertSqlContains(configuration, "cn.sduonline.business.mapper.SubmissionMapper.updateEditableWithVersion",
                Map.of(
                        "submission", submission,
                        "expectedStatus", SubmissionStatus.PENDING.getValue(),
                        "expectedVersion", 4
                ), "version = version + 1");
        assertSqlContains(configuration, "cn.sduonline.business.mapper.SubmissionMapper.resubmitWithVersion",
                Map.of(
                        "submissionId", 1L,
                        "userId", 2L,
                        "expectedVersion", 4,
                        "afterStatus", SubmissionStatus.PENDING.getValue(),
                        "submittedAt", LocalDateTime.now(),
                        "reviewedAt", LocalDateTime.now()
                ), "AND status = 2");
        assertSqlContains(configuration, "cn.sduonline.business.mapper.SubmissionMapper.withdrawWithVersion",
                Map.of(
                        "submissionId", 1L,
                        "userId", 2L,
                        "expectedVersion", 4,
                        "updatedAt", LocalDateTime.now()
                ), "AND status = 0");
    }

    private MybatisConfiguration parseMappers() throws Exception {
        Resource[] resources = new PathMatchingResourcePatternResolver()
                .getResources("classpath*:mapper/*.xml");
        assertThat(resources).isNotEmpty();

        MybatisConfiguration configuration = new MybatisConfiguration();
        Arrays.sort(resources, java.util.Comparator.comparing(Resource::getDescription));
        for (Resource resource : resources) {
            try (InputStream input = resource.getInputStream()) {
                new XMLMapperBuilder(
                        input,
                        configuration,
                        resource.getURL().toExternalForm(),
                        configuration.getSqlFragments()
                ).parse();
            }
        }
        return configuration;
    }

    private void assertSqlContains(
            MybatisConfiguration configuration,
            String statementId,
            Object parameters,
            String expectedSql
    ) {
        String sql = configuration.getMappedStatement(statementId)
                .getBoundSql(parameters)
                .getSql();
        assertThat(sql).contains(expectedSql);
    }
}
