package org.endeavourhealth.transform.common.idmappers;

import org.endeavourhealth.transform.common.IdHelper;
import org.endeavourhealth.transform.common.exceptions.UnknownResourceException;
import org.endeavourhealth.transform.fhir.ReferenceComponents;
import org.endeavourhealth.transform.fhir.ReferenceHelper;
import org.hl7.fhir.instance.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

public abstract class BaseIdMapper {
    private static final Logger LOG = LoggerFactory.getLogger(BaseIdMapper.class);


    public abstract void mapIds(Resource resource, UUID serviceId, UUID systemId, boolean mapResourceId) throws Exception;

    protected void mapCommonResourceFields(DomainResource resource, UUID serviceId, UUID systemId, boolean mapResourceId) throws Exception {
        if (mapResourceId) {
            mapResourceId(resource, serviceId, systemId);
        }
        mapExtensions(resource, serviceId, systemId);
        mapContainedResources(resource, serviceId, systemId);
    }

    /**
     * maps the main ID of any resource
     */
    private void mapResourceId(Resource resource, UUID serviceId, UUID systemId) {

        if (!resource.hasId()) {
            return;
        }

        String newId = IdHelper.getOrCreateEdsResourceIdString(serviceId, systemId, resource.getResourceType(), resource.getId());
        resource.setId(newId);
    }

    /**
     * maps the IDs in any extensions of a resource
     */
    private void mapExtensions(DomainResource resource, UUID serviceId, UUID systemId) throws Exception {

        if (!resource.hasExtension()) {
            return;
        }

        for (Extension extension: resource.getExtension()) {
            if (extension.hasValue()
                && extension.getValue() instanceof Reference) {
                mapReference((Reference)extension.getValue(), resource, serviceId, systemId);
            }
        }
    }

    /**
     * maps the IDs in any extensions of a resource
     */
    private void mapContainedResources(DomainResource resource, UUID serviceId, UUID systemId) throws Exception {

        if (!resource.hasContained()) {
            return;
        }

        for (Resource contained: resource.getContained()) {
            //pass in false so we don't map the ID of the contained resource, since it's not supposed to be a global ID
            IdHelper.mapIds(serviceId, systemId, contained, false);
        }
    }


    /**
     * maps the IDs in any identifiers of a resource
     */
    protected void mapIdentifiers(List<Identifier> identifiers, Resource resource, UUID serviceId, UUID systemId) throws Exception {
        for (Identifier identifier: identifiers) {
            if (identifier.hasAssigner()) {
                mapReference(identifier.getAssigner(), resource, serviceId, systemId);
            }
        }
    }


    /**
     * maps the ID within any reference
     */
    protected void mapReference(Reference reference, Resource resource, UUID serviceId, UUID systemId) throws Exception {
        if (reference == null) {
            return;
        }

        if (reference.hasReference()) {

            ReferenceComponents comps = ReferenceHelper.getReferenceComponents(reference);

            //if the reference is to an internal contained resource, the above will return null
            if (comps != null) {

                //validate that the resource the reference points to is valid and something we've encountered before
                UUID existingEdsId = IdHelper.getEdsResourceId(serviceId, systemId, comps.getResourceType(), comps.getId());
                if (existingEdsId == null) {
                    throw new UnknownResourceException(comps.getResourceType(),
                            comps.getId(),
                            resource.getResourceType(),
                            resource.getId(),
                            serviceId,
                            systemId);
                }

                String newId = IdHelper.getOrCreateEdsResourceIdString(serviceId, systemId, comps.getResourceType(), comps.getId());
                reference.setReference(ReferenceHelper.createResourceReference(comps.getResourceType(), newId));
            }

        } else {

            //if the reference doesn't have an actual reference, it will have an inline resource
            Resource referredResource = (Resource)reference.getResource();
            IdHelper.mapIds(serviceId, systemId, referredResource);
        }
    }

    /**
     * maps the ID within any reference
     */
    protected void mapReferences(List<Reference> references, Resource resource, UUID serviceId, UUID systemId) throws Exception {
        if (references == null
                || references.isEmpty()) {
            return;
        }

        for (Reference reference: references) {
            mapReference(reference, resource, serviceId, systemId);
        }
    }




}
