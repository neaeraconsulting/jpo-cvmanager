package us.dot.its.jpo.ode.api.accessors.atspm;

import java.time.Instant;
import java.util.Date;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import us.dot.its.jpo.ode.api.accessors.IntersectionCriteria;
import us.dot.its.jpo.ode.api.accessors.PageableQuery;
import us.dot.its.jpo.ode.api.models.atspm.AtspmSpatPairLog;

@Component
public class AtspmSpatPairLogRepositoryImpl implements AtspmSpatPairLogRepository, PageableQuery {

    private final MongoTemplate mongoTemplate;

    private final String collectionName = "CmAtspmSpatPairLog";
    private final String INTERSECTION_ID_FIELD = "intersectionId";
    private final String START_TIME_FIELD = "startTime";
    private final String END_TIME_FIELD = "endTime";
    private final String SORT_FIELD = END_TIME_FIELD;

    public AtspmSpatPairLogRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * Get the count of records for a given intersectionId and queryTime.
     *
     * @param intersectionId the intersection ID to query by, if null will not be
     *                       applied
     * @param queryTime      if provided, only records where startTime <= queryTime
     *                       <= endTime are returned
     * @return the count of records that match the given criteria
     */
    public long count(
            Integer intersectionId,
            Long queryTime) {
        Query query = Query.query(buildCriteria(intersectionId, queryTime));
        return mongoTemplate.count(query, collectionName);
    }

    /**
     * Get the single most recent record for a given intersectionId and queryTime.
     *
     * @param intersectionId the intersection ID to query by, if null will not be
     *                       applied
     * @param queryTime      if provided, only records where startTime <= queryTime
     *                       <= endTime are returned
     * @return a single-page response containing the latest matching record
     */
    public Page<AtspmSpatPairLog> findLatest(
            Integer intersectionId,
            Long queryTime) {
        Query query = Query.query(buildCriteria(intersectionId, queryTime));
        Sort sort = Sort.by(Sort.Direction.DESC, SORT_FIELD);
        return wrapSingleResultWithPage(
                mongoTemplate.findOne(
                        query.with(sort),
                        AtspmSpatPairLog.class,
                        collectionName));
    }

    /**
     * Get paginated data for a given intersectionId and queryTime.
     *
     * @param intersectionId the intersection ID to query by, if null will not be
     *                       applied
     * @param queryTime      if provided, only records where startTime <= queryTime
     *                       <= endTime are returned
     * @param pageable       the pageable object to use for pagination
     * @return the paginated data that matches the given criteria
     */
    public Page<AtspmSpatPairLog> find(
            Integer intersectionId,
            Long queryTime,
            Pageable pageable) {
        Criteria criteria = buildCriteria(intersectionId, queryTime);
        Sort sort = Sort.by(Sort.Direction.DESC, SORT_FIELD);
        return findPage(mongoTemplate, collectionName, pageable, criteria, sort, null, AtspmSpatPairLog.class);
    }

    private Criteria buildCriteria(Integer intersectionId, Long queryTime) {
        Criteria criteria = new IntersectionCriteria()
                .whereOptional(INTERSECTION_ID_FIELD, intersectionId);

        if (queryTime != null) {
            Date queryDate = toDate(queryTime);
            criteria.and(START_TIME_FIELD).lte(queryDate);
            criteria.and(END_TIME_FIELD).gte(queryDate);
        }

        return criteria;
    }

    private Date toDate(Long epochMillis) {
        return Date.from(Instant.ofEpochMilli(epochMillis));
    }
}