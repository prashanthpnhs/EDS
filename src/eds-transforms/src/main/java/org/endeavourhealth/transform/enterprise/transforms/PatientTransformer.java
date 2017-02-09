package org.endeavourhealth.transform.enterprise.transforms;

import OpenPseudonymiser.Crypto;
import com.google.common.base.Strings;
import org.endeavourhealth.core.data.ehr.models.ResourceByExchangeBatch;
import org.endeavourhealth.core.utility.Resources;
import org.endeavourhealth.transform.enterprise.outputModels.OutputContainer;
import org.endeavourhealth.transform.fhir.IdentifierHelper;
import org.hl7.fhir.instance.model.Address;
import org.hl7.fhir.instance.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

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
            model.writeDelete(enterpriseId.intValue());

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
