package org.endeavourhealth.core.data.ehr.models;

import com.datastax.driver.mapping.annotations.ClusteringColumn;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

import java.util.UUID;

@Table(keyspace = "ehr", name = "resource_by_patient")
public class ResourceByPatient {
    @PartitionKey(0)
    @Column(name = "service_id")
    private UUID serviceId;

    @PartitionKey(1)
    @Column(name = "system_id")
    private UUID systemId;

    @PartitionKey(2)
    @Column(name = "patient_id")
    private UUID patientId;

    @ClusteringColumn(0)
    @Column(name = "resource_type")
    private String resourceType;

    @ClusteringColumn(1)
    @Column(name = "resource_id")
    private UUID resourceId;

    @Column(name = "schema_version")
    private String schemaVersion;

    @Column(name = "resource_data")
    private String resourceData;

    public UUID getServiceId() {
        return serviceId;
    }

    public void setServiceId(UUID serviceId) {
        this.serviceId = serviceId;
    }

    public UUID getSystemId() {
        return systemId;
    }

    public void setSystemId(UUID systemId) {
        this.systemId = systemId;
    }

    public UUID getPatientId() {
        return patientId;
    }

    public void setPatientId(UUID patientId) {
        this.patientId = patientId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public UUID getResourceId() {
        return resourceId;
    }

    public void setResourceId(UUID resourceId) {
        this.resourceId = resourceId;
    }

    public String getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(String schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public String getResourceData() {
        return resourceData;
    }

    public void setResourceData(String resourceData) {
        this.resourceData = resourceData;
    }
}
