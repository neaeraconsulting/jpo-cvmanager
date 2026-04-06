package us.dot.its.jpo.rsustatusmonitor.controllers;

import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import lombok.extern.slf4j.Slf4j;
import us.dot.its.jpo.rsustatusmonitor.models.api.RsuModeRequest;
import us.dot.its.jpo.rsustatusmonitor.models.api.RsuModeResponse;
import us.dot.its.jpo.rsustatusmonitor.services.RsuModeService;

@RestController
@RequestMapping("/api/rsus")
@Slf4j
public class RsuModeController {

    private final RsuModeService rsuModeService;

    @Autowired
    public RsuModeController(RsuModeService rsuModeService) {
        this.rsuModeService = rsuModeService;
    }

    @PostMapping("/mode")
    public ResponseEntity<RsuModeResponse> setRsuMode(@RequestBody(required = true) RsuModeRequest request) {
        if (!StringUtils.hasText(request.ipAddress())) {
            RsuModeResponse response = new RsuModeResponse("", -1, "failure",
                    "IP address is required.");
            return ResponseEntity.badRequest().body(response);
        }
        if (request.mode() == null) {
            RsuModeResponse response = new RsuModeResponse(request.ipAddress(), request.mode(), "failure",
                    "Mode is Required");
            return ResponseEntity.badRequest().body(response);
        } else {
            int mode = request.mode();
            if (mode != 2 && mode != 4 && mode != 16) {
                RsuModeResponse response = new RsuModeResponse(request.ipAddress(), request.mode(), "failure",
                        "Invalid mode value. Mode must be set to one of the following 2 (Standby) 4 (Operate) or 16 (Off).");
                return ResponseEntity.badRequest().body(response);
            }
        }

        try {
            return ResponseEntity.ok(rsuModeService.setRsuMode(request.ipAddress(), request.mode()));
        } catch (IllegalArgumentException e) {
            RsuModeResponse response = new RsuModeResponse(request.ipAddress(), request.mode(), "failure",
                    e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (UnsupportedOperationException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        } catch (Exception e) {
            log.warn("Unable to set RSU {} to mode {}", request.ipAddress(), request.mode(), e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to update RSU mode.", e);
        }
    }

    @GetMapping("/status")
    public ResponseEntity<RsuModeResponse> getRsuStatus(@RequestParam(required = true) String ipAddress) {
        try {
            return ResponseEntity.ok(rsuModeService.getCurrentRsuStatus(ipAddress));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (UnsupportedOperationException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        } catch (Exception e) {
            log.warn("Unable to get RSU {} status", ipAddress, e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to retrieve RSU status.", e);
        }
    }
}
