package org.endeavourhealth.transform.enterprise.transforms;

import org.endeavourhealth.common.fhir.CodeableConceptHelper;
import org.endeavourhealth.common.fhir.ExtensionConverter;
import org.endeavourhealth.common.fhir.FhirExtensionUri;
import org.endeavourhealth.transform.enterprise.EnterpriseTransformParams;
import org.endeavourhealth.transform.enterprise.outputModels.AbstractEnterpriseCsvWriter;
import org.hl7.fhir.instance.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Date;

public class DiagnosticReportTransformer extends AbstractTransformer {

    private static final Logger LOG = LoggerFactory.getLogger(DiagnosticReportTransformer.class);

    public boolean shouldAlwaysTransform() {
        return true;
    }

    public void transform(Long enterpriseId,
                          Resource resource,
                          AbstractEnterpriseCsvWriter csvWriter,
                          EnterpriseTransformParams params) throws Exception {

        DiagnosticReport fhir = (DiagnosticReport)resource;

        long id;
        long organisationId;
        long patientId;
        long personId;
        Long encounterId = null;
        Long practitionerId = null;
        Date clinicalEffectiveDate = null;
        Integer datePrecisionId = null;
        Long snomedConceptId = null;
        BigDecimal value = null;
        String units = null;
        String originalCode = null;
        boolean isProblem = false;
        String originalTerm = null;
        boolean isReview = false;

        id = enterpriseId.longValue();
        organisationId = params.getEnterpriseOrganisationId().longValue();
        patientId = params.getEnterprisePatientId().longValue();
        personId = params.getEnterprisePersonId().longValue();

        if (fhir.hasEncounter()) {
            Reference encounterReference = fhir.getEncounter();
            encounterId = findEnterpriseId(params, encounterReference);
        }

        if (fhir.hasExtension()) {
            for (Extension extension: fhir.getExtension()) {
                if (extension.getUrl().equals(FhirExtensionUri.DIAGNOSTIC_REPORT_FILED_BY)) {
                    Reference practitionerReference = (Reference)extension.getValue();
                    practitionerId = findEnterpriseId(params, practitionerReference);
                    if (practitionerId == null) {
                        practitionerId = transformOnDemand(practitionerReference, params);
                    }
                }
            }
        }

        if (fhir.hasEffectiveDateTimeType()) {
            DateTimeType dt = fhir.getEffectiveDateTimeType();
            clinicalEffectiveDate = dt.getValue();
            datePrecisionId = convertDatePrecision(dt.getPrecision());
        }

        snomedConceptId = CodeableConceptHelper.findSnomedConceptId(fhir.getCode());

        //add the raw original code, to assist in data checking
        originalCode = CodeableConceptHelper.findOriginalCode(fhir.getCode());

        //add original term too, for easy display of results
        originalTerm = fhir.getCode().getText();

        Extension reviewExtension = ExtensionConverter.findExtension(fhir, FhirExtensionUri.IS_REVIEW);
        if (reviewExtension != null) {
            BooleanType b = (BooleanType)reviewExtension.getValue();
            if (b.getValue() != null) {
                isReview = b.getValue();
            }
        }

        org.endeavourhealth.transform.enterprise.outputModels.Observation model = (org.endeavourhealth.transform.enterprise.outputModels.Observation)csvWriter;
        model.writeUpsert(id,
                organisationId,
                patientId,
                personId,
                encounterId,
                practitionerId,
                clinicalEffectiveDate,
                datePrecisionId,
                snomedConceptId,
                value,
                units,
                originalCode,
                isProblem,
                originalTerm,
                isReview);
    }
}
