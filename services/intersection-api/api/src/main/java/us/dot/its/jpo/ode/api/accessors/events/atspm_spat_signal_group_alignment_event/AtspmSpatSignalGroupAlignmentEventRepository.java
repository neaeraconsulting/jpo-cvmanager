package us.dot.its.jpo.ode.api.accessors.events.atspm_spat_signal_group_alignment_event;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

import us.dot.its.jpo.ode.api.models.IDCount;
import us.dot.its.jpo.ode.api.models.atspm.AtspmSpatSignalGroupAlignmentEvent;

public interface AtspmSpatSignalGroupAlignmentEventRepository {
    long count(Integer intersectionID, Long startTime, Long endTime);

    Page<AtspmSpatSignalGroupAlignmentEvent> findLatest(Integer intersectionID, Long startTime, Long endTime);

    Page<AtspmSpatSignalGroupAlignmentEvent> find(Integer intersectionID, Long startTime, Long endTime,
            Pageable pageable);

    List<IDCount> getAggregatedDailyAtspmSpatSignalGroupAlignmentEventCounts(int intersectionID, Long startTime,
            Long endTime);
}
