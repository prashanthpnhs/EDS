package org.endeavourhealth.transform.enterprise.transforms;

import com.google.common.base.Strings;
import org.endeavourhealth.common.fhir.CodeableConceptHelper;
import org.endeavourhealth.common.fhir.FhirExtensionUri;
import org.endeavourhealth.common.fhir.ReferenceHelper;
import org.endeavourhealth.common.fhir.schema.ReferralPriority;
import org.endeavourhealth.common.fhir.schema.ReferralType;
import org.endeavourhealth.core.data.ehr.ResourceNotFoundException;
import org.endeavourhealth.core.data.ehr.models.ResourceByExchangeBatch;
import org.endeavourhealth.transform.common.exceptions.TransformException;
import org.endeavourhealth.transform.enterprise.outputModels.OutputContainer;
import org.hl7.fhir.instance.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Map;

public class ReferralRequestTransformer extends AbstractTransformer {

    private static final Logger LOG = LoggerFactory.getLogger(ReferralRequestTransformer.class);

    public void transform(ResourceByExchangeBatch resource,
                          OutputContainer data,
                          Map<String, ResourceByExchangeBatch> otherResources,
                          Long enterpriseOrganisationId,
                          Long enterprisePatientId,
                          Long enterprisePersonId,
                          String configName) throws Exception {

        org.endeavourhealth.transform.enterprise.outputModels.ReferralRequest model = data.getReferralRequests();

        Long enterpriseId = mapId(resource, model);
        if (enterpriseId == null) {
            return;
            
        } else if (resource.getIsDeleted()) {
            model.writeDelete(enterpriseId.longValue());
            
        } else {

            ReferralRequest fhir = (ReferralRequest)deserialiseResouce(resource);

            /*Reference patientReference = fhir.getPatient();
            Long enterprisePatientId = findEnterpriseId(data.getPatients(), patientReference);*/

            //the test pack has data that refers to deleted or missing patients, so if we get a null
            //patient ID here, then skip this resource
            if (enterprisePatientId == null) {
                LOG.warn("Skipping " + fhir.getResourceType() + " " + fhir.getId() + " as no Enterprise patient ID could be found for it");
                return;
            }

            long id;
            long organizationId;
            long patientId;
            long personId;
            Long encounterId = null;
            Long practitionerId = null;
            Date clinicalEffectiveDate = null;
            Integer datePrecisionId = null;
            Long snomedConceptId = null;
            Long requesterOrganizationId = null;
            Long recipientOrganizationId = null;
            Integer priorityId = null;
            Integer typeId = null;
            String mode = null;
            Boolean outgoing = null;
            String originalCode = null;
            String originalTerm = null;

            id = enterpriseId.longValue();
            organizationId = enterpriseOrganisationId.longValue();
            patientId = enterprisePatientId.longValue();
            personId = enterprisePersonId.longValue();

            if (fhir.hasEncounter()) {
                Reference encounterReference = (Reference)fhir.getEncounter();
                encounterId = findEnterpriseId(data.getEncounters(), encounterReference);
            }

            if (fhir.hasRequester()) {
                Reference practitionerReference = fhir.getRequester();
                practitionerId = findEnterpriseId(data.getPractitioners(), practitionerReference);
            }

            if (fhir.hasDateElement()) {
                DateTimeType dt = fhir.getDateElement();
                clinicalEffectiveDate = dt.getValue();
                datePrecisionId = convertDatePrecision(dt.getPrecision());
            }

            //changed where the observation code is stored
            if (fhir.hasServiceRequested()) {
                if (fhir.getServiceRequested().size() > 1) {
                    throw new TransformException("Transform doesn't support referrals with multiple service codes " + fhir.getId());
                }
                CodeableConcept fhirServiceRequested = fhir.getServiceRequested().get(0);
                snomedConceptId = CodeableConceptHelper.findSnomedConceptId(fhirServiceRequested);

                //add the raw original code, to assist in data checking
                originalCode = CodeableConceptHelper.findOriginalCode(fhirServiceRequested);

                //add original term too, for easy display of results
                originalTerm = fhirServiceRequested.getText();
            }
            /*Long snomedConceptId = findSnomedConceptId(fhir.getType());
            model.setSnomedConceptId(snomedConceptId);*/

            if (fhir.hasRequester()) {
                Reference requesterReference = fhir.getRequester();
                ResourceType resourceType = ReferenceHelper.getResourceType(requesterReference);

                //the requester can be an organisation or practitioner
                if (resourceType == ResourceType.Organization) {
                    requesterOrganizationId = findEnterpriseId(data.getOrganisations(), requesterReference);

                } else if (resourceType == ResourceType.Practitioner) {
                    requesterOrganizationId = findOrganisationEnterpriseIdFromPractictioner(requesterReference, data, otherResources, fhir);
                }
            }

            if (fhir.hasRecipient()) {

                //there may be two recipients, one for the organisation and one for the practitioner
                for (Reference recipientReference: fhir.getRecipient()) {
                    ResourceType resourceType = ReferenceHelper.getResourceType(recipientReference);
                    if (resourceType == ResourceType.Organization) {
                        //the EMIS test pack contains referrals that point to recipient organisations that don't exist,
                        //so we need to handle the failure to find the organisation
                        recipientOrganizationId = findEnterpriseId(data.getOrganisations(), recipientReference);
                    }
                }

                //if we didn't find an organisation reference, look for a practitioner one
                if (recipientOrganizationId == null) {
                    for (Reference recipientReference : fhir.getRecipient()) {
                        ResourceType resourceType = ReferenceHelper.getResourceType(recipientReference);

                        if (resourceType == ResourceType.Practitioner) {
                            recipientOrganizationId = findOrganisationEnterpriseIdFromPractictioner(recipientReference, data, otherResources, fhir);
                        }
                    }
                }
            }

            //base the outgoing flag simply on whether the recipient ID matches the owning ID
            if (requesterOrganizationId != null) {
                outgoing = requesterOrganizationId.longValue() == organizationId;
            }

            if (fhir.hasPriority()) {
                CodeableConcept codeableConcept = fhir.getPriority();
                if (codeableConcept.hasCoding()) {
                    Coding coding = codeableConcept.getCoding().get(0);
                    ReferralPriority fhirReferralPriority = ReferralPriority.fromCode(coding.getCode());
                    priorityId = fhirReferralPriority.ordinal();
                }
            }

            if (fhir.hasType()) {
                CodeableConcept codeableConcept = fhir.getType();
                if (codeableConcept.hasCoding()) {
                    Coding coding = codeableConcept.getCoding().get(0);
                    ReferralType fhirReferralType = ReferralType.fromCode(coding.getCode());
                    typeId = fhirReferralType.ordinal();
                }
            }

            if (fhir.hasExtension()) {
                for (Extension extension: fhir.getExtension()) {
                    if (extension.getUrl().equals(FhirExtensionUri.REFERRAL_REQUEST_SEND_MODE)) {
                        CodeableConcept cc = (CodeableConcept)extension.getValue();
                        if (!Strings.isNullOrEmpty(cc.getText())) {
                            mode = cc.getText();
                        } else {
                            Coding coding = cc.getCoding().get(0);
                            mode = coding.getDisplay();
                        }
                    }
                }
            }

            model.writeUpsert(id, 
                organizationId,
                patientId,
                personId,
                encounterId,
                practitionerId,
                clinicalEffectiveDate,
                datePrecisionId,
                snomedConceptId,
                requesterOrganizationId,
                recipientOrganizationId,
                priorityId,
                typeId,
                mode,
                outgoing,
                originalCode,
                originalTerm);
        }
    }

    private Long findOrganisationEnterpriseIdFromPractictioner(Reference practitionerReference,
                                                                  OutputContainer data,
                                                                  Map<String, ResourceByExchangeBatch> otherResources,
                                                                  ReferralRequest fhir) throws Exception {

        try {
            Practitioner fhirPractitioner = (Practitioner)findResource(practitionerReference, otherResources);
            Practitioner.PractitionerPractitionerRoleComponent role = fhirPractitioner.getPractitionerRole().get(0);
            Reference organisationReference = role.getManagingOrganization();
            return findEnterpriseId(data.getOrganisations(), organisationReference);

        } catch (ResourceNotFoundException ex) {
            //we have a number of examples of Emis data where the practitioner doesn't exist, so handle this not being found
            LOG.warn("" + fhir.getResourceType() + " " + fhir.getId() + " refers to a Practitioner that doesn't exist");
            return null;
        }
    }



}

