package org.endeavourhealth.transform.emis.emisopen.transforms.clinical;

import org.endeavourhealth.transform.common.exceptions.TransformException;
import org.endeavourhealth.transform.emis.emisopen.schema.eommedicalrecord38.EventListType;
import org.endeavourhealth.transform.emis.emisopen.schema.eommedicalrecord38.EventType;
import org.endeavourhealth.transform.emis.emisopen.schema.eommedicalrecord38.MedicalRecordType;
import org.hl7.fhir.instance.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class EventTransformer {

    private static final Logger LOG = LoggerFactory.getLogger(EventTransformer.class);

    //Internal use 0 = text, 1= Observation; 2= Problems; 5=Values; 7=Attachments; 8=Referrals;
    //        10=Alerts; 11=Allergies; 12=Family History; 13=Immunisations; 14=Problem Ratings
    enum ObservationType
    {
        TEXT(0),
        OBSERVATION(1),
        PROBLEM(2),
        VALUE(5),
        ATTACHMENT(7),
        REFERRAL(8),
        ALERT(10),
        ALLERGY(11),
        FAMILYHISTORY(12),
        IMMUNISATION(13),
        PROBLEMRATING(14);

        private final int value;

        ObservationType(final int value)
        {
            this.value = value;
        }

        public int getValue()
        {
            return this.value;
        }

        public static ObservationType fromValue(int value)
        {
            for (ObservationType observationType : ObservationType.values())
                if (observationType.getValue() == value)
                    return observationType;

            throw new IllegalArgumentException(Integer.toString(value));
        }
    }

    public static void transform(MedicalRecordType medicalRecordType, List<Resource> results, String patientUuid) throws TransformException {

        //got records that have null event lists
        EventListType eventList = medicalRecordType.getEventList();
        if (eventList == null) {
            return;
        }

        for (EventType eventType : eventList.getEvent()) {
            Resource resource = transform(eventType, patientUuid);

            //need to handle null resources being returned
            if (resource != null) {
                results.add(resource);
            }
            //resource.add(transform(eventType, medicalRecordType.getRegistration().getGUID()));
        }
    }

    public static Resource transform(EventType eventType, String patientUuid) throws TransformException {

        switch (ObservationType.fromValue(eventType.getEventType().intValue())) {

            case TEXT:
            case OBSERVATION:
                return ObservationTransformer.transform(eventType, patientUuid);
            case PROBLEM:
            case VALUE:
                return ObservationTransformer.transform(eventType, patientUuid);
            case ATTACHMENT:
            case REFERRAL:
            case ALERT:
                return null;
            case ALLERGY:
                return AllergyTransformer.transform(eventType, patientUuid);
            case FAMILYHISTORY:
                return FamilyHistoryTransformer.transform(eventType, patientUuid);
            case IMMUNISATION:
                return ImmunisationTransformer.transform(eventType, patientUuid);
            case PROBLEMRATING:
                return null;
            default:
                throw new TransformException("Unhandled event type " + eventType);
        }
    }
}
