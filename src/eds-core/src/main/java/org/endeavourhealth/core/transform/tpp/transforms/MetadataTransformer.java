package org.endeavourhealth.core.transform.tpp.transforms;

import org.endeavourhealth.core.transform.fhir.Fhir;
import org.endeavourhealth.core.transform.tpp.schema.Metadata;
import org.endeavourhealth.core.transform.tpp.schema.Site;
import org.endeavourhealth.core.transform.tpp.schema.User;
import org.hl7.fhir.instance.model.*;

import java.util.List;
import java.util.stream.Collectors;

public class MetadataTransformer {

    public static void transform(Metadata tppMetadata, List<Resource> fhirResources) {

        boolean first = true;
        for (Site site : tppMetadata.getSite()) {
            SiteTransformer.transform(site, first, fhirResources);
            first = false;
        }

        for (User user : tppMetadata.getUser()) {
            UserTransformer.transform(user, fhirResources);
        }

        linkOrganisations(fhirResources);
    }

    private static void linkOrganisations(List<Resource> fhirResources) {

        Organization fhirOrganisation = Fhir.findOrganisation(fhirResources);
        String orgId = fhirOrganisation.getId();

        //link all locations to the organisation
        List<Location> fhirLocations = fhirResources
                .stream()
                .filter(t -> t instanceof Location)
                .map(t -> (Location)t)
                .collect(Collectors.toList());

        for (Location fhirLocation: fhirLocations) {
            fhirLocation.setManagingOrganization(Fhir.createReference(ResourceType.Organization, orgId));
        }

        //link all users to the organisation
        List<Practitioner> fhirPractitioners = fhirResources
                .stream()
                .filter(t -> t instanceof Practitioner)
                .map(t -> (Practitioner)t)
                .collect(Collectors.toList());

        for (Practitioner fhirPractitioner: fhirPractitioners) {

            List<Practitioner.PractitionerPractitionerRoleComponent> fhirRoles = fhirPractitioner.getPractitionerRole();
            for (Practitioner.PractitionerPractitionerRoleComponent fhirRole: fhirRoles) {
                fhirRole.setManagingOrganization(Fhir.createReference(ResourceType.Organization, orgId));
            }

        }

    }
}
