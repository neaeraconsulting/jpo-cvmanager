# RSU Status Monitor

## Overview

The RSU Status Monitor is a service designed to track and report the operational status of Roadside Units (RSUs) within a connected vehicle environment. It collects status data, performs health checks, and provides alerts for any detected issues.

## Features

### Core Monitoring Capabilities

- **Real-time RSU Status Monitoring**: Continuously monitors all accessible Roadside Units (RSUs) in the deployment detailed by the CV Manager PostgreSQL database
- **Scheduled Data Collection**: Configurable monitoring intervals with fixed rate execution
- **Parallel Processing**: Utilizes a fixed thread pool (10 threads) with CompletableFuture for concurrent RSU querying

### SNMP Integration

- **SNMP v2 & v3 Support**: Communicates with RSUs using both SNMP v2c (community-based) and SNMP v3 (username/password with authentication and encryption)
- **Security Protocols**: Implements SHA authentication and AES-128 encryption for secure SNMP v3 communications
- **Comprehensive OID Mapping**: Supports extensive NTCIP 1218 standard Object Identifiers (OIDs) including:
  - System status and operational modes
  - GPS/GNSS positioning data
  - Temperature and environmental sensors
  - Message forwarding configurations
  - Firmware and MIB version information
  - Clock source and synchronization status

### Statistical Data Collection

The application collects and reports the following RSU statistics:

- **Uptime**: Time in seconds since the RSU last rebooted (`rsuTimeSincePowerOn`)
- **Temperature**: Internal temperature readings of the RSU hardware (`rsuIntTemp`)
- **Mode Status**: Operational state including Operational (4), Standby (2), and Off (16) modes (`rsuModeStatus`)
- **Timestamp**: UTC millisecond-precision timestamps for all collected data

### Database and Event Streaming Integration

- **PostgreSQL Integration**: Retrieves RSU credentials and configuration from a PostgreSQL database using JPA/Hibernate
- **Multi-RSU Support**: Queries all RSUs with their associated SNMP credentials, intersection mappings, and protocol configurations
- **Intersection Correlation**: Links RSU data with intersection identifiers for geographic context
- **Kafka Producer**: Publishes RSU status updates to Kafka topics for downstream processing and analytics

## License

This project is licensed under the MIT License.
