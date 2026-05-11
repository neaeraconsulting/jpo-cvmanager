package us.dot.its.jpo.ode.api.accessors.events.atspm_spat_pair_event;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

import us.dot.its.jpo.ode.api.models.IDCount;
import us.dot.its.jpo.ode.api.models.atspm.AtspmSpatPairEvent;

public interface AtspmSpatPairEventRepository {
    long count(Integer intersectionID, Long startTime, Long endTime);

    Page<AtspmSpatPairEvent> findLatest(Integer intersectionID, Long startTime, Long endTime);

    Page<AtspmSpatPairEvent> find(Integer intersectionID, Long startTime, Long endTime, Pageable pageable);

    List<IDCount> getAggregatedDailyAtspmSpatPairEventCounts(int intersectionID, Long startTime, Long endTime);
}
