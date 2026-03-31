package com.example.edi.common.document;

public class RequestedProcedure {

    private String procedureCode;
    private String clinicalReason;

    public RequestedProcedure() {}

    public RequestedProcedure(String procedureCode, String clinicalReason) {
        this.procedureCode = procedureCode;
        this.clinicalReason = clinicalReason;
    }

    public String getProcedureCode() { return procedureCode; }
    public void setProcedureCode(String procedureCode) { this.procedureCode = procedureCode; }

    public String getClinicalReason() { return clinicalReason; }
    public void setClinicalReason(String clinicalReason) { this.clinicalReason = clinicalReason; }
}
