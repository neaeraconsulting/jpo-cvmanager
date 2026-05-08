package us.dot.its.jpo.ode.api.accessors.atspm;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import us.dot.its.jpo.ode.api.models.atspm.AtspmSpatPairLog;

public interface AtspmSpatPairLogRepository {
    long count(Integer intersectionId, Long queryTime);

    Page<AtspmSpatPairLog> findLatest(Integer intersectionId, Long queryTime);

    Page<AtspmSpatPairLog> find(Integer intersectionId, Long queryTime, Pageable pageable);
}