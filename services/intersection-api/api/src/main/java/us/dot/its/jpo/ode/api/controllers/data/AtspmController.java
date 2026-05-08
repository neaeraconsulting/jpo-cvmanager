package us.dot.its.jpo.ode.api.controllers.data;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import us.dot.its.jpo.ode.api.accessors.atspm.AtspmSpatPairLogRepository;
import us.dot.its.jpo.ode.api.models.atspm.AtspmSpatPairLog;

@Slf4j
@RestController
@ConditionalOnProperty(name = "enable.api", havingValue = "true", matchIfMissing = false)
@ApiResponses(value = {
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "500", description = "Internal Server Error")
})
@RequestMapping("/data/atspm")
public class AtspmController {

    private final AtspmSpatPairLogRepository atspmSpatPairLogRepository;

    @Autowired
    public AtspmController(AtspmSpatPairLogRepository atspmSpatPairLogRepository) {
        this.atspmSpatPairLogRepository = atspmSpatPairLogRepository;
    }

    @Operation(summary = "Find ATSPM SPAT Pairs", description = "Returns ATSPM SPAT pair logs filtered by intersection ID and query time. If query_time_utc_millis is provided, only records where startTime <= query_time <= endTime are returned. The latest parameter returns only the most recent matching record.")
    @RequestMapping(value = "/spat-pair", method = RequestMethod.GET, produces = "application/json")
    @PreAuthorize("@PermissionService.isSuperUser() || (@PermissionService.hasIntersection(#intersectionID, 'USER') and @PermissionService.hasRole('USER'))")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires SUPER_USER or USER role with access to the intersection requested"),
    })
    public ResponseEntity<Page<AtspmSpatPairLog>> findAtspmSpatPairLogs(
            @RequestParam(name = "intersection_id") Integer intersectionID,
            @RequestParam(name = "query_time_utc_millis", required = false) Long queryTime,
            @RequestParam(name = "latest", required = false, defaultValue = "false") boolean latest,
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "size", required = false, defaultValue = "10000") int size) {

        if (latest) {
            System.out.println(atspmSpatPairLogRepository.findLatest(intersectionID, queryTime));
            return ResponseEntity.ok(atspmSpatPairLogRepository.findLatest(intersectionID, queryTime));
        }

        PageRequest pageable = PageRequest.of(page, size);
        Page<AtspmSpatPairLog> response = atspmSpatPairLogRepository.find(intersectionID, queryTime,
                pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Count ATSPM SPAT Pairs", description = "Returns the count of ATSPM SPAT pair logs filtered by intersection ID and query time. If query_time_utc_millis is provided, only records where startTime <= query_time <= endTime are counted.")
    @RequestMapping(value = "/spat-pair/count", method = RequestMethod.GET, produces = "application/json")
    @PreAuthorize("@PermissionService.isSuperUser() || (@PermissionService.hasIntersection(#intersectionID, 'USER') and @PermissionService.hasRole('USER'))")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires SUPER_USER or USER role with access to the intersection requested"),
    })
    public ResponseEntity<Long> countAtspmSpatPairLogs(
            @RequestParam(name = "intersection_id") Integer intersectionID,
            @RequestParam(name = "query_time_utc_millis", required = false) Long queryTime) {

        long count = atspmSpatPairLogRepository.count(intersectionID, queryTime);
        return ResponseEntity.ok(count);
    }
}
