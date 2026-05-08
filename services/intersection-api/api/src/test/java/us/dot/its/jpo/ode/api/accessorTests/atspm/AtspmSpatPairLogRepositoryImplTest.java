package us.dot.its.jpo.ode.api.accessorTests.atspm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import us.dot.its.jpo.ode.api.accessors.atspm.AtspmSpatPairLogRepositoryImpl;
import us.dot.its.jpo.ode.api.models.AggregationResult;
import us.dot.its.jpo.ode.api.models.AggregationResultCount;
import us.dot.its.jpo.ode.api.models.atspm.AtspmSpatPairLog;

@SpringBootTest
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@AutoConfigureEmbeddedDatabase
public class AtspmSpatPairLogRepositoryImplTest {

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private AggregationResults<AggregationResult> mockAggregationResult;

    @Mock
    private Page<AtspmSpatPairLog> mockPage;

    @Mock
    private MongoConverter mongoConverter;

    @InjectMocks
    private AtspmSpatPairLogRepositoryImpl repository;

    private final Integer intersectionId = 123;
    private final Long queryTime = 1724170718205L;
    private final Long startTime = 1724170658205L;
    private final Long endTime = 1724170778205L;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        repository = new AtspmSpatPairLogRepositoryImpl(mongoTemplate);
        when(mongoTemplate.getConverter()).thenReturn(mongoConverter);

        when(mongoConverter.read(eq(AtspmSpatPairLog.class), any(Document.class))).thenAnswer(invocation -> {
            Document doc = invocation.getArgument(1);

            AtspmSpatPairLog log = new AtspmSpatPairLog();
            log.setIntersectionId(doc.getInteger("intersectionId"));

            Date docStartTime = doc.getDate("startTime");
            if (docStartTime != null) {
                log.setStartTime(docStartTime.toInstant());
            }

            Date docEndTime = doc.getDate("endTime");
            if (docEndTime != null) {
                log.setEndTime(docEndTime.toInstant());
            }

            return log;
        });
    }

    @Test
    void testCount() {
        long expectedCount = 10;
        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);

        when(mongoTemplate.count(queryCaptor.capture(), Mockito.<String>any())).thenReturn(expectedCount);

        long resultCount = repository.count(intersectionId, queryTime);

        assertThat(resultCount).isEqualTo(expectedCount);
        assertThat(queryCaptor.getValue().getQueryObject().toJson()).contains("intersectionId");
        assertThat(queryCaptor.getValue().getQueryObject().toJson()).contains("startTime");
        assertThat(queryCaptor.getValue().getQueryObject().toJson()).contains("endTime");
        verify(mongoTemplate).count(any(Query.class), eq("CmAtspmSpatPairLog"));
    }

    @Test
    void testFind() {
        AtspmSpatPairLogRepositoryImpl repo = mock(AtspmSpatPairLogRepositoryImpl.class);

        when(repo.findPage(
                any(),
                any(),
                any(PageRequest.class),
                any(Criteria.class),
                any(Sort.class),
                any(),
                eq(AtspmSpatPairLog.class))).thenReturn(mockPage);
        PageRequest pageRequest = PageRequest.of(0, 1);
        doCallRealMethod().when(repo).find(intersectionId, queryTime, pageRequest);

        Page<AtspmSpatPairLog> results = repo.find(intersectionId, queryTime, pageRequest);

        assertThat(results).isEqualTo(mockPage);
    }

    @Test
    void testFindLatest() {
        AtspmSpatPairLog log = new AtspmSpatPairLog();
        log.setIntersectionId(intersectionId);
        log.setStartTime(Instant.ofEpochMilli(startTime));
        log.setEndTime(Instant.ofEpochMilli(endTime));

        doReturn(log).when(mongoTemplate).findOne(any(Query.class), eq(AtspmSpatPairLog.class), anyString());

        Page<AtspmSpatPairLog> page = repository.findLatest(intersectionId, queryTime);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().getIntersectionId()).isEqualTo(intersectionId);
        assertThat(page.getContent().getFirst().getStartTime()).isEqualTo(Instant.ofEpochMilli(startTime));
        assertThat(page.getContent().getFirst().getEndTime()).isEqualTo(Instant.ofEpochMilli(endTime));
        verify(mongoTemplate).findOne(any(Query.class), eq(AtspmSpatPairLog.class), eq("CmAtspmSpatPairLog"));
    }

    @Test
    void testFindWithData() {
        List<Document> sampleDocuments = List.of(new Document()
                .append("routeId", 10)
                .append("signalId", "signal-1")
                .append("intersectionId", intersectionId)
                .append("startTime", Date.from(Instant.ofEpochMilli(startTime)))
                .append("endTime", Date.from(Instant.ofEpochMilli(endTime)))
                .append("percentPaired", 95.0)
                .append("percentGreenPaired", 94.0)
                .append("percentRedPaired", 96.0)
                .append("percentYellowPaired", 93.0));

        AggregationResult aggregationResult = new AggregationResult();
        aggregationResult.setResults(sampleDocuments);
        AggregationResultCount aggregationResultCount = new AggregationResultCount();
        aggregationResultCount.setCount(1L);
        aggregationResult.setMetadata(List.of(aggregationResultCount));

        when(mockAggregationResult.getUniqueMappedResult()).thenReturn(aggregationResult);

        ArgumentCaptor<Aggregation> aggregationCaptor = ArgumentCaptor.forClass(Aggregation.class);
        doReturn(mockAggregationResult).when(mongoTemplate).aggregate(aggregationCaptor.capture(), anyString(),
                eq(AggregationResult.class));

        Page<AtspmSpatPairLog> response = repository.find(intersectionId, queryTime, PageRequest.of(0, 1));

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().getFirst().getIntersectionId()).isEqualTo(intersectionId);
        assertThat(response.getContent().getFirst().getStartTime()).isEqualTo(Instant.ofEpochMilli(startTime));
        assertThat(response.getContent().getFirst().getEndTime()).isEqualTo(Instant.ofEpochMilli(endTime));

        Document matchStage = aggregationCaptor.getValue().toPipeline(Aggregation.DEFAULT_CONTEXT).getFirst();
        assertThat(matchStage.toJson()).contains("intersectionId");
        assertThat(matchStage.toJson()).contains("startTime");
        assertThat(matchStage.toJson()).contains("endTime");
    }
}