package org.endeavourhealth.transform.emis.csv.transforms.admin;

import com.google.common.base.Strings;
import org.apache.commons.csv.CSVFormat;
import org.endeavourhealth.transform.common.CsvProcessor;
import org.endeavourhealth.transform.common.exceptions.TransformException;
import org.endeavourhealth.transform.emis.csv.EmisCsvHelper;
import org.endeavourhealth.transform.emis.csv.schema.Admin_Patient;
import org.endeavourhealth.transform.emis.openhr.schema.VocSex;
import org.endeavourhealth.transform.emis.openhr.transforms.common.SexConverter;
import org.endeavourhealth.transform.fhir.*;
import org.endeavourhealth.transform.fhir.schema.ContactRelationship;
import org.endeavourhealth.transform.fhir.schema.RegistrationType;
import org.hl7.fhir.instance.model.*;

import java.util.Date;
import java.util.List;

public class PatientTransformer {

    public static void transform(String folderPath,
                                 CSVFormat csvFormat,
                                 CsvProcessor csvProcessor,
                                 EmisCsvHelper csvHelper) throws Exception {

        Admin_Patient parser = new Admin_Patient(folderPath, csvFormat);
        try {
            while (parser.nextRecord()) {
                createPatient(parser, csvProcessor, csvHelper);
            }
        } catch (Exception ex) {
            throw new TransformException(parser.getErrorLine(), ex);
        } finally {
            parser.close();
        }
    }

    private static void createPatient(Admin_Patient patientParser,
                                      CsvProcessor csvProcessor,
                                      EmisCsvHelper csvHelper) throws Exception {

        //create Patient Resource
        Patient fhirPatient = new Patient();
        fhirPatient.setMeta(new Meta().addProfile(FhirUri.PROFILE_URI_PATIENT));

        String patientGuid = patientParser.getPatientGuid();
        String organisationGuid = patientParser.getOrganisationGuid();

        EmisCsvHelper.setUniqueId(fhirPatient, patientGuid, null);

        //create Episode of Care Resource
        EpisodeOfCare fhirEpisode = new EpisodeOfCare();
        fhirEpisode.setMeta(new Meta().addProfile(FhirUri.PROFILE_URI_EPISODE_OF_CARE));

        EmisCsvHelper.setUniqueId(fhirEpisode, patientGuid, null);

        fhirEpisode.setPatient(csvHelper.createPatientReference(patientGuid.toString()));

        //if the Resource is to be deleted from the data store, then stop processing the CSV row
        if (patientParser.getDeleted() || patientParser.getIsConfidential()) {
            csvProcessor.deletePatientResource(fhirPatient, patientGuid);
            csvProcessor.deletePatientResource(fhirEpisode, patientGuid);
            return;
        }

        String nhsNumber = patientParser.getNhsNumber();
        if (!Strings.isNullOrEmpty(nhsNumber)) {
            fhirPatient.addIdentifier(IdentifierHelper.createNhsNumberIdentifier(nhsNumber));
        }

        //store the patient GUID and patient number to the patient resource
        int patientNumber = patientParser.getPatientNumber();
        fhirPatient.addIdentifier(IdentifierHelper.createIdentifier(Identifier.IdentifierUse.SECONDARY, FhirUri.IDENTIFIER_SYSTEM_EMIS_PATIENT_GUID, patientGuid));
        fhirPatient.addIdentifier(IdentifierHelper.createIdentifier(Identifier.IdentifierUse.SECONDARY, FhirUri.IDENTIFIER_SYSTEM_EMIS_PATIENT_NUMBER, "" + patientNumber));

        Date dob = patientParser.getDateOfBirth();
        fhirPatient.setBirthDate(dob);

        Date dod = patientParser.getDateOfDeath();
        if (dod != null) {
            fhirPatient.setDeceased(new DateType(dod));
        }

        //EMIS only provides sex but FHIR requires gender, but will treat as the same concept
        VocSex vocSex = VocSex.fromValue(patientParser.getSex());
        Enumerations.AdministrativeGender gender = SexConverter.convertSex(vocSex);
        fhirPatient.setGender(gender);

        String title = patientParser.getTitle();
        String givenName = patientParser.getGivenName();
        String middleNames = patientParser.getMiddleNames();
        String surname = patientParser.getSurname();

        //the test CSV data has at least one patient with no surname, so treat the given name as surname
        if (Strings.isNullOrEmpty(surname)) {
            surname = givenName;
            givenName = "";
        }

        String forenames = (givenName + " " + middleNames).trim();

        List<HumanName> fhirNames = NameConverter.convert(title, forenames, surname, null, null, null);
        if (fhirNames != null) {
            fhirNames.forEach(fhirPatient::addName);
        }

        String houseNameFlat = patientParser.getHouseNameFlatNumber();
        String numberAndStreet = patientParser.getNumberAndStreet();
        String village = patientParser.getVillage();
        String town = patientParser.getTown();
        String county = patientParser.getCounty();
        String postcode = patientParser.getPostcode();

        Address fhirAddress = AddressConverter.createAddress(Address.AddressUse.HOME, houseNameFlat, numberAndStreet, village, town, county, postcode);
        fhirPatient.addAddress(fhirAddress);

        String homePhone = patientParser.getHomePhone();
        if (!Strings.isNullOrEmpty(homePhone)) {
            ContactPoint fhirContact = ContactPointHelper.createContactPoint(ContactPoint.ContactPointSystem.PHONE, ContactPoint.ContactPointUse.HOME, homePhone);
            fhirPatient.addTelecom(fhirContact);
        }

        String mobilePhone = patientParser.getMobilePhone();
        if (!Strings.isNullOrEmpty(mobilePhone)) {
            ContactPoint fhirContact = ContactPointHelper.createContactPoint(ContactPoint.ContactPointSystem.PHONE, ContactPoint.ContactPointUse.MOBILE, mobilePhone);
            fhirPatient.addTelecom(fhirContact);
        }

        String email = patientParser.getEmailAddress();
        if (!Strings.isNullOrEmpty(email)) {
            ContactPoint fhirContact = ContactPointHelper.createContactPoint(ContactPoint.ContactPointSystem.EMAIL, ContactPoint.ContactPointUse.HOME, email);
            fhirPatient.addTelecom(fhirContact);
        }

        fhirPatient.setManagingOrganization(csvHelper.createOrganisationReference(organisationGuid));

        String carerName = patientParser.getCarerName();
        String carerRelationship = patientParser.getCarerRelation();
        if (!Strings.isNullOrEmpty(carerName)) {

            Patient.ContactComponent fhirContact = new Patient.ContactComponent();
            fhirContact.setName(NameConverter.convert(carerName));

            if (!Strings.isNullOrEmpty(carerRelationship)) {
                //FHIR spec states that we should map to their relationship types if possible, but if
                //not possible, then send as a textual codeable concept
                try {
                    ContactRelationship fhirContactRelationship = ContactRelationship.fromCode(carerRelationship);
                    fhirContact.addRelationship(CodeableConceptHelper.createCodeableConcept(fhirContactRelationship));
                } catch (IllegalArgumentException ex) {
                    fhirContact.addRelationship(CodeableConceptHelper.createCodeableConcept(carerRelationship));
                }
            }

            fhirPatient.addContact(fhirContact);
        }

        RegistrationType registrationType = convertRegistrationType(patientParser.getPatientTypedescription(), patientParser.getDummyType());
        fhirPatient.addExtension(ExtensionConverter.createExtension(FhirExtensionUri.REGISTRATION_TYPE, CodingHelper.createCoding(registrationType)));

        String usualGpGuid = patientParser.getUsualGpUserInRoleGuid();
        if (!Strings.isNullOrEmpty(usualGpGuid)) {
            fhirPatient.addCareProvider(csvHelper.createPractitionerReference(usualGpGuid));

        } else {
            String externalGpGuid = patientParser.getExternalUsualGPGuid();
            if (!Strings.isNullOrEmpty(externalGpGuid)) {
                fhirPatient.addCareProvider(csvHelper.createPractitionerReference(externalGpGuid));

            } else {
                String externalOrgGuid = patientParser.getExternalUsualGPOrganisation();
                if (!Strings.isNullOrEmpty(externalOrgGuid)) {
                    fhirPatient.addCareProvider(csvHelper.createOrganisationReference(externalOrgGuid));
                }
            }
        }

        String orgUuid = patientParser.getOrganisationGuid();
        fhirEpisode.setManagingOrganization(csvHelper.createOrganisationReference(orgUuid));

        if (!Strings.isNullOrEmpty(usualGpGuid)) {
            fhirEpisode.setCareManager(csvHelper.createPractitionerReference(usualGpGuid));
        }

        Date regDate = patientParser.getDateOfRegistration();
        Date dedDate = patientParser.getDateOfDeactivation();
        Period fhirPeriod = PeriodHelper.createPeriod(regDate, dedDate);
        fhirEpisode.setPeriod(fhirPeriod);

        boolean active = PeriodHelper.isActive(fhirPeriod);
        fhirPatient.setActive(active);
        if (active) {
            fhirEpisode.setStatus(EpisodeOfCare.EpisodeOfCareStatus.ACTIVE);
        } else {
            fhirEpisode.setStatus(EpisodeOfCare.EpisodeOfCareStatus.FINISHED);
        }

        csvProcessor.savePatientResource(fhirPatient, patientGuid);
        csvProcessor.savePatientResource(fhirEpisode, patientGuid);
    }

    /**
     * converts the patientDescription String from the CSV to the FHIR registration type
     * possible registration types based on the VocPatientType enum from EMIS Open
     */
    private static RegistrationType convertRegistrationType(String csvRegType, boolean dummyRecord) {

        if (dummyRecord || csvRegType.equalsIgnoreCase("Dummy")) {
            return RegistrationType.DUMMY;
        } else if (csvRegType.equalsIgnoreCase("Emg")) {
            return RegistrationType.EMERGENCY;
        } else if (csvRegType.equalsIgnoreCase("Immediately necessary")) {
            return RegistrationType.IMMEDIATELY_NECESSARY;
        } else if (csvRegType.equalsIgnoreCase("Private")) {
            return RegistrationType.PRIVATE;
        } else if (csvRegType.equalsIgnoreCase("Regular")) {
            return RegistrationType.REGULAR_GMS;
        } else if (csvRegType.equalsIgnoreCase("Temporary")) {
            return RegistrationType.TEMPORARY;
        } else {
            return RegistrationType.OTHER;
        }
    }

}
