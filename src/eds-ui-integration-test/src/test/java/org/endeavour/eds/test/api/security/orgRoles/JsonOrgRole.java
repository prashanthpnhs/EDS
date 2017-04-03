package org.endeavour.eds.test.api.security.orgRoles;

import java.util.List;
import java.util.UUID;

public class JsonOrgRole {

    private UUID orgRoleId;
    private String name;
    private String description;
    private UUID organisationId;

    private List<String> roles;

    public JsonOrgRole(UUID orgRoleId, String name, String description, UUID organisationId, List<String> roles) {
        this.orgRoleId = orgRoleId;
        this.name = name;
        this.description = description;
        this.organisationId = organisationId;
        this.roles = roles;
    }

    public JsonOrgRole() {

    }

    public UUID getOrgRoleId() {
        return orgRoleId;
    }

    public void setOrgRoleId(UUID orgRoleId) {
        this.orgRoleId = orgRoleId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public UUID getOrganisationId() {
        return organisationId;
    }

    public void setOrganisationId(UUID organisationId) {
        this.organisationId = organisationId;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }
}
