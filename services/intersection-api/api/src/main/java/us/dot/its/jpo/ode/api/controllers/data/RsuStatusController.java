package us.dot.its.jpo.ode.api.controllers.data;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import us.dot.its.jpo.ode.api.models.snmp.RsuState;
import us.dot.its.jpo.ode.api.accessors.rsuState.RsuStateRepository;

import java.util.List;

@RestController
@ConditionalOnProperty(name = "enable.api", havingValue = "true", matchIfMissing = false)
@ApiResponses(value = {
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "500", description = "Internal Server Error")
})
@RequestMapping("/data/rsu-status")
public class RsuStatusController {

    private final RsuStateRepository rsuStateRepository;
    private final String rsuStatusMonitorURL;
    private final RestTemplate restTemplate = new RestTemplate();
    private final String rsuModeSetTemplate = "%s/api/rsus/mode";
    private final String rsuModeStatusTemplate = "%s/api/rsus/status";

    @Autowired
    public RsuStatusController(RsuStateRepository rsuStateRepository,
            @Value("${rsuStatusMonitorURL}") String rsuStatusMonitorURL) {
        this.rsuStateRepository = rsuStateRepository;
        this.rsuStatusMonitorURL = rsuStatusMonitorURL;
    }

    @Operation(summary = "Get historical RSU Status", description = "Returns all RSU status records for the given RSU IP and time range (UTC ms)")
    @GetMapping(value = "/historical", produces = "application/json")
    @PreAuthorize("@PermissionService.isSuperUser() || @PermissionService.hasRole('USER')")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires SUPER_USER or USER role"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters")
    })
    public ResponseEntity<List<RsuState>> getHistoricalRsuStatus(
            @RequestParam String rsuIp,
            @RequestParam long startTime,
            @RequestParam long endTime) {
        if (startTime > endTime) {
            return ResponseEntity.badRequest().build();
        }
        List<RsuState> results = rsuStateRepository.retrieveRsuStateWithinTimeInterval(rsuIp, startTime, endTime);
        return ResponseEntity.ok(results);
    }

    @Operation(summary = "Get latest RSU Status", description = "Returns the most recent RSU status record for the given RSU IP")
    @GetMapping(value = "/latest", produces = "application/json")
    @PreAuthorize("@PermissionService.isSuperUser() || @PermissionService.hasRole('USER')")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires SUPER_USER or USER role"),
            @ApiResponse(responseCode = "404", description = "No RSU status found for this RSU IP")
    })
    public ResponseEntity<RsuState> getLatestRsuStatus(@RequestParam String rsuIp) {
        RsuState latest = rsuStateRepository.findLatestByRsuIP(rsuIp);
        if (latest != null) {
            return ResponseEntity.ok(latest);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Get aggregated RSU Status", description = "Returns aggregated RSU status records for the given RSU IP and time range (UTC ms)")
    @GetMapping(value = "/aggregated", produces = "application/json")
    @PreAuthorize("@PermissionService.isSuperUser() || @PermissionService.hasRole('USER')")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires SUPER_USER or USER role"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters")
    })
    public ResponseEntity<List<RsuState>> getAggregatedRsuStatus(
            @RequestParam String rsuIp,
            @RequestParam long startTime,
            @RequestParam long endTime,
            @RequestParam int intervalMinutes) {
        if (startTime > endTime || intervalMinutes <= 0) {
            return ResponseEntity.badRequest().build();
        }
        List<RsuState> results = rsuStateRepository.retrieveRsuStateWithinTimeInterval(rsuIp, startTime,
                endTime, intervalMinutes);
        return ResponseEntity.ok(results);
    }

    @Operation(summary = "Set RSU Mode", description = "Updates RSU mode by calling RSU-Status-Monitor service")
    @PostMapping(value = "/mode", produces = "application/json")
    @PreAuthorize("@PermissionService.isSuperUser() || @PermissionService.hasRole('OPERATOR') && @PermissionService.isRsuIpAuthorized(#request.ipAddress())")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires SUPER_USER or OPERATOR role"),
            @ApiResponse(responseCode = "404", description = "RSU not found"),
            @ApiResponse(responseCode = "502", description = "RSU-Status-Monitor unavailable")
    })
    public ResponseEntity<RsuModeResponse> setRsuMode(@RequestBody RsuModeRequest request) {
        String resourceURL = String.format(rsuModeSetTemplate, rsuStatusMonitorURL);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<RsuModeRequest> requestEntity = new HttpEntity<>(request, headers);

            ResponseEntity<RsuModeResponse> response = restTemplate.postForEntity(resourceURL, requestEntity,
                    RsuModeResponse.class);
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpStatusCodeException e) {
            throw new ResponseStatusException(e.getStatusCode(), e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Failed to call RSU-Status-Monitor mode endpoint.", e);
        }
    }

    @Operation(summary = "Get current RSU mode/status", description = "Retrieves RSU mode/status by calling RSU-Status-Monitor service")
    @GetMapping(value = "/mode/status", produces = "application/json")
    @PreAuthorize("@PermissionService.isSuperUser() || @PermissionService.hasRole('USER') && @PermissionService.isRsuIpAuthorized(#rsuIp)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires SUPER_USER or USER role"),
            @ApiResponse(responseCode = "404", description = "RSU not found"),
            @ApiResponse(responseCode = "502", description = "RSU-Status-Monitor unavailable")
    })
    public ResponseEntity<RsuModeResponse> getCurrentRsuModeStatus(@RequestParam(required = false) String rsuIp) {
        if (!StringUtils.hasText(rsuIp)) {
            // Adding explict check to make sure rsuIP is not null so as to provide a better
            // error message.
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "rsuIp parameter is required.");
        }

        String resourceURL = UriComponentsBuilder
                .fromUriString(String.format(rsuModeStatusTemplate, rsuStatusMonitorURL))
                .queryParam("ipAddress", rsuIp)
                .toUriString();

        try {
            ResponseEntity<RsuModeResponse> response = restTemplate.getForEntity(resourceURL, RsuModeResponse.class);
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpStatusCodeException e) {
            throw new ResponseStatusException(e.getStatusCode(), e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Failed to call RSU-Status-Monitor status endpoint.", e);
        }
    }

    public record RsuModeRequest(String ipAddress, Integer mode) {
    }

    public record RsuModeResponse(String ipAddress, int mode, String status, String message) {
    }
}