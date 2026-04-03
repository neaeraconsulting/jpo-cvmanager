package us.dot.its.jpo.rsustatusmonitor.services;

import java.util.NoSuchElementException;

import org.snmp4j.smi.Variable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import lombok.extern.slf4j.Slf4j;
import us.dot.its.jpo.rsustatusmonitor.models.api.RsuModeResponse;
import us.dot.its.jpo.rsustatusmonitor.models.postgres.derived.RsuSnmpCredentials;
import us.dot.its.jpo.rsustatusmonitor.models.snmp.OID;
import us.dot.its.jpo.rsustatusmonitor.models.snmp.OIDMap;

@Service
@Slf4j
public class RsuModeService {

    private final PostgresService postgresService;
    private final SNMPService snmpService;

    @Autowired
    public RsuModeService(PostgresService postgresService, SNMPService snmpService) {
        this.postgresService = postgresService;
        this.snmpService = snmpService;
    }

    public RsuModeResponse setRsuMode(String ipAddress, int mode) throws Exception {
        String normalizedIpAddress = normalizeIpAddress(ipAddress);
        validateMode(mode);

        RsuSnmpCredentials credentials = postgresService.getRsuCredentialsByIp(normalizedIpAddress)
                .orElseThrow(() -> new NoSuchElementException(
                        "No RSU credentials found for IP address " + normalizedIpAddress));

        validateCredentials(credentials);

        OID rsuModeOid = OIDMap.oids.get("rsuMode");
        if (rsuModeOid == null) {
            throw new IllegalStateException("RSU mode OID is not configured.");
        }

        try {
            snmpService.setSnmpV3Value(normalizedIpAddress,
                    credentials.getUsername(),
                    credentials.getPassword(),
                    rsuModeOid.getOid(),
                    mode);
        } catch (Exception e) {
            return new RsuModeResponse(normalizedIpAddress, mode, "failure",
                    "Failed to update RSU mode: " + e.getMessage());
        }

        log.info("Updated RSU {} to mode {}", normalizedIpAddress, mode);
        return new RsuModeResponse(normalizedIpAddress, mode, "success", "RSU mode updated successfully.");
    }

    public RsuModeResponse getCurrentRsuStatus(String ipAddress) throws Exception {
        String normalizedIpAddress = normalizeIpAddress(ipAddress);

        RsuSnmpCredentials credentials = postgresService.getRsuCredentialsByIp(normalizedIpAddress)
                .orElseThrow(() -> new NoSuchElementException(
                        "No RSU credentials found for IP address " + normalizedIpAddress));

        validateCredentials(credentials);

        OID rsuModeStatusOid = OIDMap.oids.get("rsuModeStatus");
        if (rsuModeStatusOid == null) {
            throw new IllegalStateException("RSU mode status OID is not configured.");
        }

        String privacyPassword = StringUtils.hasText(credentials.getEncrypt_password())
                ? credentials.getEncrypt_password()
                : credentials.getPassword();

        Variable modeStatus = snmpService.getSnmpV3Value(normalizedIpAddress,
                credentials.getUsername(),
                credentials.getPassword(),
                privacyPassword,
                rsuModeStatusOid.getOid());

        if (modeStatus == null) {
            throw new IllegalStateException("No mode status returned for RSU " + normalizedIpAddress);
        }

        int mode = modeStatus.toInt();
        log.info("Retrieved RSU {} current mode {}", normalizedIpAddress, mode);
        return new RsuModeResponse(normalizedIpAddress, mode, "success", "RSU status retrieved successfully.");
    }

    private String normalizeIpAddress(String ipAddress) {
        if (!StringUtils.hasText(ipAddress)) {
            throw new IllegalArgumentException("IP address is required.");
        }
        return ipAddress.trim();
    }

    private void validateMode(int mode) {
        if (mode < 0) {
            throw new IllegalArgumentException("Mode must be zero or greater.");
        }
    }

    private void validateCredentials(RsuSnmpCredentials credentials) {
        if (!StringUtils.hasText(credentials.getUsername()) || !StringUtils.hasText(credentials.getPassword())) {
            throw new IllegalStateException(
                    "Stored SNMP credentials are incomplete for RSU " + credentials.getIpv4_address());
        }
    }
}
