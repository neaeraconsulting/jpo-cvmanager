package us.dot.its.jpo.rsustatusmonitor.services;

import java.util.HashMap;
import java.util.Map;

import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.OctetString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import us.dot.its.jpo.rsustatusmonitor.models.snmp.OIDMap;
import us.dot.its.jpo.rsustatusmonitor.models.snmp.RsuMsgfwdValues;
import us.dot.its.jpo.rsustatusmonitor.utils.SnmpHelperUtil;

@Service
@Slf4j
public class RsuNearestNeighborService {

	private SNMPService snmpService;

	@Autowired
	public RsuNearestNeighborService(SNMPService snmpService) {
		this.snmpService = snmpService;
	}

	@Async
	public void configureRsuForwarding(RsuMsgfwdValues values) {
		// TODO: Verify current SNMP configuration
		// Configure the rsuReceivedMsg to forward to the RSU Status Monitor
		try {
			// TODO: Determine index based on the current SNMP configurations
			int snmpIndex = 33;

			Map<String, org.snmp4j.smi.Variable> oidValues = new HashMap<>();

			oidValues.put(OIDMap.oids.get("rsuReceivedMsgPsid").getOid() + "." + snmpIndex,
					SnmpHelperUtil.hexStringToOctetString(values.getRsuReceivedMsgPsid()));
			oidValues.put(OIDMap.oids.get("rsuReceivedMsgDestIpAddr").getOid() + "." + snmpIndex,
					new OctetString(values.getRsuReceivedMsgDestIpAddr()));
			oidValues.put(OIDMap.oids.get("rsuReceivedMsgDestPort").getOid() + "." + snmpIndex,
					new Integer32(values.getRsuReceivedMsgDestPort()));
			oidValues.put(OIDMap.oids.get("rsuReceivedMsgProtocol").getOid() + "." + snmpIndex,
					new Integer32(values.getRsuReceivedMsgProtocol()));
			oidValues.put(OIDMap.oids.get("rsuReceivedMsgRssi").getOid() + "." + snmpIndex,
					new Integer32(values.getRsuReceivedMsgRssi()));
			oidValues.put(OIDMap.oids.get("rsuReceivedMsgInterval").getOid() + "." + snmpIndex,
					new Integer32(values.getRsuReceivedMsgInterval()));
			oidValues.put(OIDMap.oids.get("rsuReceivedMsgDeliveryStart").getOid() + "." + snmpIndex,
					SnmpHelperUtil.hexStringToOctetString(values.getRsuReceivedMsgDeliveryStart()));
			oidValues.put(OIDMap.oids.get("rsuReceivedMsgDeliveryStop").getOid() + "." + snmpIndex,
					SnmpHelperUtil.hexStringToOctetString(values.getRsuReceivedMsgDeliveryStop()));
			oidValues.put(OIDMap.oids.get("rsuReceivedMsgStatus").getOid() + "." + snmpIndex,
					new Integer32(values.getRsuReceivedMsgStatus()));
			oidValues.put(OIDMap.oids.get("rsuReceivedMsgSecure").getOid() + "." + snmpIndex,
					new Integer32(values.getRsuReceivedMsgSecure()));
			oidValues.put(OIDMap.oids.get("rsuReceivedMsgAuthMsgInterval").getOid() + "." + snmpIndex,
					new Integer32(values.getRsuReceivedMsgAuthMsgInterval()));

			snmpService.setSnmpV3Values(
					values.getRsuSnmpCredentials().getIpv4_address(),
					values.getRsuSnmpCredentials().getUsername(),
					values.getRsuSnmpCredentials().getPassword(),
					oidValues);
		} catch (Exception e) {
			log.error("Failed to configure RSU {}: {}", values.getRsuSnmpCredentials().getIntersection_id(),
					e.getMessage());
		}

		// TODO: Verify current SNMP configuration again and write results to
		// PostgreSQL's SnmpMsgfwdConfig table
	}
}