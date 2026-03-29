package com.example.edi.common.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "encounter_diagnoses")
public class EncounterDiagnosis {

    @Id
    private String id;
    private String encounterId;
    private int rank;
    private String diagnosisCode;
    private String description;

    public EncounterDiagnosis() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEncounterId() { return encounterId; }
    public void setEncounterId(String encounterId) { this.encounterId = encounterId; }

    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }

    public String getDiagnosisCode() { return diagnosisCode; }
    public void setDiagnosisCode(String diagnosisCode) { this.diagnosisCode = diagnosisCode; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
