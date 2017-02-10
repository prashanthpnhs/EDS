package org.endeavourhealth.transform.enterprise.transforms;

import OpenPseudonymiser.Crypto;
import com.google.common.base.Strings;
import org.endeavourhealth.core.data.ehr.ResourceRepository;
import org.endeavourhealth.core.data.ehr.models.ResourceByExchangeBatch;
import org.endeavourhealth.core.data.ehr.models.ResourceByPatient;
import org.endeavourhealth.core.data.ehr.models.ResourceHistory;
import org.endeavourhealth.core.fhirStorage.FhirResourceHelper;
import org.endeavourhealth.core.utility.Resources;
import org.endeavourhealth.transform.common.IdHelper;
import org.endeavourhealth.transform.enterprise.outputModels.AbstractEnterpriseCsvWriter;
import org.endeavourhealth.transform.enterprise.outputModels.OutputContainer;
import org.endeavourhealth.transform.fhir.IdentifierHelper;
import org.hl7.fhir.instance.model.Address;
import org.hl7.fhir.instance.model.Patient;
import org.hl7.fhir.instance.model.Resource;
import org.hl7.fhir.instance.model.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.*;

public class PatientTransformer extends AbstractTransformer {
    private static final Logger LOG = LoggerFactory.getLogger(PatientTransformer.class);

    private static final String PSEUDO_KEY_NHS_NUMBER = "NHSNumber";
    private static final String PSEUDO_KEY_DATE_OF_BIRTH = "DOB";
    private static final String PSEUDO_SALT_RESOURCE = "Endeavour Enterprise - East London.EncryptedSalt";

    private static byte[] saltBytes = null;

    public void transform(ResourceByExchangeBatch resource,
                          OutputContainer data,
                          Map<String, ResourceByExchangeBatch> otherResources,
                          Integer enterpriseOrganisationUuid) throws Exception {

        org.endeavourhealth.transform.enterprise.outputModels.Patient model = data.getPatients();

        Integer enterpriseId = mapId(resource, model);
        if (enterpriseId == null) {
            return;

        } else if (resource.getIsDeleted()) {
            //we've got records with a deleted patient but the child resources aren't deleted, so we need to manually delete them
            //from Enterprise
            deleteAllDependentEntities(data, resource);
            //model.writeDelete(enterpriseId.intValue());

        } else {
            Patient fhirPatient = (Patient)deserialiseResouce(resource);

            int id;
            int organizationId;
            int patientGenderId;
            String pseudoId = null;
            String nhsNumber = null;
            Date dateOfBirth = null;
            Date dateOfDeath = null;
            String postcode = null;

            id = enterpriseId.intValue();
            organizationId = enterpriseOrganisationUuid.intValue();

            //Calendar cal = Calendar.getInstance();

            dateOfBirth = fhirPatient.getBirthDate();
            /*cal.setTime(dob);
            int yearOfBirth = cal.get(Calendar.YEAR);
            model.setYearOfBirth(yearOfBirth);*/

            if (fhirPatient.hasDeceasedDateTimeType()) {
                dateOfDeath = fhirPatient.getDeceasedDateTimeType().getValue();
                /*cal.setTime(dod);
                int yearOfDeath = cal.get(Calendar.YEAR);
                model.setYearOfDeath(new Integer(yearOfDeath));*/
            }

            patientGenderId = fhirPatient.getGender().ordinal();

            if (fhirPatient.hasAddress()) {
                for (Address address: fhirPatient.getAddress()) {
                    if (address.getUse().equals(Address.AddressUse.HOME)) {
                        postcode = address.getPostalCode();
                    }
                }
            }

            pseudoId = pseudonomise(fhirPatient);

            //adding NHS number to allow data checking
            nhsNumber = IdentifierHelper.findNhsNumber(fhirPatient);

            model.writeUpsert(id,
                organizationId,
                patientGenderId,
                pseudoId,
                nhsNumber,
                dateOfBirth,
                dateOfDeath,
                postcode);
        }
    }

    /**
     * We've had a bug that resulted in deleted patient resources where the dependend resources we're also deleted
     * This is now fixed in the inbound Emis transform, but the existing data is in a state that the need to handle below
     */
    private void deleteAllDependentEntities(OutputContainer data, ResourceByExchangeBatch resourceBatchEntry) throws Exception {

        //retrieve all past versions, to find the EDS patient ID
        ResourceRepository resourceRepository = new ResourceRepository();
        UUID patientResourceId = resourceBatchEntry.getResourceId();
        String patientResourceType = resourceBatchEntry.getResourceType();
        List<ResourceHistory> resourceWrappers = resourceRepository.getResourceHistory(patientResourceType, patientResourceId);

        UUID serviceId = null;
        UUID systemId = null;
        UUID edsPatientId = null;
        for (int i=resourceWrappers.size()-1; i>=0; i--) {
            ResourceHistory resourceWrapper = resourceWrappers.get(i);
            if (!resourceWrapper.getIsDeleted()) {
                Resource resource = FhirResourceHelper.deserialiseResouce(resourceWrapper);
                try {
                    String s = IdHelper.getPatientId(resource);
                    serviceId = resourceWrapper.getSystemId();
                    systemId = resourceWrapper.getServiceId();
                    edsPatientId = UUID.fromString(s);
                    break;
                } catch (Exception ex) {
                    //if we call this on a non-patient resource, we'll get this exception, but ignore it
                }
            }
        }

        //if the patient has never existed in a non-deleted state, just return out
        if (edsPatientId == null) {
            return;
        }

        //retrieve all non-deleted resources
        List<ResourceByPatient> resourceByPatients = resourceRepository.getResourcesByPatient(serviceId, systemId, edsPatientId);
        for (ResourceByPatient resourceByPatient: resourceByPatients) {

            String resourceTypeStr = resourceByPatient.getResourceType();
            UUID resourceId = resourceByPatient.getResourceId();
            ResourceType resourceType = ResourceType.valueOf(resourceTypeStr);

            AbstractEnterpriseCsvWriter csvWriter = null;
            if (resourceType == ResourceType.Organization) {
                csvWriter = data.getOrganisations();
            } else if (resourceType == ResourceType.Practitioner) {
                csvWriter = data.getPractitioners();
            } else if (resourceType == ResourceType.Schedule) {
                csvWriter = data.getSchedules();
            } else if (resourceType == ResourceType.Patient) {
                csvWriter = data.getPatients();
            } else if (resourceType == ResourceType.EpisodeOfCare) {
                csvWriter = data.getEpisodesOfCare();
            } else if (resourceType == ResourceType.Appointment) {
                csvWriter = data.getAppointments();
            } else if (resourceType == ResourceType.Encounter) {
                csvWriter = data.getEncounters();
            } else if (resourceType == ResourceType.ReferralRequest) {
                csvWriter = data.getReferralRequests();
            } else if (resourceType == ResourceType.ProcedureRequest) {
                csvWriter = data.getProcedureRequests();
            } else if (resourceType == ResourceType.Observation) {
                csvWriter = data.getObservations();
            } else if (resourceType == ResourceType.MedicationStatement) {
                csvWriter = data.getMedicationStatements();
            } else if (resourceType == ResourceType.MedicationOrder) {
                csvWriter = data.getMedicationOrders();
            } else if (resourceType == ResourceType.AllergyIntolerance) {
                csvWriter = data.getAllergyIntolerances();
            } else {
                throw new Exception("Unhandlded resource type " + resourceType);
            }

            Integer enterpriseId = findEnterpriseId(csvWriter, resourceTypeStr, resourceId);
            if (enterpriseId != null) {
                csvWriter.writeDelete(enterpriseId.intValue());
            }
        }
    }

    /*public void transform(ResourceByExchangeBatch resource,
                          EnterpriseData data,
                          Map<String, ResourceByExchangeBatch> otherResources,
                          Integer enterpriseOrganisationUuid) throws Exception {

        org.endeavourhealth.core.xml.enterprise.Patient model = new org.endeavourhealth.core.xml.enterprise.Patient();

        if (!mapIdAndMode(resource, model)) {
            return;
        }

        //if it will be passed to Enterprise as an Insert or Update, then transform the remaining fields
        if (model.getSaveMode() == SaveMode.UPSERT) {

            Patient fhirPatient = (Patient)deserialiseResouce(resource);

            model.setOrganizationId(enterpriseOrganisationUuid);

            //Calendar cal = Calendar.getInstance();

            Date dob = fhirPatient.getBirthDate();
            model.setDateOfBirth(convertDate(dob));
            *//*cal.setTime(dob);
            int yearOfBirth = cal.get(Calendar.YEAR);
            model.setYearOfBirth(yearOfBirth);*//*

            if (fhirPatient.hasDeceasedDateTimeType()) {
                Date dod = fhirPatient.getDeceasedDateTimeType().getValue();
                model.setDateOfDeath(convertDate(dod));
                *//*cal.setTime(dod);
                int yearOfDeath = cal.get(Calendar.YEAR);
                model.setYearOfDeath(new Integer(yearOfDeath));*//*
            }

            model.setPatientGenderId(fhirPatient.getGender().ordinal());

            if (fhirPatient.hasAddress()) {
                for (Address address: fhirPatient.getAddress()) {
                    if (address.getUse().equals(Address.AddressUse.HOME)) {
                        String postcode = address.getPostalCode();
                        model.setPostcode(postcode);
                    }
                }
            }

            model.setPseudoId(pseudonomise(fhirPatient));

            //adding NHS number to allow data checking
            String nhsNumber = findNhsNumber(fhirPatient);
            model.setNhsNumber(nhsNumber);
        }

        data.getPatient().add(model);
    }*/

    private static String pseudonomise(Patient fhirPatient) throws Exception {

        String nhsNumber = IdentifierHelper.findNhsNumber(fhirPatient);

        String dob = null;
        if (fhirPatient.hasBirthDate()) {
            Date d = fhirPatient.getBirthDate();
            dob = new SimpleDateFormat("dd-MM-yyyy").format(d);
        }

        //if we don't have either of these values, we can't generate a pseudo ID
        if (Strings.isNullOrEmpty(nhsNumber)
                || Strings.isNullOrEmpty(dob)) {
            return "";
        }

        TreeMap keys = new TreeMap();
        keys.put(PSEUDO_KEY_DATE_OF_BIRTH, dob);
        keys.put(PSEUDO_KEY_NHS_NUMBER, nhsNumber);

        Crypto crypto = new Crypto();
        crypto.SetEncryptedSalt(getEncryptedSalt());
        return crypto.GetDigest(keys);
    }

    private static byte[] getEncryptedSalt() throws Exception {
        if (saltBytes == null) {
            saltBytes = Resources.getResourceAsBytes(PSEUDO_SALT_RESOURCE);
        }
        return saltBytes;
    }

}
