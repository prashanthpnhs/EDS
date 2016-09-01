package org.endeavourhealth.transform.ceg.transforms;

import org.endeavourhealth.transform.ceg.models.AbstractModel;
import org.endeavourhealth.transform.ceg.models.Encounter;
import org.hl7.fhir.instance.model.Condition;

import java.util.List;

public class ConditionTransformer extends AbstractTransformer {

    public static void transform(Condition fhir, List<AbstractModel> models) throws Exception {

        Encounter model = new Encounter();

        model.setPatientId(transformPatientId(fhir.getPatient()));
        model.setEventDate(transformDate(fhir.getOnset()));

//TODO - finish
        /**

         private String nativeClinicalCode;
         private Long staffId;
         private String consultationType;
         private Integer consultationDuration;
         private Integer problemId;
         private Long snomedConceptCode;

         */

        models.add(model);
    }
}
