package us.dot.its.jpo.rsustatusmonitor.rest;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import us.dot.its.jpo.rsustatusmonitor.models.IntersectionStatusRecord;
import us.dot.its.jpo.rsustatusmonitor.services.IntersectionStatusQueryService;

@Slf4j
@RestController
@RequestMapping("/api/intersection-status")
public class IntersectionStatusController {

    private IntersectionStatusQueryService queryService;

    public IntersectionStatusController(IntersectionStatusQueryService queryService) {
        this.queryService = queryService;
    }

    /**
     * Get the latest status for a specific intersection
     */
    @GetMapping("/{intersectionId}")
    public ResponseEntity<IntersectionStatusRecord> getIntersectionStatus(@PathVariable String intersectionId) {
        log.debug("Querying status for intersection ID: {}", intersectionId);

        IntersectionStatusRecord status = queryService.getIntersectionStatus(intersectionId);
        if (status != null) {
            return ResponseEntity.ok(status);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get the latest status for all intersections
     */
    @GetMapping("/all")
    public ResponseEntity<Map<String, IntersectionStatusRecord>> getAllIntersectionStatuses() {
        log.debug("Querying status for all intersections");

        Map<String, IntersectionStatusRecord> statuses = queryService.getAllIntersectionStatuses();
        return ResponseEntity.ok(statuses);
    }
}
