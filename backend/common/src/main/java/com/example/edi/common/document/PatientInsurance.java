package com.example.edi.common.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Document(collection = "patient_insurances")
public class PatientInsurance {

    @Id
    private String id;
    private String patientId;
    private String payerId;
    private String memberId;
    private String groupNumber;
    private String policyType;
    private String subscriberRelationship;
    private LocalDate effectiveDate;
    private LocalDate terminationDate;

    public PatientInsurance() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getPayerId() { return payerId; }
    public void setPayerId(String payerId) { this.payerId = payerId; }

    public String getMemberId() { return memberId; }
    public void setMemberId(String memberId) { this.memberId = memberId; }

    public String getGroupNumber() { return groupNumber; }
    public void setGroupNumber(String groupNumber) { this.groupNumber = groupNumber; }

    public String getPolicyType() { return policyType; }
    public void setPolicyType(String policyType) { this.policyType = policyType; }

    public String getSubscriberRelationship() { return subscriberRelationship; }
    public void setSubscriberRelationship(String subscriberRelationship) { this.subscriberRelationship = subscriberRelationship; }

    public LocalDate getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }

    public LocalDate getTerminationDate() { return terminationDate; }
    public void setTerminationDate(LocalDate terminationDate) { this.terminationDate = terminationDate; }
}
