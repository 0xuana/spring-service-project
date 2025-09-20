# Spring Cloud Config Server Setup

This document provides instructions for using the centralized configuration system implemented in Part 2 of the project requirements.

## Overview

The Config Server provides centralized configuration management for all microservices. Configuration is externalized from service JARs and managed in a central repository.

## Architecture

```
config-repo/               # Configuration repository
├── application.yml        # Shared defaults for all services
├── employee-service.yml   # Employee service specific config
├── department-service.yml # Department service specific config
├── api-gateway.yml       # API Gateway specific config
└── discovery-service.yml # Eureka discovery specific config

config-server/            # Spring Cloud Config Server
└── Application runs on port 8888
```

## Starting Services (Correct Order)

**IMPORTANT**: Services must be started in this order for proper dependency resolution:

```bash
# 1. Start Config Server FIRST (all other services depend on it)
mvn -pl config-server spring-boot:run

# 2. Start Discovery Service
mvn -pl discovery-service spring-boot:run

# 3. Start other services (order doesn't matter after discovery is up)
mvn -pl api-gateway spring-boot:run
mvn -pl department-service spring-boot:run
mvn -pl employee-service spring-boot:run
```

## Configuration Management

### Shared Configuration (application.yml)
- Common Eureka settings
- Standard actuator endpoints
- Default logging levels
- Feature flags (enable-detailed-logging, enable-metrics, etc.)

### Service-Specific Configuration
Each service has its own configuration file:
- **employee-service.yml**: Database settings, service-specific logging, feature flags
- **department-service.yml**: Database settings, service-specific logging, feature flags
- **api-gateway.yml**: Gateway routes, filters, gateway-specific logging
- **discovery-service.yml**: Eureka server settings, discovery-specific configuration

### Externalized Settings
The following configurations have been moved from service JARs to Config Server:
- Database connections (JDBC URLs, credentials, JPA settings)
- Eureka URLs and service discovery settings
- Flyway migration settings
- Log levels and logging configuration
- Feature flags for enabling/disabling functionality
- Server ports and application names
- Actuator endpoint exposure

## Runtime Configuration Refresh Demo

### Step 1: Start the Config Server and a Service

```bash
# Terminal 1: Start config server
mvn -pl config-server spring-boot:run

# Terminal 2: Start employee service
mvn -pl employee-service spring-boot:run
```

### Step 2: Verify Current Configuration

Check the current log level for the employee service:
```bash
curl http://localhost:8081/actuator/env | jq '.propertySources[] | select(.name | contains("configserver")) | .properties."logging.level.com.example.employee"'
```

You should see the current value is `DEBUG`.

### Step 3: Change Configuration in Config Repository

Edit `config-repo/employee-service.yml` and change the log level:

```yaml
# Change this line:
logging:
  level:
    com.example.employee: DEBUG

# To this:
logging:
  level:
    com.example.employee: TRACE
```

### Step 4: Refresh Configuration at Runtime

Call the refresh endpoint to reload configuration without restarting:

```bash
curl -X POST http://localhost:8081/actuator/refresh
```

The response will show which properties were refreshed:
```json
["logging.level.com.example.employee"]
```

### Step 5: Verify the Change Applied

Check the log level again:
```bash
curl http://localhost:8081/actuator/env | jq '.propertySources[] | select(.name | contains("configserver")) | .properties."logging.level.com.example.employee"'
```

You should now see the value is `TRACE`, and the application logs will show more detailed trace-level logging.

## Testing Config Server Dependency

Run the automated test to verify services depend on Config Server:

```bash
./test-config-dependency.sh
```

This test:
1. ✅ Confirms employee-service fails to start without config-server
2. ✅ Confirms employee-service starts successfully when config-server is available

## Available Actuator Endpoints

All services expose these management endpoints:

| Endpoint | Purpose |
|----------|---------|
| `/actuator/health` | Service health check |
| `/actuator/info` | Service information |
| `/actuator/refresh` | Refresh configuration from Config Server |
| `/actuator/env` | View environment properties |
| `/actuator/configprops` | View configuration properties |
| `/actuator/metrics` | Application metrics |

## Feature Flags

The configuration includes feature flags that can be toggled at runtime:

### Global Feature Flags (application.yml)
- `features.enable-detailed-logging`: Enable/disable detailed logging
- `features.enable-metrics`: Enable/disable metrics collection
- `features.enable-tracing`: Enable/disable distributed tracing

### Service-Specific Feature Flags
- **Employee Service**: `enable-department-enrichment`, `enable-employee-validation`
- **Department Service**: `enable-department-validation`, `enable-audit-logging`
- **API Gateway**: `enable-request-logging`, `enable-rate-limiting`
- **Discovery Service**: `enable-self-preservation`, `enable-registry-metrics`

## Troubleshooting

### Service Won't Start
- Ensure Config Server is running on port 8888
- Check Config Server logs for connectivity issues
- Verify `spring.config.import=configserver:http://localhost:8888` in service application.yml

### Configuration Not Refreshing
- Ensure actuator refresh endpoint is enabled
- Check that the property is refreshable (marked with `@RefreshScope` if needed)
- Verify the property was actually changed in the config repository

### Config Server Connection Issues
- Confirm config-repo directory exists and contains the configuration files
- Check Config Server startup logs for Git repository initialization
- Verify file permissions on config-repo directory

## Best Practices

1. **Always start Config Server first** - Other services will fail without it
2. **Use feature flags** for toggling functionality without code deployment
3. **Leverage refresh endpoint** for runtime configuration changes
4. **Monitor actuator endpoints** for health and configuration status
5. **Keep sensitive data encrypted** in production environments (consider Spring Cloud Config encryption)

## Next Steps for Production

- Add Config Server clustering for high availability
- Implement configuration encryption for sensitive data
- Set up monitoring and alerting for Config Server health
- Consider using Git backend instead of local filesystem for config repository
- Implement configuration change approval workflows