package org.endeavourhealth.core.fhirStorage.metadata;

import org.hl7.fhir.instance.model.CodeableConcept;
import org.hl7.fhir.instance.model.Procedure;
import org.hl7.fhir.instance.model.ResourceType;

import java.util.UUID;

public class ProcedureMetadata extends AbstractResourceMetadata implements PatientCompartment {
    private UUID patientId;
    private CodeableConcept codeableConcept;

    @Override
    public UUID getPatientId() {
        return patientId;
    }

    public CodeableConcept getCodeableConcept() { return codeableConcept; }


    public ProcedureMetadata(Procedure resource) {
        super(resource);
        populateMetadataFromResource(resource);
    }

    private void populateMetadataFromResource(Procedure resource) {
        patientId = UUID.fromString(ReferenceHelper.getReferenceId(resource.getSubject(), ResourceType.Patient));
        codeableConcept = resource.getCode();
    }
}
