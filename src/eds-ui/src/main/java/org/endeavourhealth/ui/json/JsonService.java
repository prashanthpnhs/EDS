package org.endeavourhealth.ui.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.endeavourhealth.core.database.dal.admin.models.Service;
import org.endeavourhealth.core.fhirStorage.ServiceInterfaceEndpoint;

import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public final class JsonService {
    private UUID uuid = null;
    private String localIdentifier = null;
    private String publisherConfigName = null;
    private String name = null;
    private List<ServiceInterfaceEndpoint> endpoints = null;
    private String notes = null;
    private String postcode;
    private String ccgCode;
    private String organisationTypeDesc;
    private String organisationTypeCode;
    private List<JsonServiceSystemStatus> systemStatuses;

    public JsonService() {
    }

    public JsonService(Service service) throws Exception {
        this.uuid = service.getId();
        this.localIdentifier = service.getLocalId();
        this.name = service.getName();
        this.publisherConfigName = service.getPublisherConfigName();
        //this.additionalInfo = additionalInfo;
        this.endpoints = service.getEndpointsList();

        this.notes = service.getNotes();
        this.postcode = service.getPostcode();
        this.ccgCode = service.getCcgCode();
        if (service.getOrganisationType() != null) {
            this.organisationTypeDesc = service.getOrganisationType().getDescription();
            this.organisationTypeCode = service.getOrganisationType().getCode();
        }
    }

    /**
     * gets/sets
     */
    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getLocalIdentifier() {
        return localIdentifier;
    }

    public void setLocalIdentifier(String localIdentifier) {
        this.localIdentifier = localIdentifier;
    }

    public String getPublisherConfigName() {
        return publisherConfigName;
    }

    public void setPublisherConfigName(String publisherConfigName) {
        this.publisherConfigName = publisherConfigName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<ServiceInterfaceEndpoint> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(List<ServiceInterfaceEndpoint> endpoints) {
        this.endpoints = endpoints;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getPostcode() {
        return postcode;
    }

    public void setPostcode(String postcode) {
        this.postcode = postcode;
    }

    public String getCcgCode() {
        return ccgCode;
    }

    public void setCcgCode(String ccgCode) {
        this.ccgCode = ccgCode;
    }

    public String getOrganisationTypeDesc() {
        return organisationTypeDesc;
    }

    public void setOrganisationTypeDesc(String organisationTypeDesc) {
        this.organisationTypeDesc = organisationTypeDesc;
    }

    public String getOrganisationTypeCode() {
        return organisationTypeCode;
    }

    public void setOrganisationTypeCode(String organisationTypeCode) {
        this.organisationTypeCode = organisationTypeCode;
    }

    public List<JsonServiceSystemStatus> getSystemStatuses() {
        return systemStatuses;
    }

    public void setSystemStatuses(List<JsonServiceSystemStatus> systemStatuses) {
        this.systemStatuses = systemStatuses;
    }
}

