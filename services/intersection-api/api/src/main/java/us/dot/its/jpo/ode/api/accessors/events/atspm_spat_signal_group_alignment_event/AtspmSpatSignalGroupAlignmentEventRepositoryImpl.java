package us.dot.its.jpo.ode.api.accessors.events.atspm_spat_signal_group_alignment_event;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.DateOperators;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

import us.dot.its.jpo.ode.api.accessors.IntersectionCriteria;
import us.dot.its.jpo.ode.api.accessors.PageableQuery;
import us.dot.its.jpo.ode.api.models.IDCount;
import us.dot.its.jpo.ode.api.models.atspm.AtspmSpatSignalGroupAlignmentEvent;

@Component
public class AtspmSpatSignalGroupAlignmentEventRepositoryImpl
        implements AtspmSpatSignalGroupAlignmentEventRepository, PageableQuery {

    private final MongoTemplate mongoTemplate;

    private final String collectionName = "CmAtspmSpatSignalGroupAlignmentEvent";
    private final String DATE_FIELD = "eventGeneratedAt";
    private final String INTERSECTION_ID_FIELD = "intersectionID";

    @Autowired
    public AtspmSpatSignalGroupAlignmentEventRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public long count(Integer intersectionID, Long startTime, Long endTime) {
        Criteria criteria = new IntersectionCriteria()
                .whereOptional(INTERSECTION_ID_FIELD, intersectionID)
                .withinUtcTimeWindow(DATE_FIELD, startTime, endTime);
        Query query = Query.query(criteria);
        return mongoTemplate.count(query, collectionName);
    }

    @Override
    public Page<AtspmSpatSignalGroupAlignmentEvent> findLatest(Integer intersectionID, Long startTime, Long endTime) {
        Criteria criteria = new IntersectionCriteria()
                .whereOptional(INTERSECTION_ID_FIELD, intersectionID)
                .withinUtcTimeWindow(DATE_FIELD, startTime, endTime);
        Query query = Query.query(criteria);
        Sort sort = Sort.by(Sort.Direction.DESC, DATE_FIELD);
        return wrapSingleResultWithPage(
                mongoTemplate.findOne(
                        query.with(sort),
                        AtspmSpatSignalGroupAlignmentEvent.class,
                        collectionName));
    }

    @Override
    public Page<AtspmSpatSignalGroupAlignmentEvent> find(Integer intersectionID, Long startTime, Long endTime,
            Pageable pageable) {
        Criteria criteria = new IntersectionCriteria()
                .whereOptional(INTERSECTION_ID_FIELD, intersectionID)
                .withinUtcTimeWindow(DATE_FIELD, startTime, endTime);
        Sort sort = Sort.by(Sort.Direction.DESC, DATE_FIELD);
        return findPage(mongoTemplate, collectionName, pageable, criteria, sort, null,
                AtspmSpatSignalGroupAlignmentEvent.class);
    }

    @Override
    public List<IDCount> getAggregatedDailyAtspmSpatSignalGroupAlignmentEventCounts(int intersectionID, Long startTime,
            Long endTime) {
        Date startTimeDate = new Date(0);
        Date endTimeDate = new Date();

        if (startTime != null) {
            startTimeDate = new Date(startTime);
        }
        if (endTime != null) {
            endTimeDate = new Date(endTime);
        }

        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("intersectionID").is(intersectionID)),
                Aggregation.match(Criteria.where("eventGeneratedAt").gte(startTimeDate).lte(endTimeDate)),
                Aggregation.project()
                        .and(DateOperators.DateToString.dateOf("eventGeneratedAt").toString("%Y-%m-%d"))
                        .as("dateStr"),
                Aggregation.group("dateStr").count().as("count"));

        AggregationResults<IDCount> result = mongoTemplate.aggregate(aggregation, collectionName, IDCount.class);

        return result.getMappedResults();
    }
}
