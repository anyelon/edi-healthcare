package com.example.edi.common.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.util.List;

@Document(collection = "encounter_procedures")
public class EncounterProcedure {

    @Id
    private String id;
    private String encounterId;
    private int lineNumber;
    private String procedureCode;
    private List<String> modifiers;
    private BigDecimal chargeAmount;
    private int units;
    private String unitType;
    private List<Integer> diagnosisPointers;
    private boolean needsAuth;
    private String clinicalReason;

    public EncounterProcedure() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEncounterId() { return encounterId; }
    public void setEncounterId(String encounterId) { this.encounterId = encounterId; }

    public int getLineNumber() { return lineNumber; }
    public void setLineNumber(int lineNumber) { this.lineNumber = lineNumber; }

    public String getProcedureCode() { return procedureCode; }
    public void setProcedureCode(String procedureCode) { this.procedureCode = procedureCode; }

    public List<String> getModifiers() { return modifiers; }
    public void setModifiers(List<String> modifiers) { this.modifiers = modifiers; }

    public BigDecimal getChargeAmount() { return chargeAmount; }
    public void setChargeAmount(BigDecimal chargeAmount) { this.chargeAmount = chargeAmount; }

    public int getUnits() { return units; }
    public void setUnits(int units) { this.units = units; }

    public String getUnitType() { return unitType; }
    public void setUnitType(String unitType) { this.unitType = unitType; }

    public List<Integer> getDiagnosisPointers() { return diagnosisPointers; }
    public void setDiagnosisPointers(List<Integer> diagnosisPointers) { this.diagnosisPointers = diagnosisPointers; }

    public boolean isNeedsAuth() { return needsAuth; }
    public void setNeedsAuth(boolean needsAuth) { this.needsAuth = needsAuth; }

    public String getClinicalReason() { return clinicalReason; }
    public void setClinicalReason(String clinicalReason) { this.clinicalReason = clinicalReason; }
}
