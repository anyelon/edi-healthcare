package com.example.edi.common.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Document(collection = "visits")
public class Visit {

    @Id
    private String id;
    private String patientId;
    private LocalDate dateOfService;
    private List<String> diagnosisCodes;
    private List<String> procedureCodes;
    private List<BigDecimal> chargeAmounts;
    private String placeOfServiceCode;
    private String renderingProviderNpi;

    public Visit() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public LocalDate getDateOfService() { return dateOfService; }
    public void setDateOfService(LocalDate dateOfService) { this.dateOfService = dateOfService; }

    public List<String> getDiagnosisCodes() { return diagnosisCodes; }
    public void setDiagnosisCodes(List<String> diagnosisCodes) { this.diagnosisCodes = diagnosisCodes; }

    public List<String> getProcedureCodes() { return procedureCodes; }
    public void setProcedureCodes(List<String> procedureCodes) { this.procedureCodes = procedureCodes; }

    public List<BigDecimal> getChargeAmounts() { return chargeAmounts; }
    public void setChargeAmounts(List<BigDecimal> chargeAmounts) { this.chargeAmounts = chargeAmounts; }

    public String getPlaceOfServiceCode() { return placeOfServiceCode; }
    public void setPlaceOfServiceCode(String placeOfServiceCode) { this.placeOfServiceCode = placeOfServiceCode; }

    public String getRenderingProviderNpi() { return renderingProviderNpi; }
    public void setRenderingProviderNpi(String renderingProviderNpi) { this.renderingProviderNpi = renderingProviderNpi; }
}
